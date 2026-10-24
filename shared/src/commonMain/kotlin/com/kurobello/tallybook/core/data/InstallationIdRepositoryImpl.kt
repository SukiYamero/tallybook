package com.kurobello.tallybook.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.first
import okio.IOException

class InstallationIdRepositoryImpl(
    private val deviceDataStore: DataStore<Preferences>,
    private val backupExclusion: BackupExclusion,
) : InstallationIdRepository {

  override suspend fun nodeId(): DataOutcome<String> =
      try {
        val stored = deviceDataStore.data.first()[NODE_ID_KEY]
        // A malformed node cannot have issued any stamp, so replacing it loses no history and keeps
        // startup from failing on every launch.
        val node = stored?.takeIf(NODE_FORMAT::matches) ?: createNode(replacing = stored)
        if (NODE_FORMAT.matches(node)) {
          // Re-applied on every read: DataStore replaces the file on write, which clears the iOS
          // flag, and a crash between creation and exclusion must not leave it unmarked.
          backupExclusion.exclude(PreferencesFile.DEVICE)
          DataOutcome.Success(node)
        } else {
          DataOutcome.Failure(DataError.Local(IOException("Malformed installation node")))
        }
      } catch (error: IOException) {
        DataOutcome.Failure(DataError.Local(error))
      }

  private suspend fun createNode(replacing: String?): String {
    val candidate = randomNode()
    val stored = deviceDataStore.edit { preferences ->
      if (preferences[NODE_ID_KEY] == replacing) preferences[NODE_ID_KEY] = candidate
    }
    return checkNotNull(stored[NODE_ID_KEY])
  }

  private companion object {
    val NODE_ID_KEY = stringPreferencesKey("node_id")
    val NODE_FORMAT = Regex("[0-9a-f]{16}")
    const val NODE_HEX_DIGITS = 16
    const val HEX_RADIX = 16

    // A v4 UUID fixes its version bits in the high half and its variant bits in the low half;
    // they never overlap, so XOR-ing the halves leaves all 64 bits random.
    fun randomNode(): String =
        Uuid.random()
            .toLongs { mostSignificant, leastSignificant -> mostSignificant xor leastSignificant }
            .toULong()
            .toString(HEX_RADIX)
            .padStart(NODE_HEX_DIGITS, '0')
  }
}
