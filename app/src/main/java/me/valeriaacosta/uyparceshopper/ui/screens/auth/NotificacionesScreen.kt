package me.valeriaacosta.uyparceshopper.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.valeriaacosta.uyparceshopper.data.Listas
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.hace
import me.valeriaacosta.uyparceshopper.data.models.Notificacion
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── NotificacionesScreen ────────────────────────────────────────────────────

/**
 * Pantalla 17. Avisos al cliente (en vivo): pago aprobado o rechazado, producto agotado, pedido en
 * Cúcuta, envío… Al tocar uno se marca como leído y se abre el pedido (o el carrito).
 */
@Composable
fun NotificacionesScreen(
    onClickBack: () -> Unit,
    onClickNotificacion: (Notificacion) -> Unit
) {
    val estado = LocalEstadoApp.current
    val lista = estado.notificaciones

    UpScreen("Notificaciones", onClickBack, acciones = {
        if (estado.noLeidas > 0) IconButton(onClick = { estado.marcarTodasLeidas() }) {
            Icon(Icons.Outlined.DoneAll, "Marcar todas como leídas", tint = Color.White)
        }
    }) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(lista, key = { it.id }) { n ->
                Tarjeta(alPulsar = {
                    estado.marcarLeida(n)
                    onClickNotificacion(n)
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (!n.leida) Box(Modifier.size(9.dp).clip(RoundedCornerShape(50)).background(Yellow))
                        Text(n.titulo, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(hace(n.fecha), color = Muted, fontSize = 13.sp)
                    }
                    Text(n.texto, color = Muted)
                }
            }
            if (lista.isEmpty()) item {
                if (estado.cargado(Listas.NOTIFICACIONES)) EstadoVacio(Icons.Outlined.NotificationsNone, "Todavía no tienes avisos")
                else Cargando()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NotificacionesScreenPreview() {
    VistaPrevia { NotificacionesScreen(onClickBack = {}, onClickNotificacion = {}) }
}
