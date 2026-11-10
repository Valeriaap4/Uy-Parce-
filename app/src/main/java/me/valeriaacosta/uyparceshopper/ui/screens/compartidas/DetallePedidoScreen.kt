package me.valeriaacosta.uyparceshopper.ui.screens.compartidas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import me.valeriaacosta.uyparceshopper.data.models.EstadoPedido
import me.valeriaacosta.uyparceshopper.data.models.Pedido
import me.valeriaacosta.uyparceshopper.data.models.Precios
import me.valeriaacosta.uyparceshopper.data.models.Rol
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── DetallePedidoScreen ─────────────────────────────────────────────────────

/** Pantalla 14. Seguimiento del pedido. La ve el cliente y también el supervisor y el admin. */
@Composable
fun DetallePedidoScreen(
    numero: Int,
    onClickBack: () -> Unit,
    onClickPagar: () -> Unit,
    onClickFactura: () -> Unit,
    onClickFaltante: () -> Unit
) {
    val estado = LocalEstadoApp.current
    val pedido = DatosEjemplo.buscarPedido(numero) ?: return NoEncontrado("Pedido #$numero", onClickBack)
    val esCliente = (estado.rol ?: Rol.CLIENTE) == Rol.CLIENTE
    val d = Precios.desglose(pedido, estado.ajustes)
    val mensaje = LocalMensaje.current
    var cancelar by remember { mutableStateOf(false) }
    val pasoActual = EstadoPedido.lineaDeTiempo.indexOf(pedido.estado)
    val hayFaltante = pedido.estado == EstadoPedido.EN_CUCUTA && pedido.lineas.any { it.recibidas < it.cantidad }

    UpScreen(
        titulo = "Pedido #$numero",
        onClickBack = onClickBack,
        subtitulo = pedido.fecha,
        barraInferior = when {
            !esCliente -> null
            pedido.estado == EstadoPedido.PENDIENTE_PAGO -> ({
                BotonSecundario("Cancelar", { cancelar = true }, Modifier.weight(1f), color = Red)
                BotonPrincipal("Pagar", onClickPagar, Modifier.weight(1.4f))
            })
            pedido.estado == EstadoPedido.ENVIADO -> ({
                BotonPrincipal("Recibí mi pedido", { mensaje("¡Gracias! Pedido entregado") }, Modifier.fillMaxWidth(), Icons.Outlined.CheckCircle)
            })
            else -> null
        }
    ) {
        ContenidoScroll {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChipEstado(pedido.estado)
                Chip(pedido.cliente.tipoCliente?.etiqueta ?: "", TonoChip.GRIS)
            }
            if (!esCliente) Tarjeta {
                FilaDato("Cliente", pedido.cliente.nombre, true)
                FilaDato("Ciudad", pedido.cliente.ciudad)
                FilaDato("Dirección", pedido.cliente.direccion)
            }
            pedido.motivoRechazo?.let { Aviso("Pago rechazado: $it. Sube otro comprobante.", TonoChip.ROJO) }
            if (hayFaltante && esCliente) {
                Aviso("Una prenda de tu pedido no llegó. Elige qué hacer con ese dinero.", TonoChip.ROJO, Icons.Outlined.ErrorOutline)
                BotonAmarillo("Resolver faltante", onClickFaltante, Modifier.fillMaxWidth())
            }
            if (pedido.guia != null) Tarjeta {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.LocalShipping, null, tint = Blue)
                    Column {
                        Text(pedido.transportadora ?: "", fontWeight = FontWeight.Bold)
                        Text("Guía ${pedido.guia}", fontFamily = Mono)
                    }
                }
            }
            LineaDeTiempo(pasoActual)
            TarjetaPrendas(pedido)
            TarjetaDesglose(d, "Pago")
            if (pasoActual >= EstadoPedido.PAGADO.ordinal)
                BotonSecundario("Ver factura", onClickFactura, Modifier.fillMaxWidth(), Icons.Outlined.Description)
        }
    }
    if (cancelar) DialogoConfirmar(
        "Cancelar pedido", "El pedido #$numero se cancelará. Esta acción no se puede deshacer.", "Cancelar pedido",
        { mensaje("Pedido cancelado"); onClickBack() }, { cancelar = false }, peligro = true
    )
}

// ── Línea de tiempo de estados ──
@Composable
private fun LineaDeTiempo(pasoActual: Int) {
    Tarjeta {
        Text("Seguimiento", style = MaterialTheme.typography.titleMedium)
        EstadoPedido.lineaDeTiempo.forEachIndexed { i, e ->
            val hecho = i <= pasoActual
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(22.dp).clip(CircleShape).background(if (hecho) Green else GreySoft),
                    contentAlignment = Alignment.Center
                ) {
                    if (hecho) Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
                Text(
                    e.etiqueta,
                    fontWeight = if (i == pasoActual) FontWeight.Bold else FontWeight.Normal,
                    color = if (hecho) Ink else Muted
                )
            }
        }
    }
}

// ── Prendas del pedido ──
@Composable
private fun TarjetaPrendas(pedido: Pedido) {
    Tarjeta {
        Text("Prendas", style = MaterialTheme.typography.titleMedium)
        pedido.lineas.forEach { l ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FotoProducto(l.producto, Modifier.size(48.dp), 12.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(l.producto.nombre, fontWeight = FontWeight.SemiBold)
                    Text("Talla ${l.talla} · ${l.cantidad} und.", color = Muted, fontSize = 14.sp)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DetallePedidoScreenPreview() {
    UyParceTheme { DetallePedidoScreen(numero = 1012, onClickBack = {}, onClickPagar = {}, onClickFactura = {}, onClickFaltante = {}) }
}
