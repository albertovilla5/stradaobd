import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.isFile) {
        keystorePropsFile.inputStream().use(::load)
    }
}
val releaseStoreFile = (keystoreProps.getProperty("storeFile") ?: System.getenv("STRADAOBD_KEYSTORE_FILE"))
    ?.takeIf { it.isNotBlank() }
    ?.let(rootProject::file)
val hasReleaseKeystore = releaseStoreFile != null && releaseStoreFile.isFile
val isBundleTask = gradle.startParameter.taskNames.any { it.contains("bundle", ignoreCase = true) }

android {
    namespace = "com.torqueobd2.dash"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.stradaobd.dash"
        minSdk = 23 // Android 6+: also supports head units running an older system.
        targetSdk = 35
        versionCode = 15
        versionName = "1.6.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    androidResources {
        localeFilters += listOf("es", "en", "fr", "de", "pt", "it")
    }

    signingConfigs {
        getByName("debug") {
            // Include the classic signature for head units with older package installers.
            enableV1Signing = true
            enableV2Signing = true
        }
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = keystoreProps.getProperty("storePassword") ?: System.getenv("STRADAOBD_KEYSTORE_PASSWORD")
                keyAlias = keystoreProps.getProperty("keyAlias") ?: System.getenv("STRADAOBD_KEY_ALIAS")
                keyPassword = keystoreProps.getProperty("keyPassword") ?: System.getenv("STRADAOBD_KEY_PASSWORD")
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = if (hasReleaseKeystore) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            ndk {
                debugSymbolLevel = "FULL"
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    packaging {
        // Keep legacy JNI packaging for direct APK installs on older head units,
        // while letting Play Store App Bundles (.aab) use modern 16 KB page-aligned uncompressed packaging.
        jniLibs.useLegacyPackaging = !isBundleTask
    }
    bundle {
        language {
            // Keep all supported language resources in the base APK when building an AAB for Google Play Store
            // so users can switch languages inside the app immediately without downloading split APKs.
            enableSplit = false
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    testImplementation(libs.junit)
    testImplementation(libs.org.json)
}

