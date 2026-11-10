package me.valeriaacosta.uyparceshopper.ui.screens.cliente

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.valeriaacosta.uyparceshopper.data.DatosEjemplo
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.models.Precios
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── ReportarPagoScreen ──────────────────────────────────────────────────────

/** Pantalla 12. Formulario validado: medio, referencia, fecha y foto del comprobante. */
@Composable
fun ReportarPagoScreen(
    numero: Int,
    onClickBack: () -> Unit,
    onEnviado: () -> Unit
) {
    val estado = LocalEstadoApp.current
    val pedido = DatosEjemplo.buscarPedido(numero) ?: return NoEncontrado("Pedido #$numero", onClickBack)
    val d = Precios.desglose(pedido, estado.ajustes)
    val mensaje = LocalMensaje.current
    var medio by remember { mutableStateOf<String?>(null) }
    var referencia by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf("09/11/2026") }
    var foto by remember { mutableStateOf<String?>(null) }
    var errores by remember { mutableStateOf(mapOf<String, String>()) }

    UpScreen("Reportar pago", onClickBack, subtitulo = "Pedido #$numero", barraInferior = {
        BotonPrincipal("Enviar comprobante", {
            val e = mutableMapOf<String, String>()
            if (medio == null) e["medio"] = "Elige cómo pagaste"
            if (referencia.length < 4) e["ref"] = "Escribe el número de referencia del comprobante"
            if (!Regex("""\d{2}/\d{2}/\d{4}""").matches(fecha)) e["fecha"] = "Usa el formato dd/mm/aaaa"
            if (foto == null) e["foto"] = "Adjunta la foto del comprobante"
            errores = e
            if (e.isEmpty()) {
                mensaje("Pago enviado a revisión")
                onEnviado()
            }
        }, Modifier.fillMaxWidth(), Icons.AutoMirrored.Outlined.Send)
    }) {
        ContenidoScroll {
            Tarjeta {
                FilaDinero("Monto a reportar", d.total, d.moneda, total = true)
                Text("El monto es el total del pedido y no se puede cambiar.", color = Muted, fontSize = 13.sp)
            }
            Selector("Medio de pago", DatosEjemplo.mediosPago.map { it.nombre }, medio, { errores = errores - "medio"; medio = it }, error = errores["medio"])
            CampoTexto("Referencia / número de transacción", referencia, { errores = errores - "ref"; referencia = it.trim() },
                placeholder = "Ej: M1234567", error = errores["ref"])
            CampoTexto("Fecha del pago", fecha, { errores = errores - "fecha"; fecha = it }, placeholder = "dd/mm/aaaa", error = errores["fecha"])

            EtiquetaCampo("Foto del comprobante")
            val archivo = foto
            if (archivo != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(GreenSoft).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Outlined.Image, null, tint = Green)
                    Text(archivo, color = Green, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    TextButton(onClick = { foto = null }) { Text("Cambiar", color = Blue) }
                }
            } else {
                // Siguiente fase: cámara (TakePicture) y galería (PickVisualMedia).
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BotonSecundario("Cámara", { foto = "comprobante_camara.jpg"; errores = errores - "foto" }, Modifier.weight(1f), Icons.Outlined.PhotoCamera)
                    BotonSecundario("Galería", { foto = "comprobante_galeria.jpg"; errores = errores - "foto" }, Modifier.weight(1f), Icons.Outlined.PhotoLibrary)
                }
                errores["foto"]?.let { Text(it, color = Red) }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReportarPagoScreenPreview() {
    UyParceTheme { ReportarPagoScreen(numero = 1016, onClickBack = {}, onEnviado = {}) }
}
