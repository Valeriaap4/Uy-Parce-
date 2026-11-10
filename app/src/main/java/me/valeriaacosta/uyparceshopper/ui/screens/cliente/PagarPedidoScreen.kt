package me.valeriaacosta.uyparceshopper.ui.screens.cliente

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.models.*
import me.valeriaacosta.uyparceshopper.data.tiempoRestante
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── PagarPedidoScreen ───────────────────────────────────────────────────────

/** Pantalla 11. Total a pagar, cuentas del negocio y cuenta regresiva del plazo para pagar. */
@Composable
fun PagarPedidoScreen(
    numero: Int,
    onClickBack: () -> Unit,
    onClickReportar: () -> Unit
) {
    SegunCarga(rememberPedido(numero), "Pagar pedido #$numero", onClickBack) { p -> PagarContenido(p, onClickBack, onClickReportar) }
}

@Composable
private fun PagarContenido(pedido: Pedido, onClickBack: () -> Unit, onClickReportar: () -> Unit) {
    val estado = LocalEstadoApp.current
    val mensaje = LocalMensaje.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    // La cuenta regresiva se actualiza cada 30 segundos.
    val ahora by produceState(Timestamp.now()) {
        while (true) {
            delay(30_000)
            value = Timestamp.now()
        }
    }
    val restante = tiempoRestante(pedido.venceEn, ahora)
    val pendiente = pedido.estado == EstadoPedido.PENDIENTE_PAGO
    val puedePagar = pendiente && restante != null
    val total = pedido.totalAPagar
    val medios = estado.ajustes.mediosPago

    UpScreen("Pagar pedido #${pedido.numero}", onClickBack, barraInferior = if (!puedePagar) null else ({
        BotonPrincipal(if (total > 0) "Ya pagué, reportar pago" else "Enviar a revisión", onClickReportar, Modifier.fillMaxWidth(), Icons.Outlined.Upload)
    })) {
        ContenidoScroll {
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Navy).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Total a pagar", color = NavyText)
                Dinero(total, pedido.moneda, 30.sp, Color.White)
                if (pedido.saldoAplicado > 0) Text(
                    "Ya se descontó tu saldo a favor de ${formatoDinero(pedido.saldoAplicado, pedido.moneda)}", color = NavyText, fontSize = 13.sp
                )
                if (pendiente) Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Timer, null, tint = Yellow, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (restante != null) "Te quedan $restante para pagar" else "El plazo para pagar venció",
                        color = Yellow, fontWeight = FontWeight.SemiBold
                    )
                }
            }
            when {
                !pendiente -> Aviso("Este pedido ya no está pendiente de pago: ${pedido.estado.etiqueta.lowercase()}.", TonoChip.AZUL, Icons.Outlined.Info)
                restante == null -> Aviso("Se pasó el plazo para pagar, así que el pedido se cancela. Puedes hacer uno nuevo desde el catálogo.", TonoChip.ROJO)
                total <= 0 -> Aviso("Tu saldo a favor cubre todo el pedido. Toca \"Enviar a revisión\" y lo aprobamos.", TonoChip.VERDE)
                medios.isEmpty() -> Aviso("La tienda todavía no tiene cuentas para recibir pagos. Escríbele a UyParce.", TonoChip.AMARILLO)
                else -> {
                    Text("Transfiere el valor exacto a cualquiera de estas cuentas:", color = Muted)
                    medios.forEach { m ->
                        Tarjeta {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${m.nombre} · ${m.moneda}", fontWeight = FontWeight.Bold)
                                    Text(m.numero, fontFamily = Mono, fontSize = 17.sp)
                                    Text("A nombre de ${m.titular}", color = Muted, fontSize = 13.sp)
                                    // El mayorista paga en USD; si la cuenta es en pesos, se muestra cuánto es con la tasa del pedido.
                                    if (m.moneda != pedido.moneda && pedido.moneda == Moneda.USD && pedido.desglose.tasaDolar > 0) {
                                        val pesos = Precios.redondear(total * pedido.desglose.tasaDolar, Moneda.COP)
                                        Text(
                                            "En pesos: ${formatoDinero(pesos, Moneda.COP)} COP (tasa ${formatoDinero(pedido.desglose.tasaDolar, Moneda.COP)})",
                                            color = Blue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                IconButton(onClick = {
                                    scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Cuenta", m.numero))) }
                                    mensaje("Número copiado")
                                }) {
                                    Icon(Icons.Outlined.ContentCopy, "Copiar", tint = Blue)
                                }
                            }
                        }
                    }
                    Aviso(
                        "No hay abonos: se paga el total. Si no reportas el pago a tiempo, el pedido se cancela.",
                        TonoChip.AMARILLO
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PagarPedidoScreenPreview() {
    VistaPrevia { PagarPedidoScreen(numero = 1016, onClickBack = {}, onClickReportar = {}) }
}
