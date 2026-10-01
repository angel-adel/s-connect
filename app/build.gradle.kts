plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.10"
}

android {
    namespace = "com.adel.s_connect"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.adel.s_connect"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Supabase
        buildConfigField("String", "SUPABASE_URL", "\"https://wgewycchecbsulyvqfzj.supabase.co\"")
        buildConfigField("String", "SUPABASE_KEY", "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6IndnZXd5Y2NoZWNic3VseXZxZnpqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODU4NTg1NTYsImV4cCI6MjEwMTQzNDU1Nn0.y80cZy7KiLWKJ1Grh2lkjUdtCGsBos6uC6cNJmRiCTs\"")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("io.coil-kt:coil-compose:2.7.0")


    // Supabase
    implementation("io.github.jan-tennert.supabase:postgrest-kt:2.6.0")
    implementation("io.github.jan-tennert.supabase:gotrue-kt:2.6.0")
    implementation("io.github.jan-tennert.supabase:realtime-kt:2.6.0")
    implementation("io.github.jan-tennert.supabase:storage-kt:2.6.0")
    implementation("io.ktor:ktor-client-okhttp:2.3.12")
    implementation("io.ktor:ktor-client-core:2.3.12")
    implementation("io.ktor:ktor-client-websockets:2.3.12")


    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}