import org.gradle.api.tasks.testing.logging.TestLogEvent
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.compose.compiler)
    id("kotlin-parcelize")
    id("kotlinx-serialization")
}

kotlin {
    jvmToolchain(17)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
keystoreProperties.load(FileInputStream(keystorePropertiesFile))

android {
    namespace = "io.github.soundremote"
    compileSdk = 37
    defaultConfig {
        applicationId = "io.github.soundremote"
        minSdk = 23
        targetSdk = 36
        versionCode = 12
        versionName = "0.5.0"
        testInstrumentationRunner = "io.github.soundremote.CustomTestRunner"
    }
    signingConfigs {
        create("release config") {
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release config")
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    sourceSets.getByName("androidTest") {
        kotlin.directories += "$projectDir/schemas"
    }
    lint {
        warning.add("MissingTranslation")
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events(TestLogEvent.FAILED)
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.media)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.ktx)  // For the predictive back gesture
    implementation(libs.bundles.androidx.lifeycle)
// Compose
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    // UI
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive)
    // Android Studio Preview support
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    // UI Tests
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
// Instrumented tests
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.androidx.test.ktx)
    androidTestImplementation(libs.androidx.navigation.testing)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotest.assertions.core)
// Local tests
    testImplementation(libs.bundles.local.tests)
    testRuntimeOnly(libs.junit.platform.launcher)
// JOpus
    implementation(libs.jopus)
// Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
// Preference datastore
    implementation(libs.androidx.datastore.preferences)
// Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
// Navigation
    implementation(libs.androidx.navigation.compose)
// Serialization
    implementation(libs.kotlinx.serialization.json)
// Accompanist
    implementation(libs.accompanist.permissions)
// Guava
    implementation(libs.guava)
// Seismic
    implementation(libs.seismic)
// Timber
    implementation(libs.timber)
}
