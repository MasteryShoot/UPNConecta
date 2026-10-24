import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// La API key de Google Maps se lee desde local.properties (MAPS_API_KEY=...)
// para no subirla al repositorio.
val propiedadesLocales = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}

android {
    namespace = "com.example.upnconecta"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.upnconecta"
        minSdk = 24
        targetSdk = 37
        versionCode = 2
        versionName = "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["MAPS_API_KEY"] =
            propiedadesLocales.getProperty("MAPS_API_KEY", "")

        // URL del backend (Spring Boot). Por defecto usa el servicio publicado en Render;
        // para probar con el backend en la PC se define API_URL=http://10.0.2.2:8080/api/ en local.properties
        val apiUrl = propiedadesLocales.getProperty("API_URL", "https://upnconecta-backend.onrender.com/api/")
        buildConfigField("String", "API_URL", "\"$apiUrl\"")

        // Id de la app registrada en Microsoft Entra ID (portal de Azure), cuenta gratuita.
        // No es un secreto (la app es un cliente público con PKCE); se puede cambiar en local.properties
        val msClientId = propiedadesLocales.getProperty("MS_CLIENT_ID", "26c34a39-4a53-4350-98f8-9cf9d7faf639")
        buildConfigField("String", "MS_CLIENT_ID", "\"$msClientId\"")
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
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.maps.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // Consumo del backend con Retrofit (S09)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
