import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import groovy.json.JsonSlurper
import java.time.Duration

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
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
    var fallbackWebClientId = ""
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

