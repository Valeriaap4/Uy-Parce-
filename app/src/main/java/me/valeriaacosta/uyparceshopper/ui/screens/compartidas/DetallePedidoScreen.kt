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
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.fecha
import me.valeriaacosta.uyparceshopper.data.fechaHora
import me.valeriaacosta.uyparceshopper.data.mensaje
import me.valeriaacosta.uyparceshopper.data.models.*
import me.valeriaacosta.uyparceshopper.data.tiempoRestante
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── DetallePedidoScreen ─────────────────────────────────────────────────────

/** Pantalla 14. Seguimiento del pedido (en vivo). La ve el cliente y también el equipo. */
@Composable
fun DetallePedidoScreen(
    numero: Int,
    onClickBack: () -> Unit,
    onClickPagar: () -> Unit,
    onClickFactura: () -> Unit,
    onClickFaltante: () -> Unit
) {
    SegunCarga(rememberPedido(numero), "Pedido #$numero", onClickBack) { p ->
        DetallePedidoContenido(p, onClickBack, onClickPagar, onClickFactura, onClickFaltante)
    }
}

@Composable
private fun DetallePedidoContenido(
    pedido: Pedido,
    onClickBack: () -> Unit,
    onClickPagar: () -> Unit,
    onClickFactura: () -> Unit,
    onClickFaltante: () -> Unit
) {
    val estado = LocalEstadoApp.current
    val mensaje = LocalMensaje.current
    val scope = rememberCoroutineScope()
    val esCliente = (estado.rol ?: Rol.CLIENTE) == Rol.CLIENTE
    var cancelar by remember { mutableStateOf(false) }
    var trabajando by remember { mutableStateOf(false) }
    val pasoActual = EstadoPedido.lineaDeTiempo.indexOf(pedido.estado)
    val pendientes = pedido.faltantesSinResolver

    fun entregar() {
        trabajando = true
        scope.launch {
            estado.confirmarEntrega(pedido.numero)
                .onSuccess { mensaje(if (esCliente) "¡Gracias! Pedido entregado" else "Pedido marcado como entregado") }
                .onFailure { mensaje(it.mensaje()) }
            trabajando = false
        }
    }

    UpScreen(
        titulo = "Pedido #${pedido.numero}",
        onClickBack = onClickBack,
        subtitulo = fecha(pedido.fecha),
        barraInferior = when {
            esCliente && pedido.estado == EstadoPedido.PENDIENTE_PAGO && !pedido.vencido() -> ({
                BotonSecundario("Cancelar", { cancelar = true }, Modifier.weight(1f), color = Red, cargando = trabajando)
                BotonPrincipal("Pagar", onClickPagar, Modifier.weight(1.4f), habilitado = !trabajando)
            })
            pedido.estado == EstadoPedido.ENVIADO -> ({
                BotonPrincipal(
                    if (esCliente) "Recibí mi pedido" else "Marcar entregado", { entregar() },
                    Modifier.fillMaxWidth(), Icons.Outlined.CheckCircle, cargando = trabajando
                )
            })
            else -> null
        }
    ) {
        ContenidoScroll {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChipEstado(pedido.estado)
                pedido.cliente.tipoCliente?.let { Chip(it.etiqueta, TonoChip.GRIS) }
            }
            if (!esCliente) Tarjeta {
                FilaDato("Cliente", pedido.cliente.nombre, true)
                FilaDato("Teléfono", pedido.cliente.telefono)
                FilaDato("Ciudad", pedido.cliente.ciudad)
                FilaDato("Dirección", pedido.cliente.direccion)
            }
            if (pedido.estado == EstadoPedido.PENDIENTE_PAGO) {
                val restante = tiempoRestante(pedido.venceEn, Timestamp.now())
                pedido.motivoRechazo?.let { Aviso("Pago rechazado: $it. Envía otro comprobante.", TonoChip.ROJO) }
                Aviso(
                    if (restante != null) "Tienes $restante para pagar (hasta ${fechaHora(pedido.venceEn)})."
                    else "Se pasó el plazo para pagar: el pedido se cancela.",
                    if (restante != null) TonoChip.AMARILLO else TonoChip.ROJO, Icons.Outlined.Schedule
                )
            }
            if (pedido.estado == EstadoPedido.CANCELADO) Aviso(
                "Pedido cancelado" + (pedido.motivoCancelacion?.let { ": ${it.lowercase()}" } ?: "") + ".",
                TonoChip.ROJO, Icons.Outlined.Block
            )
            if (pendientes.isNotEmpty() && esCliente) {
                Aviso(
                    if (pendientes.size == 1) "Hay una prenda que no vas a recibir. Elige qué hacer con ese dinero."
                    else "Hay ${pendientes.size} faltantes. Elige qué hacer con ese dinero.",
                    TonoChip.ROJO, Icons.Outlined.ErrorOutline
                )
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
            if (pasoActual >= 0) LineaDeTiempo(pasoActual)
            TarjetaPrendas(pedido, mostrarAvance = !esCliente)
            if (pedido.faltantes.isNotEmpty()) TarjetaFaltantes(pedido)
            TarjetaDesglose(pedido.desglose, "Pago", pedido.saldoAplicado)
            if (pedido.pagadoEn != null)
                BotonSecundario("Ver factura", onClickFactura, Modifier.fillMaxWidth(), Icons.Outlined.Description)
        }
    }
    if (cancelar) DialogoConfirmar(
        "Cancelar pedido", "El pedido #${pedido.numero} se cancelará. Esta acción no se puede deshacer.", "Cancelar pedido",
        {
            trabajando = true
            scope.launch {
                estado.cancelarPedido(pedido.numero)
                    .onSuccess { mensaje("Pedido cancelado") }
                    .onFailure { mensaje(it.mensaje()) }
                trabajando = false
            }
        }, { cancelar = false }, peligro = true
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
private fun TarjetaPrendas(pedido: Pedido, mostrarAvance: Boolean) {
    Tarjeta {
        Text("Prendas", style = MaterialTheme.typography.titleMedium)
        pedido.lineas.forEach { l ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FotoProducto(l.producto, Modifier.size(48.dp), 12.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(l.producto.nombre, fontWeight = FontWeight.SemiBold)
                    Text("Talla ${l.talla} · ${if (l.cantidad == 1) "1 unidad" else "${l.cantidad} unidades"}", color = Muted, fontSize = 14.sp)
                    if (mostrarAvance) Text(
                        "Compradas ${l.compradas} · empacadas ${l.empacadas} · recibidas ${l.recibidas}" +
                            (if (l.noConseguidas > 0) " · no conseguidas ${l.noConseguidas}" else ""),
                        color = Muted, fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// ── Faltantes y qué se hizo con cada uno ──
@Composable
private fun TarjetaFaltantes(pedido: Pedido) {
    Tarjeta {
        Text("Faltantes", style = MaterialTheme.typography.titleMedium)
        pedido.faltantes.forEach { f ->
            val r = pedido.resoluciones[f.id]
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(f.detalle, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Dinero(f.valor, f.moneda, 14.sp)
                }
                Text(
                    f.motivo + " · " + when {
                        r == null -> "sin resolver"
                        r.tipo == TipoResolucion.SALDO_A_FAVOR -> "saldo a favor"
                        f.devuelto -> "devolución hecha"
                        else -> "devolución pendiente"
                    },
                    color = if (r == null) Red else Muted, fontSize = 13.sp
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DetallePedidoScreenPreview() {
    VistaPrevia { DetallePedidoScreen(numero = 1012, onClickBack = {}, onClickPagar = {}, onClickFactura = {}, onClickFaltante = {}) }
}
