import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val serverProperties = Properties().apply {
    rootProject.file("config/server.properties").inputStream().use { load(it) }
}

fun serverProperty(name: String, fallback: String): String =
    serverProperties.getProperty(name, fallback)

android {
    namespace = "com.example.childapp"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.childapp"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

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
    buildFeatures{
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
    //implementation(libs.androidx.material3)
    //implementation(libs.androidx.compose.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.14")
    implementation(project(":blur"))
    implementation(project(":blocker"))
    implementation(project(":timeguard"))
    implementation(project(":childassistant"))
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    implementation("com.google.android.material:material:1.12.0")
}
