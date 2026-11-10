package me.valeriaacosta.uyparceshopper.ui.screens.auth

import me.valeriaacosta.uyparceshopper.data.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.valeriaacosta.uyparceshopper.data.DatosEjemplo
import me.valeriaacosta.uyparceshopper.data.models.Notificacion
import me.valeriaacosta.uyparceshopper.ui.components.Tarjeta
import me.valeriaacosta.uyparceshopper.ui.components.UpScreen
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── NotificacionesScreen ────────────────────────────────────────────────────

/** Pantalla 17. Lista de avisos al cliente (también le llegan como push). */
@Composable
fun NotificacionesScreen(
    onClickBack: () -> Unit,
    onClickNotificacion: (Notificacion) -> Unit
) {
    UpScreen("Notificaciones", onClickBack) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(DatosEjemplo.notificaciones) { n ->
                Tarjeta(alPulsar = { onClickNotificacion(n) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (!n.leida) Box(Modifier.size(9.dp).clip(RoundedCornerShape(50)).background(Yellow))
                        Text(n.titulo, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(n.hace, color = Muted, fontSize = 13.sp)
                    }
                    Text(n.texto, color = Muted)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NotificacionesScreenPreview() {
    UyParceTheme { NotificacionesScreen(onClickBack = {}, onClickNotificacion = {}) }
}
