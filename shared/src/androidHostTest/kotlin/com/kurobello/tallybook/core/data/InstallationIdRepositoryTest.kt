package com.kurobello.tallybook.core.data

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.io.path.readBytes
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

class InstallationIdRepositoryTest {

  @Test
  fun `first read creates a node that later reads and a recreated repository return`() = runTest {
    withPreferencesDirectory { directory ->
      val firstFactory = factory(directory)
      val first =
          try {
            val repository =
                InstallationIdRepositoryImpl(firstFactory.device(), RecordingExclusion())
            val created = assertIs<DataOutcome.Success<String>>(repository.nodeId()).data
            assertEquals(DataOutcome.Success(created), repository.nodeId())
            created
          } finally {
            firstFactory.close()
          }

      val secondFactory = factory(directory)
      try {
        val recreated = InstallationIdRepositoryImpl(secondFactory.device(), RecordingExclusion())
        assertTrue(NODE_FORMAT.matches(first), first)
        assertEquals(DataOutcome.Success(first), recreated.nodeId())
      } finally {
        secondFactory.close()
      }
    }
  }

  @Test
  fun `concurrent first reads agree on a single node`() = runTest {
    withPreferencesDirectory { directory ->
      val factory = factory(directory)
      try {
        val repository = InstallationIdRepositoryImpl(factory.device(), RecordingExclusion())

        val reads = List(4) { async { repository.nodeId() } }.map { it.await() }

        assertEquals(1, reads.toSet().size, reads.toString())
        assertIs<DataOutcome.Success<String>>(reads.first())
      } finally {
        factory.close()
      }
    }
  }

  @Test
  fun `the node lives in the device file and leaves the financial preferences file untouched`() =
      runTest {
        withPreferencesDirectory { directory ->
          val factory = factory(directory)
          try {
            AppPreferencesRepositoryImpl(factory.create(PreferencesFile.APP)).initialize(Moneda.COP)
            val appFile = directory.resolve(PreferencesFile.APP.fileName)
            val appBytes = appFile.readBytes()

            val node =
                assertIs<DataOutcome.Success<String>>(
                        InstallationIdRepositoryImpl(factory.device(), RecordingExclusion())
                            .nodeId()
                    )
                    .data

            assertContentEquals(appBytes, appFile.readBytes())
            assertFalse(appFile.readBytes().decodeToString().contains(node))
            val deviceFile = directory.resolve(PreferencesFile.DEVICE.fileName)
            assertTrue(deviceFile.exists())
            assertTrue(deviceFile.readBytes().decodeToString().contains(node))
          } finally {
            factory.close()
          }
        }
      }

  @Test
  fun `every successful read excludes the device file from backup`() = runTest {
    withPreferencesDirectory { directory ->
      val factory = factory(directory)
      try {
        val exclusion = RecordingExclusion()
        val repository = InstallationIdRepositoryImpl(factory.device(), exclusion)

        repository.nodeId()
        repository.nodeId()

        assertEquals(listOf(PreferencesFile.DEVICE, PreferencesFile.DEVICE), exclusion.excluded)
      } finally {
        factory.close()
      }
    }
  }

  @Test
  fun `a malformed stored node is a local error`() = runTest {
    withPreferencesDirectory { directory ->
      val factory = factory(directory)
      try {
        factory.device().edit { it[stringPreferencesKey("node_id")] = "NOT-A-NODE" }
        val exclusion = RecordingExclusion()

        val failure =
            assertIs<DataOutcome.Failure>(
                InstallationIdRepositoryImpl(factory.device(), exclusion).nodeId()
            )

        assertIs<DataError.Local>(failure.error)
        assertEquals(emptyList(), exclusion.excluded)
      } finally {
        factory.close()
      }
    }
  }

  private fun TestScope.factory(directory: Path) =
      PreferencesDataStoreFactory(
          pathProvider = DirectoryPathProvider(directory),
          dispatcher = StandardTestDispatcher(testScheduler),
      )

  private fun PreferencesDataStoreFactory.device() = create(PreferencesFile.DEVICE)

  private suspend fun withPreferencesDirectory(block: suspend (Path) -> Unit) {
    val directory = createTempDirectory("tallybook-installation-")
    try {
      block(directory)
    } finally {
      directory.toFile().deleteRecursively()
    }
  }

  private class RecordingExclusion : BackupExclusion {
    val excluded = mutableListOf<PreferencesFile>()

    override fun exclude(file: PreferencesFile) {
      excluded += file
    }
  }

  private class DirectoryPathProvider(private val directory: Path) : PreferencesPathProvider {
    override fun path(file: PreferencesFile): String = directory.resolve(file.fileName).toString()
  }

  private companion object {
    val NODE_FORMAT = Regex("[0-9a-f]{16}")
  }
}
