plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.lkz.affa.core.network"
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
    implementation(project(":core:common"))
    implementation(libs.jsoup)
    testImplementation(libs.junit)
}
