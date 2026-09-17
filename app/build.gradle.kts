import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.thehub.hb"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.thehub.hb"
    minSdk = 24
    targetSdk = 36
    versionCode = 21
    versionName = "1.0.21"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH")?.trim()
      val storePassword = System.getenv("STORE_PASSWORD")
      val keyAlias = System.getenv("KEY_ALIAS")
      val keyPassword = System.getenv("KEY_PASSWORD")

      if (!keystorePath.isNullOrEmpty()) {
        storeFile = file(keystorePath)
      }
      this.storePassword = storePassword
      this.keyAlias = keyAlias
      this.keyPassword = keyPassword
    }

    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    getByName("release") {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
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

val validateReleaseSigning = tasks.register("validateReleaseSigning") {
  group = "verification"
  description = "Fails when the production release keystore or credentials are missing/invalid."

  doLast {
    val keystorePath = System.getenv("KEYSTORE_PATH")?.trim().orEmpty()
    val storePassword = System.getenv("STORE_PASSWORD")?.trim().orEmpty()
    val keyAlias = System.getenv("KEY_ALIAS")?.trim().orEmpty()
    val keyPassword = System.getenv("KEY_PASSWORD")?.trim().orEmpty()

    require(keystorePath.isNotEmpty()) {
      "RELEASE SIGNING ERROR: KEYSTORE_PATH is required for release builds."
    }
    require(storePassword.isNotEmpty()) {
      "RELEASE SIGNING ERROR: STORE_PASSWORD is required for release builds."
    }
    require(keyAlias.isNotEmpty()) {
      "RELEASE SIGNING ERROR: KEY_ALIAS is required for release builds."
    }
    require(keyPassword.isNotEmpty()) {
      "RELEASE SIGNING ERROR: KEY_PASSWORD is required for release builds."
    }

    val keystoreFile = project.file(keystorePath)
    require(keystoreFile.isFile) {
      "RELEASE SIGNING ERROR: Production keystore not found: ${keystoreFile.absolutePath}"
    }

    logger.lifecycle("Production release signing configuration detected: ${keystoreFile.absolutePath}")
  }
}

tasks.configureEach {
  when (name) {
    "assembleRelease", "bundleRelease" -> dependsOn(validateReleaseSigning)
  }
}

secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  implementation(libs.firebase.firestore)
  implementation(libs.firebase.auth)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services)
  implementation(libs.googleid)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.retrofit)
  ksp(libs.androidx.room.compiler)
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
}
