plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.sensoresmapa"

    // Si te compila así, déjalo. Si te da error, cambia por: compileSdk = 34
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.sensoresmapa"
        minSdk = 24
        // Si falla con 36, usa: targetSdk = 34
        targetSdk = 36

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    implementation("com.google.android.gms:play-services-maps:18.2.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
