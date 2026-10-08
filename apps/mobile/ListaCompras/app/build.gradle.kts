import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "br.com.listacompras"
    compileSdk = 34

    // Assinatura release via keystore.properties (gitignored). Sem ele, usa chave debug.
    val ksProps = Properties().apply {
        rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
    }
    signingConfigs {
        create("release") {
            storeFile = file(ksProps.getProperty("storeFile") ?: "${System.getProperty("user.home")}/.android-keystores/listacompras-release.jks")
            storePassword = ksProps.getProperty("storePassword") ?: System.getenv("LISTACOMPRA_STORE_PASSWORD")
            keyAlias = ksProps.getProperty("keyAlias") ?: "listacompras"
            keyPassword = ksProps.getProperty("keyPassword") ?: System.getenv("LISTACOMPRA_KEY_PASSWORD") ?: storePassword
        }
    }

    defaultConfig {
        applicationId = "br.com.listacompras"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    // Room schema exportado para versionamento (NFR-003)
    ksp { arg("room.schemaLocation", "$projectDir/schemas") }
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.runtime)
    implementation(libs.navigation.compose)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.datastore.prefs)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.coroutines.core)
    implementation(libs.serialization.json)
    ksp(libs.room.compiler)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit5.api)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
}
