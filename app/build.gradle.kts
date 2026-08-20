plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.zion.mobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.zion.mobile"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
    }
}

kotlin {
    jvmToolchain(25)
}
