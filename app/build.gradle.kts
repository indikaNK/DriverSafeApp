import org.gradle.kotlin.dsl.annotationProcessor

plugins {
            alias(libs.plugins.android.application)
            alias(libs.plugins.kotlin.android)
            alias(libs.plugins.kotlin.compose)
            alias(libs.plugins.google.gms.google.services)


        }

android {
    namespace = "com.example.driversafeapp_application"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.driversafeapp_application"
        minSdk = 24
        targetSdk = 35
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = false
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.material)
    implementation(libs.androidx.core.ktx.v1131)
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation(libs.material)
    //google maps API

    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    implementation(libs.google.maps.utils)  // Add this for PolyUtil
    implementation(libs.google.maps)
    implementation(libs.google.location)



    // Retrofit for OpenWeatherMap API
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    implementation ("com.google.android.libraries.places:places:2.6.0")


    implementation ("com.google.android.material:material:1.9.0")
    implementation ("androidx.coordinatorlayout:coordinatorlayout:1.2.0")

    implementation ("com.github.bumptech.glide:glide:4.16.0") // Latest version as of now
    annotationProcessor ("com.github.bumptech.glide:compiler:4.16.0")

}

