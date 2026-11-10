package me.valeriaacosta.uyparceshopper.ui.screens.supervisor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.fechaHora
import me.valeriaacosta.uyparceshopper.data.mensaje
import me.valeriaacosta.uyparceshopper.data.models.*
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*
import kotlin.math.abs

// ─── RevisarPagoScreen ───────────────────────────────────────────────────────

/**
 * Pantalla 26. El supervisor revisa el comprobante y aprueba (se genera la factura) o rechaza con
 * motivo. Antes avisa si el total o el saldo a favor usado no cuadran.
 */
@Composable
fun RevisarPagoScreen(
    id: String,
    onClickBack: () -> Unit,
    onClickVerPedido: (numero: Int) -> Unit,
    onRevisado: () -> Unit
) {
    SegunCarga(rememberPago(id), "Revisar pago", onClickBack) { pago -> RevisarContenido(pago, onClickBack, onClickVerPedido, onRevisado) }
}

@Composable
private fun RevisarContenido(pago: Pago, onClickBack: () -> Unit, onClickVerPedido: (Int) -> Unit, onRevisado: () -> Unit) {
    val estado = LocalEstadoApp.current
    val mensaje = LocalMensaje.current
    val scope = rememberCoroutineScope()
    val pedido = (rememberPedido(pago.numeroPedido) as? Carga.Listo)?.valor
    var aprobar by remember { mutableStateOf(false) }
    var rechazar by remember { mutableStateOf(false) }
    var ampliar by remember { mutableStateOf(false) }
    var trabajando by remember { mutableStateOf(false) }
    val enRevision = pago.estado == EstadoPago.EN_REVISION

    // Saldo a favor que el cliente tenía disponible (sin contar este pedido), para revisar lo que usó.
    val saldoDisponible by produceState<Double?>(null, pago.clienteId, pedido?.saldoAplicado) {
        value = if (pedido == null || pedido.saldoAplicado <= 0) null
        else estado.pedidosDeCliente(pago.clienteId).getOrNull()
            ?.let { lista -> Precios.saldoAFavor(lista.filter { it.numero != pedido.numero }, pedido.moneda) }
    }

    UpScreen(
        titulo = "Revisar pago",
        onClickBack = onClickBack,
        subtitulo = "Pedido #${pago.numeroPedido}",
        barraInferior = if (!enRevision) null else ({
            BotonSecundario("Rechazar", { rechazar = true }, Modifier.weight(1f), color = Red, habilitado = !trabajando)
            BotonPrincipal("Aprobar", { aprobar = true }, Modifier.weight(1.4f), Icons.Outlined.Check, cargando = trabajando)
        })
    ) {
        ContenidoScroll {
            val fotoId = pago.fotoId
            if (fotoId != null) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(20.dp)).background(GreySoft)
                        .clickable { ampliar = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Receipt, null, tint = Muted, modifier = Modifier.size(56.dp))
                    FotoGuardada(fotoId, Modifier.fillMaxSize(), miniatura = false, contentScale = ContentScale.Fit)
                    Text(
                        "Toca para ampliar", color = Color.White, fontSize = 13.sp,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp)
                            .clip(RoundedCornerShape(8.dp)).background(Navy.copy(alpha = 0.7f)).padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else {
                Aviso("Pagado con saldo a favor: no hay comprobante.", TonoChip.VERDE, Icons.Outlined.Savings)
            }
            when (pago.estado) {
                EstadoPago.APROBADO -> Aviso("Aprobado por ${pago.revisadoPor} · ${fechaHora(pago.revisadoEn)}", TonoChip.VERDE, Icons.Outlined.Check)
                EstadoPago.RECHAZADO -> Aviso("Rechazado por ${pago.revisadoPor}: ${pago.motivoRechazo}", TonoChip.ROJO)
                EstadoPago.EN_REVISION -> Unit
            }
            Tarjeta {
                FilaDato("Cliente", pago.clienteNombre, true)
                FilaDato("Medio", pago.medio)
                FilaDato("Referencia", pago.referencia)
                FilaDato("Fecha del pago", pago.fechaPago)
                FilaDato("Reportado", fechaHora(pago.fecha))
                HorizontalDivider(color = Line)
                FilaDinero("Total esperado", pago.monto, pago.moneda, total = true)
                if (pago.moneda == Moneda.USD && pedido != null && pedido.desglose.tasaDolar > 0) Text(
                    "En pesos: ${formatoDinero(Precios.redondear(pago.monto * pedido.desglose.tasaDolar, Moneda.COP), Moneda.COP)} COP " +
                        "(tasa ${formatoDinero(pedido.desglose.tasaDolar, Moneda.COP)} del pedido)",
                    color = Muted, fontSize = 13.sp
                )
            }
            if (enRevision && pedido != null) Verificaciones(pedido, saldoDisponible)
            if (enRevision) Aviso("Revisa en la cuenta del negocio que el dinero sí llegó antes de aprobar.", TonoChip.AZUL, Icons.Outlined.Info)
            BotonSecundario("Ver pedido", { onClickVerPedido(pago.numeroPedido) }, Modifier.fillMaxWidth())
        }
    }

    if (ampliar) pago.fotoId?.let { VisorFoto(it) { ampliar = false } }
    if (aprobar) DialogoConfirmar(
        "Aprobar pago", "Se genera la factura y el pedido pasa a Pagado. El operador ya lo podrá comprar.",
        "Aprobar", {
            trabajando = true
            scope.launch {
                estado.aprobarPago(pago)
                    .onSuccess { factura -> mensaje("Pago aprobado · factura $factura"); onRevisado() }
                    .onFailure { mensaje(it.mensaje()) }
                trabajando = false
            }
        }, { aprobar = false }
    )
    if (rechazar) DialogoRechazo(
        horas = estado.ajustes.horasParaPagar,
        onRechazar = { motivo ->
            rechazar = false
            trabajando = true
            scope.launch {
                estado.rechazarPago(pago, motivo)
                    .onSuccess { mensaje("Pago rechazado: $motivo"); onRevisado() }
                    .onFailure { mensaje(it.mensaje()) }
                trabajando = false
            }
        },
        onCerrar = { rechazar = false }
    )
}

/**
 * Revisiones antes de aprobar: el total se compara con los precios de hoy del catálogo (los precios
 * los calcula el celular del cliente) y el saldo a favor usado con el que tenía disponible.
 */
@Composable
private fun Verificaciones(pedido: Pedido, saldoDisponible: Double?) {
    val estado = LocalEstadoApp.current
    val tipo = pedido.cliente.tipoCliente ?: TipoCliente.DETAL
    val lineasHoy = pedido.lineas.mapNotNull { l -> estado.productoEnMemoria(l.producto.id)?.let { l.copy(producto = it) } }
    if (lineasHoy.size == pedido.lineas.size && lineasHoy.isNotEmpty()) {
        val hoy = Precios.desgloseCarrito(lineasHoy, tipo, pedido.zona, estado.ajustes).total
        if (hoy > 0 && abs(hoy - pedido.desglose.total) / hoy > 0.02) Aviso(
            "El total del pedido (${formatoDinero(pedido.desglose.total, pedido.moneda)}) no coincide con los precios de hoy " +
                "(${formatoDinero(hoy, pedido.moneda)}). Puede que hayan cambiado precios o la tasa; revísalo antes de aprobar.",
            TonoChip.AMARILLO, Icons.Outlined.Warning
        )
    }
    if (pedido.saldoAplicado > 0 && saldoDisponible != null && pedido.saldoAplicado > saldoDisponible + 0.01) Aviso(
        "El cliente usó ${formatoDinero(pedido.saldoAplicado, pedido.moneda)} de saldo a favor, pero solo tenía " +
            "${formatoDinero(saldoDisponible, pedido.moneda)}. Rechaza el pago y pídele que haga el pedido de nuevo.",
        TonoChip.ROJO, Icons.Outlined.Warning
    )
}

// ── Diálogo con el motivo del rechazo ──
@Composable
private fun DialogoRechazo(horas: Int, onRechazar: (String) -> Unit, onCerrar: () -> Unit) {
    var motivo by remember { mutableStateOf<String?>(null) }
    var otro by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val opciones = listOf("La foto no se lee", "El monto no coincide", "No llegó el dinero", "Referencia inválida", "Otro motivo")
    AlertDialog(
        onDismissRequest = onCerrar,
        containerColor = Color.White,
        title = { Text("Rechazar pago", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("El cliente verá el motivo y tendrá $horas horas para subir otro comprobante.")
                Selector("Motivo", opciones, motivo, { motivo = it; error = null }, error = error)
                if (motivo == "Otro motivo") CampoTexto("Escribe el motivo", otro, { otro = it.take(120); error = null })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val m = motivo
                when {
                    m == null -> error = "Elige un motivo"
                    m == "Otro motivo" && otro.trim().length < 4 -> error = "Escribe el motivo"
                    else -> onRechazar(if (m == "Otro motivo") otro.trim() else m)
                }
            }) { Text("Rechazar", color = Red, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar", color = Muted) } }
    )
}

@Preview(showBackground = true)
@Composable
fun RevisarPagoScreenPreview() {
    VistaPrevia(Rol.SUPERVISOR) { RevisarPagoScreen(id = "pg1", onClickBack = {}, onClickVerPedido = {}, onRevisado = {}) }
}
