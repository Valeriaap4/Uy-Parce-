plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // Se aplica abajo solo si existe google-services.json (se descarga de la consola de Firebase).
    alias(libs.plugins.google.services) apply false
}

// El plugin de Google Services lee app/google-services.json y deja lista la configuración de Firebase.
// Ese archivo no se sube al repositorio: cada persona del equipo lo descarga de la consola.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "me.valeriaacosta.uyparceshopper"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "me.valeriaacosta.uyparceshopper"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.compose)

    // Firebase: cuentas (Auth) y base de datos (Firestore). El BoM fija versiones compatibles entre sí.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    // await() para usar las tareas de Firebase con corrutinas
    implementation(libs.kotlinx.coroutines.play.services)
    // Lector de códigos de barras de Google (no necesita permiso de cámara)
    implementation(libs.play.services.code.scanner)
    // Mostrar las fotos guardadas
    implementation(libs.coil.compose)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
