package me.valeriaacosta.uyparceshopper.ui.screens.supervisor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.*
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
import me.valeriaacosta.uyparceshopper.data.models.EstadoPago
import me.valeriaacosta.uyparceshopper.data.models.Precios
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── RevisarPagoScreen ───────────────────────────────────────────────────────

/** Pantalla 26. El supervisor revisa el comprobante y aprueba (genera factura) o rechaza con motivo. */
@Composable
fun RevisarPagoScreen(
    id: String,
    onClickBack: () -> Unit,
    onClickVerPedido: (numero: Int) -> Unit,
    onRevisado: () -> Unit
) {
    val pago = DatosEjemplo.pagos.firstOrNull { it.id == id } ?: return NoEncontrado("Revisar pago", onClickBack)
    val d = Precios.desglose(pago.pedido, LocalEstadoApp.current.ajustes)
    val mensaje = LocalMensaje.current
    var aprobar by remember { mutableStateOf(false) }
    var rechazar by remember { mutableStateOf(false) }

    UpScreen(
        titulo = "Revisar pago",
        onClickBack = onClickBack,
        subtitulo = "Pedido #${pago.pedido.numero}",
        barraInferior = if (pago.estado != EstadoPago.EN_REVISION) null else ({
            BotonSecundario("Rechazar", { rechazar = true }, Modifier.weight(1f), color = Red)
            BotonPrincipal("Aprobar", { aprobar = true }, Modifier.weight(1.4f), Icons.Outlined.Check)
        })
    ) {
        ContenidoScroll {
            // Foto del comprobante (siguiente fase: viene de Firebase Storage).
            Box(
                modifier = Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(20.dp)).background(GreySoft),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Receipt, null, tint = Muted, modifier = Modifier.size(56.dp))
                    Text("Comprobante ${pago.medio}", color = Muted)
                    Text("Toca para ampliar", color = Blue, fontSize = 13.sp)
                }
            }
            Tarjeta {
                FilaDato("Cliente", pago.pedido.cliente.nombre, true)
                FilaDato("Medio", pago.medio)
                FilaDato("Referencia", pago.referencia)
                FilaDato("Fecha reportada", pago.fecha)
                HorizontalDivider(color = Line)
                FilaDinero("Total esperado", d.total, d.moneda, total = true)
            }
            Aviso("Revisa en la cuenta del negocio que el dinero sí llegó antes de aprobar.", TonoChip.AZUL)
            BotonSecundario("Ver pedido", { onClickVerPedido(pago.pedido.numero) }, Modifier.fillMaxWidth())
        }
    }

    if (aprobar) DialogoConfirmar(
        "Aprobar pago", "Se genera la factura y el pedido pasa a Pagado. El operador ya lo podrá comprar.",
        "Aprobar", { mensaje("Pago aprobado · factura generada"); onRevisado() }, { aprobar = false }
    )
    if (rechazar) DialogoRechazo(
        onRechazar = { motivo -> rechazar = false; mensaje("Pago rechazado: $motivo"); onRevisado() },
        onCerrar = { rechazar = false }
    )
}

// ── Diálogo con el motivo del rechazo ──
@Composable
private fun DialogoRechazo(onRechazar: (String) -> Unit, onCerrar: () -> Unit) {
    var motivo by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onCerrar,
        containerColor = Color.White,
        title = { Text("Rechazar pago", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("El cliente verá el motivo y tendrá 24 horas para subir otro comprobante.")
                Selector("Motivo", listOf("La foto no se lee", "El monto no coincide", "No llegó el dinero", "Referencia inválida"),
                    motivo, { motivo = it; error = null }, error = error)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val m = motivo
                if (m == null) error = "Elige un motivo" else onRechazar(m)
            }) { Text("Rechazar", color = Red, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar", color = Muted) } }
    )
}

@Preview(showBackground = true)
@Composable
fun RevisarPagoScreenPreview() {
    UyParceTheme { RevisarPagoScreen(id = "pg1", onClickBack = {}, onClickVerPedido = {}, onRevisado = {}) }
}
