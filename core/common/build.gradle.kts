plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.lkz.affa.core.common"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":domain"))
    testImplementation(libs.junit)
}
