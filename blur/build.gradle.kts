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
    namespace = "com.example.overlaydetector"
    compileSdk {
        version = release(36)
    }

    defaultConfig {

        minSdk = 26
        targetSdk = 36


        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    
}

dependencies {
    implementation("androidx.core:core:1.13.1")
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // OkHttp для HTTP-запросов
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

}
