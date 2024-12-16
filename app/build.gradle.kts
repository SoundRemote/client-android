import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.compose.compiler)
    id("kotlin-parcelize")
    id("kotlin-kapt")
    id("kotlinx-serialization")
}

kotlin {
    jvmToolchain(17)
}

android {
    namespace = "io.github.soundremote"
    compileSdk = 35
    defaultConfig {
        applicationId = "io.github.soundremote"
        minSdk = 21
        targetSdk = 35
        versionCode = 8
        versionName = "0.4.3"
        testInstrumentationRunner = "io.github.soundremote.CustomTestRunner"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    ksp {
        arg("room.generateKotlin", "true")
    }
    buildToolsVersion = "35.0.0"
    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
    lint {
        warning.add("MissingTranslation")
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
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.media)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
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
// Local tests
    testImplementation(libs.bundles.local.tests)
// JOpus
    implementation(libs.jopus)
// Room
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
// Preference datastore
    implementation(libs.androidx.datastore.preferences)
// Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    androidTestImplementation(libs.hilt.android.testing)
    kaptAndroidTest(libs.hilt.compiler)
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
