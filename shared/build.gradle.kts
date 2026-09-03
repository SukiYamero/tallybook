import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.kotlinSerialization)
  alias(libs.plugins.androidMultiplatformLibrary)
  alias(libs.plugins.composeMultiplatform)
  alias(libs.plugins.composeCompiler)
  alias(libs.plugins.detekt)
  alias(libs.plugins.ktfmt)
  alias(libs.plugins.sqldelight)
}

detekt {
  buildUponDefaultConfig = true
  config.setFrom("$rootDir/config/detekt.yml")
  source.setFrom(
      "src/commonMain/kotlin",
      "src/androidMain/kotlin",
      "src/iosMain/kotlin",
  )
}

sqldelight {
  databases {
    create("TallybookDatabase") {
      packageName.set("com.kurobello.tallybook.core.database")
      schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
      verifyMigrations.set(true)
    }
  }
}

kotlin {
  listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
    iosTarget.binaries.framework {
      baseName = "Shared"
      isStatic = true
    }
  }

  android {
    namespace = "com.kurobello.tallybook.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    minSdk = libs.versions.android.minSdk.get().toInt()

    compilerOptions { jvmTarget = JvmTarget.JVM_11 }
    androidResources { enable = true }
    withHostTest { isIncludeAndroidResources = true }
    withDeviceTestBuilder { sourceSetTreeName = "test" }
        .configure { instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
  }

  sourceSets {
    androidMain.dependencies {
      implementation(libs.compose.uiToolingPreview)
      implementation(libs.compose.uiTooling)
      implementation(libs.sqldelight.androidDriver)
    }
    commonMain.dependencies {
      implementation(libs.compose.runtime)
      implementation(libs.compose.foundation)
      implementation(libs.compose.material3)
      implementation(libs.compose.ui)
      implementation(libs.compose.components.resources)
      implementation(libs.compose.uiToolingPreview)
      implementation(libs.androidx.lifecycle.viewmodelCompose)
      implementation(libs.androidx.lifecycle.runtimeCompose)
      implementation(libs.kotlinx.coroutines.core)
      implementation(libs.kotlinx.datetime)
      implementation(libs.kotlinx.serialization.core)
      implementation(libs.koin.core)
      implementation(libs.koin.compose)
      implementation(libs.koin.composeViewmodel)
      implementation(libs.navigation3.runtime)
      implementation(libs.navigation3.ui)
      implementation(libs.sqldelight.runtime)
    }
    iosMain.dependencies { implementation(libs.sqldelight.nativeDriver) }
    commonTest.dependencies { implementation(libs.kotlin.test) }
    getByName("androidHostTest").dependencies { implementation(libs.sqldelight.sqliteDriver) }
  }
}

dependencies { androidRuntimeClasspath(libs.compose.uiTooling) }
