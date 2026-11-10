package me.valeriaacosta.uyparceshopper.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.valeriaacosta.uyparceshopper.ui.components.LogoUyParce
import me.valeriaacosta.uyparceshopper.ui.components.VistaPrevia
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── SplashScreen ────────────────────────────────────────────────────────────

/**
 * Pantalla 1. Muestra el logo mientras Firebase revisa si hay una sesión abierta.
 * A dónde ir después lo decide AppNavigation según la sesión (inicio del rol, revisión o login).
 */
@Composable
fun SplashScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy)
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LogoUyParce(alto = 150.dp)
        Spacer(modifier = Modifier.height(20.dp))
        Text("UyParce Shopper", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Text("Tu ropa de outlets de EE. UU.", color = NavyText)
        Spacer(modifier = Modifier.height(32.dp))
        CircularProgressIndicator(color = Yellow, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    VistaPrevia { SplashScreen() }
}
