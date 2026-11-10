package me.valeriaacosta.uyparceshopper.ui.screens.auth

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import me.valeriaacosta.uyparceshopper.ui.components.Aviso
import me.valeriaacosta.uyparceshopper.ui.components.FondoMarca
import me.valeriaacosta.uyparceshopper.ui.components.TonoChip
import me.valeriaacosta.uyparceshopper.ui.theme.Muted
import me.valeriaacosta.uyparceshopper.ui.theme.UyParceTheme

// ─── FaltaFirebaseScreen ─────────────────────────────────────────────────────

/**
 * Se muestra si la app se compiló sin app/google-services.json (por ejemplo, alguien del equipo
 * clonó el repositorio). En vez de cerrarse, explica cómo arreglarlo.
 */
@Composable
fun FaltaFirebaseScreen() {
    FondoMarca("UyParce Shopper", "Falta conectar Firebase") {
        Aviso("Esta versión de la app se compiló sin la configuración de Firebase.", TonoChip.ROJO, Icons.Outlined.ErrorOutline)
        Text(
            "Descarga google-services.json desde la consola de Firebase (Configuración del proyecto → " +
                "Tus apps → Android), cópialo en la carpeta app del proyecto y vuelve a compilar.",
            color = Muted
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FaltaFirebaseScreenPreview() {
    UyParceTheme { FaltaFirebaseScreen() }
}
