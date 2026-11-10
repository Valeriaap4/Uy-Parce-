package me.valeriaacosta.uyparceshopper.ui.screens.operador

import me.valeriaacosta.uyparceshopper.data.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.valeriaacosta.uyparceshopper.data.DatosEjemplo
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.models.Moneda
import me.valeriaacosta.uyparceshopper.data.models.TipoCaja
import me.valeriaacosta.uyparceshopper.data.models.TipoEnvio
import me.valeriaacosta.uyparceshopper.data.models.formatoDinero
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── EmpacarCajaScreen ───────────────────────────────────────────────────────

/** Pantalla 24. Escanear prendas a la caja; si es mayorista, pesar y elegir envío. Luego sellar. */
@Composable
fun EmpacarCajaScreen(
    codigo: String,
    onClickBack: () -> Unit,
    onSellada: () -> Unit
) {
    val caja = DatosEjemplo.cajas.firstOrNull { it.codigo == codigo } ?: return NoEncontrado("Caja $codigo", onClickBack)
    val ajustes = LocalEstadoApp.current.ajustes
    val mensaje = LocalMensaje.current
    val prendas = remember { mutableStateListOf<String>() }
    var escaner by remember { mutableStateOf(false) }
    var peso by remember { mutableStateOf("") }
    var tipoEnvio by remember { mutableStateOf<String?>(null) }
    var sellar by remember { mutableStateOf(false) }
    var errores by remember { mutableStateOf(mapOf<String, String>()) }

    UpScreen(
        titulo = "Empacar $codigo",
        onClickBack = onClickBack,
        subtitulo = if (caja.tipo == TipoCaja.MAYORISTA) "Mayorista · ${caja.cliente?.nombre}" else "Detal · caja compartida",
        barraInferior = {
            BotonSecundario("Escanear", { escaner = true }, Modifier.weight(1f), Icons.Outlined.QrCodeScanner)
            BotonPrincipal("Sellar y enviar", {
                val e = mutableMapOf<String, String>()
                if (prendas.isEmpty()) e["prendas"] = "Escanea al menos una prenda"
                if (caja.tipo == TipoCaja.MAYORISTA) {
                    if ((peso.toDoubleOrNull() ?: 0.0) <= 0) e["peso"] = "Pesa la caja"
                    if (tipoEnvio == null) e["envio"] = "Elige el tipo de envío"
                }
                errores = e
                if (e.isEmpty()) sellar = true
            }, Modifier.weight(1.3f), Icons.Outlined.Lock)
        }
    ) {
        ContenidoScroll {
            errores["prendas"]?.let { Aviso(it, TonoChip.ROJO) }
            TituloSeccion("Prendas en la caja (${prendas.size})")
            if (prendas.isEmpty()) Text("Todavía no has escaneado prendas.", color = Muted)
            prendas.forEachIndexed { i, p ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .border(1.dp, Line, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(p, modifier = Modifier.weight(1f))
                    IconButton(onClick = { prendas.removeAt(i) }) { Icon(Icons.Outlined.Close, "Quitar", tint = Muted) }
                }
            }

            // ── Solo cajas de mayorista: peso y envío ──
            if (caja.tipo == TipoCaja.MAYORISTA) {
                Tarjeta {
                    Text("Envío internacional", style = MaterialTheme.typography.titleMedium)
                    CampoTexto("Peso real (lb)", peso, { errores = errores - "peso"; peso = it }, teclado = KeyboardType.Decimal, error = errores["peso"])
                    Selector("Tipo de envío", TipoEnvio.entries.map { "${it.etiqueta} · $${ajustes.tarifaDe(it)}/lb" }, tipoEnvio,
                        { errores = errores - "envio"; tipoEnvio = it }, error = errores["envio"])
                    val t = TipoEnvio.entries.firstOrNull { tipoEnvio?.startsWith(it.etiqueta) == true }
                    val p = peso.toDoubleOrNull()
                    if (t != null && p != null) Text(
                        "$p lb × $${ajustes.tarifaDe(t)} + seguro $${ajustes.seguroUsd.toInt()} = " +
                            "${formatoDinero(p * ajustes.tarifaDe(t) + ajustes.seguroUsd, Moneda.USD)} USD",
                        color = Muted
                    )
                    Text("El cliente ya pagó un envío estimado; la diferencia la asume el negocio.", color = Muted, fontSize = 13.sp)
                }
            }
        }
    }

    if (escaner) DialogoEscaner(alLeer = {
        escaner = false
        val linea = (caja.pedidos.firstOrNull() ?: DatosEjemplo.pedido(1014)).lineas.let { it[prendas.size % it.size] }
        prendas += "${linea.producto.nombre} · ${linea.talla}"
        mensaje("Agregada: ${linea.producto.nombre}")
    }, alCerrar = { escaner = false })

    if (sellar) DialogoConfirmar(
        "Sellar caja", "¿Sellar y enviar $codigo con ${cantidad(prendas.size, "prenda")}? Ya no se le podrán agregar más.",
        "Sellar y enviar", { mensaje("Caja $codigo enviada a Cúcuta"); onSellada() }, { sellar = false }
    )
}

@Preview(showBackground = true)
@Composable
fun EmpacarCajaScreenPreview() {
    UyParceTheme { EmpacarCajaScreen(codigo = "CJ-0233", onClickBack = {}, onSellada = {}) }
}
