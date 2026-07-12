plugins {
    alias(libs.plugins.android.application)
}

android {
    enableKotlin = false
    namespace = "com.aleejaved.locallines.watchface"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aleejaved.locallines.watchface"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        debug { isMinifyEnabled = true }
        release {
            isMinifyEnabled = true
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}
