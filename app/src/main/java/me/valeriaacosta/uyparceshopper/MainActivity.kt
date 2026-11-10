package me.valeriaacosta.uyparceshopper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.activity.viewModels
import androidx.compose.ui.graphics.toArgb
import me.valeriaacosta.uyparceshopper.data.EstadoApp
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.navigation.AppNavigation
import me.valeriaacosta.uyparceshopper.ui.theme.Navy
import me.valeriaacosta.uyparceshopper.ui.theme.UyParceTheme

class MainActivity : ComponentActivity() {
    // La sesión vive en un ViewModel: sobrevive al girar el celular.
    private val estado: EstadoApp by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Barra de estado con íconos claros sobre la cabecera azul marino.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Navy.toArgb()))
        setContent {
            UyParceTheme {
                CompositionLocalProvider(LocalEstadoApp provides estado) {
                    AppNavigation()
                }
            }
        }
    }
}
