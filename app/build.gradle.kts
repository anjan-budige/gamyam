plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

val releaseStoreFile = localProperties.getProperty("RELEASE_STORE_FILE", "")
val releaseStorePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD", "")
val releaseKeyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS", "")
val releaseKeyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD", "")
val hasReleaseSigning = releaseStoreFile.isNotBlank() &&
    releaseStorePassword.isNotBlank() &&
    releaseKeyAlias.isNotBlank() &&
    releaseKeyPassword.isNotBlank()

android {
    namespace = "com.gamyam.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.gamyam.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        
        buildConfigField("String", "CLIENT_ID", "\"${localProperties.getProperty("CLIENT_ID", "")}\"")
        buildConfigField("String", "CLIENT_SECRET", "\"${localProperties.getProperty("CLIENT_SECRET", "")}\"")
        buildConfigField("String", "MAP_SDK_KEY", "\"${localProperties.getProperty("MAP_SDK_KEY", "")}\"")
        buildConfigField("String", "ATLAS_CLIENT_ID", "\"${localProperties.getProperty("ATLAS_CLIENT_ID", "")}\"")
        buildConfigField("String", "ATLAS_CLIENT_SECRET", "\"${localProperties.getProperty("ATLAS_CLIENT_SECRET", "")}\"")
        buildConfigField("String", "REST_API_KEY", "\"${localProperties.getProperty("REST_API_KEY", "")}\"")
        buildConfigField("String", "DESEDE_KEY", "\"${localProperties.getProperty("DESEDE_KEY", "")}\"")
        buildConfigField("String", "TELEGRAM_BOT_TOKEN", "\"${localProperties.getProperty("TELEGRAM_BOT_TOKEN", "")}\"")
        buildConfigField("String", "TELEGRAM_CHAT_ID", "\"${localProperties.getProperty("TELEGRAM_CHAT_ID", "")}\"")
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = true
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.ui.text.google.fonts)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.kotlinx.serialization.json)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Retrofit
  implementation(libs.retrofit)
  implementation(libs.retrofit.converter.gson)

  // DataStore
  implementation(libs.androidx.datastore.preferences)
}
