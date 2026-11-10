package me.valeriaacosta.uyparceshopper.ui.screens.cliente

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Upload
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
import me.valeriaacosta.uyparceshopper.data.DatosEjemplo
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.models.Precios
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── PagarPedidoScreen ───────────────────────────────────────────────────────

/** Pantalla 11. Total a pagar, cuentas del negocio y contador de 24 horas. */
@Composable
fun PagarPedidoScreen(
    numero: Int,
    onClickBack: () -> Unit,
    onClickReportar: () -> Unit
) {
    val estado = LocalEstadoApp.current
    val pedido = DatosEjemplo.buscarPedido(numero) ?: return NoEncontrado("Pedido #$numero", onClickBack)
    val d = Precios.desglose(pedido, estado.ajustes)
    val mensaje = LocalMensaje.current

    UpScreen("Pagar pedido #$numero", onClickBack, barraInferior = {
        BotonPrincipal("Ya pagué, reportar pago", onClickReportar, Modifier.fillMaxWidth(), Icons.Outlined.Upload)
    }) {
        ContenidoScroll {
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Navy).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Total a pagar", color = NavyText)
                Dinero(d.total, d.moneda, 30.sp, Color.White)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Timer, null, tint = Yellow, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Te quedan 23 h 41 min para pagar", color = Yellow, fontWeight = FontWeight.SemiBold)
                }
            }
            Text("Transfiere el valor exacto a cualquiera de estas cuentas:", color = Muted)
            DatosEjemplo.mediosPago.forEach { m ->
                Tarjeta {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(m.nombre, fontWeight = FontWeight.Bold)
                            Text(m.numero, fontFamily = Mono, fontSize = 17.sp)
                            Text("A nombre de ${m.titular}", color = Muted, fontSize = 13.sp)
                        }
                        IconButton(onClick = { mensaje("Número copiado") }) {
                            Icon(Icons.Outlined.ContentCopy, "Copiar", tint = Blue)
                        }
                    }
                }
            }
            Aviso("No hay abonos: se paga el total. Si no reportas el pago en 24 horas, el pedido se cancela.", TonoChip.AMARILLO)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PagarPedidoScreenPreview() {
    UyParceTheme { PagarPedidoScreen(numero = 1016, onClickBack = {}, onClickReportar = {}) }
}
