package com.kurobello.tallybook.di

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kurobello.tallybook.core.data.CategoriaRepository
import com.kurobello.tallybook.core.data.MovimientoRepository
import com.kurobello.tallybook.core.database.DatabaseDriverFactory
import com.kurobello.tallybook.core.database.TallybookDatabase
import kotlin.test.Test
import kotlin.test.assertNotNull
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class RepositoryModulesTest {

  @Test
  fun `app modules resolve both repositories with a platform driver`() {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    TallybookDatabase.Schema.create(driver)
    val testDriverModule = module {
      single<DatabaseDriverFactory> { DatabaseDriverFactory { driver } }
    }
    val application = koinApplication {
      allowOverride(true)
      modules(appModules + testDriverModule)
    }

    assertNotNull(application.koin.get<MovimientoRepository>())
    assertNotNull(application.koin.get<CategoriaRepository>())

    application.close()
    driver.close()
  }
}
