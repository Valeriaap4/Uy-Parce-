package me.valeriaacosta.uyparceshopper.ui.screens.cliente

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.fechaCampo
import me.valeriaacosta.uyparceshopper.data.firebase.FotoNueva
import me.valeriaacosta.uyparceshopper.data.leerFechaCampo
import me.valeriaacosta.uyparceshopper.data.masHoras
import me.valeriaacosta.uyparceshopper.data.mensaje
import me.valeriaacosta.uyparceshopper.data.models.*
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*
import java.util.UUID

// ─── ReportarPagoScreen ──────────────────────────────────────────────────────

/**
 * Pantalla 12. Formulario validado: medio, referencia, fecha y foto del comprobante (cámara o
 * galería). Al enviar se guardan juntos la foto, el pago y el pedido en revisión.
 */
@Composable
fun ReportarPagoScreen(
    numero: Int,
    onClickBack: () -> Unit,
    onEnviado: () -> Unit
) {
    // La foto y la cámara van aquí arriba (antes de cargar el pedido) para no perder la foto si
    // Android cierra la app mientras la cámara está abierta.
    var foto by rememberSaveable { mutableStateOf<String?>(null) }
    var errorFoto by remember { mutableStateOf<String?>(null) }
    val selector = rememberSelectorFoto { ruta -> foto = ruta; errorFoto = null }

    SegunCarga(rememberPedido(numero), "Reportar pago", onClickBack) { p ->
        ReportarContenido(p, foto, { foto = it }, errorFoto, { errorFoto = it }, selector, onClickBack, onEnviado)
    }
}

@Composable
private fun ReportarContenido(
    pedido: Pedido,
    foto: String?,
    cambiarFoto: (String?) -> Unit,
    errorFoto: String?,
    cambiarErrorFoto: (String?) -> Unit,
    selector: SelectorFoto,
    onClickBack: () -> Unit,
    onEnviado: () -> Unit
) {
    val estado = LocalEstadoApp.current
    val mensaje = LocalMensaje.current
    val scope = rememberCoroutineScope()
    val total = pedido.totalAPagar
    val soloSaldo = total <= 0
    val medios = estado.ajustes.mediosPago.map { "${it.nombre} · ${it.moneda}" }
    // Ids creados una sola vez: si se reintenta el envío, no queda un pago repetido.
    val idPago = rememberSaveable { UUID.randomUUID().toString() }
    val idFoto = rememberSaveable { UUID.randomUUID().toString() }
    var medio by rememberSaveable { mutableStateOf(if (soloSaldo) "Saldo a favor" else null) }
    var referencia by rememberSaveable { mutableStateOf(if (soloSaldo) "SALDO" else "") }
    var fecha by rememberSaveable { mutableStateOf(fechaCampo()) }
    var errores by remember { mutableStateOf(mapOf<String, String>()) }
    var enviando by remember { mutableStateOf(false) }

    if (pedido.estado != EstadoPedido.PENDIENTE_PAGO) {
        UpScreen("Reportar pago", onClickBack, subtitulo = "Pedido #${pedido.numero}") {
            ContenidoScroll {
                Aviso("Este pedido ya no está pendiente de pago: ${pedido.estado.etiqueta.lowercase()}.", TonoChip.AZUL, Icons.Outlined.Info)
            }
        }
        return
    }

    UpScreen("Reportar pago", onClickBack, subtitulo = "Pedido #${pedido.numero}", barraInferior = {
        BotonPrincipal("Enviar comprobante", {
            val e = mutableMapOf<String, String>()
            val fechaLeida = leerFechaCampo(fecha)
            if (medio == null) e["medio"] = "Elige cómo pagaste"
            if (referencia.trim().length < 4) e["ref"] = "Escribe el número de referencia del comprobante"
            when {
                fechaLeida == null -> e["fecha"] = "Escribe una fecha real con el formato dd/mm/aaaa"
                fechaLeida > Timestamp.now().masHoras(12) -> e["fecha"] = "La fecha no puede ser futura"
                fechaLeida < Timestamp.now().masHoras(-24 * 30) -> e["fecha"] = "La fecha es de hace más de un mes"
            }
            val rutaFoto = foto
            if (!soloSaldo && rutaFoto == null) cambiarErrorFoto("Adjunta la foto del comprobante")
            errores = e
            val medioElegido = medio
            if (e.isEmpty() && medioElegido != null && (soloSaldo || rutaFoto != null)) {
                enviando = true
                scope.launch {
                    estado.reportarPago(
                        pedido.numero, idPago, medioElegido, referencia, fecha,
                        if (soloSaldo) null else rutaFoto?.let { FotoNueva(idFoto, it) }
                    ).onSuccess {
                        mensaje("Pago enviado a revisión")
                        onEnviado()
                    }.onFailure {
                        mensaje(it.mensaje())
                        enviando = false
                    }
                }
            }
        }, Modifier.fillMaxWidth(), Icons.AutoMirrored.Outlined.Send, cargando = enviando)
    }) {
        ContenidoScroll {
            Tarjeta {
                FilaDinero("Monto a reportar", total, pedido.moneda, total = true)
                Text("Es el total del pedido (menos tu saldo a favor) y no se puede cambiar.", color = Muted, fontSize = 13.sp)
            }
            if (soloSaldo) {
                Aviso("Tu saldo a favor cubre todo el pedido: no tienes que adjuntar comprobante.", TonoChip.VERDE)
            } else {
                Selector("Medio de pago", medios, medio, { errores = errores - "medio"; medio = it }, error = errores["medio"])
                CampoTexto("Referencia / número de transacción", referencia, { errores = errores - "ref"; referencia = it },
                    placeholder = "Ej: M1234567", error = errores["ref"])
                CampoTexto("Fecha del pago", fecha, { errores = errores - "fecha"; fecha = it }, placeholder = "dd/mm/aaaa", error = errores["fecha"])

                EtiquetaCampo("Foto del comprobante")
                val ruta = foto
                if (ruta != null) {
                    FotoLocal(ruta, Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp)))
                    TextButton(onClick = { cambiarFoto(null) }) { Text("Cambiar foto", color = Blue) }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        BotonSecundario("Cámara", selector.tomarFoto, Modifier.weight(1f), Icons.Outlined.PhotoCamera, cargando = selector.procesando)
                        BotonSecundario("Galería", selector.elegirDeGaleria, Modifier.weight(1f), Icons.Outlined.PhotoLibrary, habilitado = !selector.procesando)
                    }
                    errorFoto?.let { Text(it, color = Red) }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReportarPagoScreenPreview() {
    VistaPrevia { ReportarPagoScreen(numero = 1016, onClickBack = {}, onEnviado = {}) }
}
