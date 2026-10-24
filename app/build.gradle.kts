import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import groovy.json.JsonSlurper
import java.time.Duration
import java.util.Base64

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

// Ensure debug.keystore always exists with the Firebase-registered SHA-1
// (43:D4:D7:F9:4E:D6:FC:D5:D1:53:B4:80:F5:4A:62:30:64:AE:46:EB) even when building
// from a partial copy or GitHub clone where debug.keystore was gitignored.
val rootDebugKeystore = file("${rootDir}/debug.keystore")
val rootDebugKeystoreBase64 = file("${rootDir}/debug.keystore.base64")
if (!rootDebugKeystore.exists()) {
  runCatching {
    val embeddedKeystoreBase64 =
      "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"
    val rawBase64 = if (rootDebugKeystoreBase64.exists()) {
      rootDebugKeystoreBase64.readText()
    } else {
      embeddedKeystoreBase64
    }
    val cleanBase64 = rawBase64.replace("\\s+".toRegex(), "")
    rootDebugKeystore.writeBytes(Base64.getDecoder().decode(cleanBase64))
  }
}

android {
  namespace = "com.example"
  compileSdk = 35

  defaultConfig {
    val appId = "com.aistudio.notasjs.vqrmpx"
    applicationId = appId
    minSdk = 24
    targetSdk = 35
    versionCode = 2
    versionName = "1.1"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    // Ensure R.string.default_web_client_id is always defined at compile time even if
    // google-services.json is missing or does not contain a type-3 OAuth client for this package.
    val googleServicesFile = file("google-services.json")
    var pluginGeneratesWebClientId = false
    var fallbackWebClientId = "318447528237-ktfp85ramrfbq9fldrk5hbmhca2e9j4b.apps.googleusercontent.com"
    if (googleServicesFile.exists()) {
      runCatching {
        @Suppress("UNCHECKED_CAST")
        val root = JsonSlurper().parse(googleServicesFile) as? Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val clients = root?.get("client") as? List<Map<String, Any?>> ?: emptyList()
        for (client in clients) {
          @Suppress("UNCHECKED_CAST")
          val clientInfo = client["client_info"] as? Map<String, Any?>
          @Suppress("UNCHECKED_CAST")
          val androidInfo = clientInfo?.get("android_client_info") as? Map<String, Any?>
          val pkgName = androidInfo?.get("package_name") as? String
          @Suppress("UNCHECKED_CAST")
          val oauthClients = client["oauth_client"] as? List<Map<String, Any?>> ?: emptyList()
          val type3ClientId = oauthClients.firstOrNull {
            (it["client_type"] as? Number)?.toInt() == 3 &&
              !(it["client_id"] as? String).isNullOrBlank()
          }?.get("client_id") as? String

          if (!type3ClientId.isNullOrBlank() && fallbackWebClientId.isBlank()) {
            fallbackWebClientId = type3ClientId
          }
          if (pkgName == appId && !type3ClientId.isNullOrBlank()) {
            pluginGeneratesWebClientId = true
          }

          if (fallbackWebClientId.isBlank()) {
            @Suppress("UNCHECKED_CAST")
            val services = client["services"] as? Map<String, Any?>
            @Suppress("UNCHECKED_CAST")
            val appInvite = services?.get("appinvite_service") as? Map<String, Any?>
            @Suppress("UNCHECKED_CAST")
            val otherClients = appInvite?.get("other_platform_oauth_client") as? List<Map<String, Any?>> ?: emptyList()
            val otherType3 = otherClients.firstOrNull {
              (it["client_type"] as? Number)?.toInt() == 3 &&
                !(it["client_id"] as? String).isNullOrBlank()
            }?.get("client_id") as? String
            if (!otherType3.isNullOrBlank()) {
              fallbackWebClientId = otherType3
            }
          }
        }
      }
    }
    if (!pluginGeneratesWebClientId) {
      resValue("string", "default_web_client_id", fallbackWebClientId)
    }
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    val debugKeystore = file("${rootDir}/debug.keystore")
    if (debugKeystore.exists()) {
      create("debugConfig") {
        storeFile = debugKeystore
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfigs.findByName("debugConfig")?.let {
        signingConfig = it
      }
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
    resValues = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
  packaging {
    resources {
      excludes += listOf(
        "META-INF/DEPENDENCIES",
        "META-INF/LICENSE",
        "META-INF/LICENSE.txt",
        "META-INF/license.txt",
        "META-INF/NOTICE",
        "META-INF/NOTICE.txt",
        "META-INF/notice.txt",
        "META-INF/ASL2.0"
      )
    }
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  implementation(platform("io.grpc:grpc-bom:1.65.1"))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation("androidx.biometric:biometric:1.2.0-alpha05")
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
    implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  implementation(libs.firebase.firestore)
  implementation(libs.firebase.storage)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
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
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  // implementation("com.k2-fsa:sherpa-onnx:1.10.30")
  implementation("androidx.media3:media3-exoplayer:1.3.0")
  implementation("androidx.media3:media3-common:1.3.0")
  implementation("com.google.mlkit:text-recognition:16.0.1")
  implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0")
  implementation("com.github.TomRoush:PdfBox-Android:2.0.7.0")

  // WorkManager
  implementation(libs.androidx.work.runtime.ktx)

  // Jetpack Glance AppWidgets
  implementation(libs.androidx.glance.appwidget)
  implementation(libs.androidx.glance.material3)

  // Google Drive REST API & Google Auth
  implementation(libs.google.api.services.drive)
  implementation(libs.google.api.client.android)
  implementation(libs.google.http.client.gson)
  implementation(libs.play.services.auth)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

tasks.withType<Test> {
  failFast = true
  timeout.set(Duration.ofMinutes(2))
  testLogging { events("passed", "failed", "skipped") }
}

