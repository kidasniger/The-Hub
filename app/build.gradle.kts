import java.util.Properties
import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}


val versionFile = rootProject.file("gradle/version.properties")
require(versionFile.isFile) {
  "VERSIONING ERROR: " + versionFile.path + " is missing."
}

val versionProperties = Properties().apply {
  versionFile.inputStream().use { load(it) }
}

val appVersionName = versionProperties.getProperty("versionName")?.trim()
  ?: error("VERSIONING ERROR: versionName is missing from " + versionFile.path)

val appVersionCode = versionProperties.getProperty("versionCode")?.trim()?.toIntOrNull()
  ?: error("VERSIONING ERROR: versionCode must be an integer in " + versionFile.path)

val versionMatch = Regex("""^(\d+)\.(\d+)\.(\d+)$""").matchEntire(appVersionName)
  ?: error("VERSIONING ERROR: versionName must use MAJOR.MINOR.PATCH format: " + appVersionName)

val major = versionMatch.groupValues[1].toInt()
val minor = versionMatch.groupValues[2].toInt()
val patch = versionMatch.groupValues[3].toInt()
val expectedVersionCode = if (major == 1 && minor == 0) patch else major * 1_000_000 + minor * 1_000 + patch

require(appVersionCode == expectedVersionCode) {
  "VERSIONING ERROR: versionCode=" + appVersionCode + " does not match versionName=" + appVersionName + " (expected " + expectedVersionCode + ")."
}


android {
  namespace = "com.thehub.hb"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.thehub.hb"
    minSdk = 24
    targetSdk = 36
    versionCode = appVersionCode
    versionName = appVersionName

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
      storeFile = rootProject.file("debug.keystore")
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
    isCoreLibraryDesugaringEnabled = true
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  testOptions { unitTests { isIncludeAndroidResources = true } }

  // Keep the v1.0.150 updater registration behavior unchanged; current Lint flags
  // the legacy pre-API-33 branch even though it remains the v1.0.150 runtime flow.
  lint {
    disable += "UnspecifiedRegisterReceiverFlag"
  }

  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

val keystorePathProvider = providers.environmentVariable("KEYSTORE_PATH").map { it.trim() }
val storePasswordProvider = providers.environmentVariable("STORE_PASSWORD").map { it.trim() }
val keyAliasProvider = providers.environmentVariable("KEY_ALIAS").map { it.trim() }
val keyPasswordProvider = providers.environmentVariable("KEY_PASSWORD").map { it.trim() }
val keystoreFileProvider = keystorePathProvider.map { layout.projectDirectory.file(it).asFile }

val validateReleaseSigning = tasks.register("validateReleaseSigning") {
  group = "verification"
  description = "Fails when the production release keystore or credentials are missing/invalid."

  doLast {
    val keystorePath = keystorePathProvider.orNull.orEmpty()
    val storePassword = storePasswordProvider.orNull.orEmpty()
    val keyAlias = keyAliasProvider.orNull.orEmpty()
    val keyPassword = keyPasswordProvider.orNull.orEmpty()

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

    val keystoreFile = keystoreFileProvider.orNull
    require(keystoreFile?.isFile == true) {
      "RELEASE SIGNING ERROR: Production keystore not found: " + (keystoreFile?.absolutePath ?: keystorePath)
    }

    logger.lifecycle("Production release signing configuration detected: " + keystoreFile.absolutePath)
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
  coreLibraryDesugaring(libs.desugar.jdk.libs)
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.fragment.ktx)
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
  implementation(libs.firebase.messaging)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services)
  implementation(libs.googleid)
  implementation(libs.android.youtube.player)
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
