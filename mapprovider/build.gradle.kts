plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.aleejaved.locallines.maps"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aleejaved.locallines.maps"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.watchface.complications.data.source.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.maplibre.android)
    implementation(libs.play.services.location)
    implementation(libs.work.runtime.ktx)
    testImplementation(libs.junit)
}
