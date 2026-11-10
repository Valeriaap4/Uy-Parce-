package me.valeriaacosta.uyparceshopper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toArgb
import com.google.firebase.FirebaseApp
import me.valeriaacosta.uyparceshopper.data.EstadoApp
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.navigation.AppNavigation
import me.valeriaacosta.uyparceshopper.ui.screens.auth.FaltaFirebaseScreen
import me.valeriaacosta.uyparceshopper.ui.theme.Navy
import me.valeriaacosta.uyparceshopper.ui.theme.UyParceTheme

class MainActivity : ComponentActivity() {
    // El estado vive en un ViewModel: la sesión y los datos sobreviven al girar el celular.
    private val estado: EstadoApp by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Barra de estado con íconos claros sobre la cabecera azul marino.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Navy.toArgb()))
        // Sin google-services.json no hay Firebase: se muestra cómo arreglarlo en vez de cerrarse.
        val hayFirebase = FirebaseApp.getApps(this).isNotEmpty()
        if (hayFirebase) estado.iniciar()
        setContent {
            UyParceTheme {
                if (!hayFirebase) FaltaFirebaseScreen()
                else CompositionLocalProvider(LocalEstadoApp provides estado) { AppNavigation() }
            }
        }
    }
}
