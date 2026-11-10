package me.valeriaacosta.uyparceshopper.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.ui.components.BotonSecundario
import me.valeriaacosta.uyparceshopper.ui.components.FondoMarca
import me.valeriaacosta.uyparceshopper.ui.components.VistaPrevia
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── CuentaRevisionScreen ────────────────────────────────────────────────────

/**
 * Pantalla 4. La cuenta espera la aprobación del admin. La app escucha la cuenta en vivo: en cuanto
 * el admin la aprueba, entra sola al catálogo.
 */
@Composable
fun CuentaRevisionScreen(
    onClickVolver: () -> Unit
) {
    val nombre = LocalEstadoApp.current.usuario?.nombre?.substringBefore(" ")
    FondoMarca(if (nombre != null) "¡Listo, $nombre!" else "¡Listo, parce!", "Recibimos tu registro") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(50)).background(AmberSoft),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Outlined.HourglassTop, null, tint = Amber) }
            Text("Tu cuenta está en revisión", style = MaterialTheme.typography.titleMedium)
        }
        Text(
            "El administrador revisa tus datos y te asigna tu tipo de cliente (mayorista o detal). " +
                "Si dejas esta pantalla abierta, la app entra sola cuando aprueben tu cuenta. " +
                "Si sales, vuelve a iniciar sesión más tarde con tu correo y contraseña.",
            color = Muted
        )
        BotonSecundario("Salir", onClickVolver, Modifier.fillMaxWidth())
    }
}

@Preview(showBackground = true)
@Composable
fun CuentaRevisionScreenPreview() {
    VistaPrevia { CuentaRevisionScreen(onClickVolver = {}) }
}
