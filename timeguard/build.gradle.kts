import java.util.Properties

plugins {
    id("com.android.library")
}

val serverProperties = Properties().apply {
    rootProject.file("config/server.properties").inputStream().use { load(it) }
}

fun serverProperty(name: String, fallback: String): String =
    serverProperties.getProperty(name, fallback)

android {
    namespace = "com.example.timeguard"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        targetSdk = 36
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("String", "SISTER_SERVER_HOST", "\"${serverProperty("server.host", "10.0.2.2")}\"")
        buildConfigField("String", "SISTER_SERVER_PORT", "\"${serverProperty("server.port", "3000")}\"")
        buildConfigField("String", "SISTER_DETECTOR_PORT", "\"${serverProperty("detector.port", "8080")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core:1.13.1")
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
