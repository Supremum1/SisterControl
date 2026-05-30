import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
}

val serverProperties = Properties().apply {
    rootProject.file("config/server.properties").inputStream().use { load(it) }
}

fun serverProperty(name: String, fallback: String): String =
    serverProperties.getProperty(name, fallback)

android {
    namespace = "com.example.childassistant"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("String", "SISTER_SERVER_HOST", "\"${serverProperty("server.host", "10.0.2.2")}\"")
        buildConfigField("String", "SISTER_SERVER_PORT", "\"${serverProperty("server.port", "3000")}\"")
        buildConfigField("String", "SISTER_DETECTOR_PORT", "\"${serverProperty("detector.port", "8080")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("androidx.core:core:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
}
