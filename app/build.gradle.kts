plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.ahlalquran.kpmzqa"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  val envKeystoreFile = System.getenv("KEYSTORE_FILE")
  val envKeystorePassword = System.getenv("KEYSTORE_PASSWORD")
  val envKeyAlias = System.getenv("KEY_ALIAS")
  val envKeyPassword = System.getenv("KEY_PASSWORD")

  val hasReleaseSigning = !envKeystoreFile.isNullOrBlank() &&
      file(envKeystoreFile).exists() &&
      !envKeystorePassword.isNullOrBlank() &&
      !envKeyAlias.isNullOrBlank() &&
      !envKeyPassword.isNullOrBlank()

  signingConfigs {
    create("release") {
      enableV1Signing = true
      enableV2Signing = true
      enableV3Signing = true
      enableV4Signing = false
      if (hasReleaseSigning) {
        storeFile = file(envKeystoreFile!!)
        storePassword = envKeystorePassword
        keyAlias = envKeyAlias
        keyPassword = envKeyPassword
      } else {
        val runningReleaseTask = gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }
        if (runningReleaseTask) {
          throw org.gradle.api.GradleException(
            "❌ [Release Signing Error]: Missing Release signing secrets or keystore file!\n" +
            "Please ensure KEYSTORE_FILE, KEYSTORE_PASSWORD, KEY_ALIAS, and KEY_PASSWORD environment variables are set and KEYSTORE_FILE exists."
          )
        }
      }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      isShrinkResources = false
      isDebuggable = false
      isCrunchPngs = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debug")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Dependencies cleaned for minimal footprint and offline-first performance
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)

  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
}
