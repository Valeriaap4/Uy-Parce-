package me.valeriaacosta.uyparceshopper.ui.screens.operador

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.buscarPorCodigo
import me.valeriaacosta.uyparceshopper.data.mensaje
import me.valeriaacosta.uyparceshopper.data.models.*
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── EmpacarCajaScreen ───────────────────────────────────────────────────────

/**
 * Pantalla 24. Se escanea cada prenda que entra a la caja y la app la asigna al pedido que
 * corresponde (mayorista: los del cliente; detal: los pedidos de detal, del más antiguo al más
 * nuevo). Si es mayorista se pesa y se elige el envío. Luego se sella y sale para Cúcuta.
 */
@Composable
fun EmpacarCajaScreen(
    codigo: String,
    onClickBack: () -> Unit,
    onSellada: () -> Unit
) {
    SegunCarga(rememberCaja(codigo), "Caja $codigo", onClickBack) { caja -> EmpacarContenido(caja, onClickBack, onSellada) }
}

/** Una prenda comprada que todavía no va en ninguna caja. */
private data class PorEmpacar(val numeroPedido: Int, val linea: LineaPedido)

@Composable
private fun EmpacarContenido(caja: Caja, onClickBack: () -> Unit, onSellada: () -> Unit) {
    val estado = LocalEstadoApp.current
    val mensaje = LocalMensaje.current
    val scope = rememberCoroutineScope()
    val ajustes = estado.ajustes
    var escaner by remember { mutableStateOf(false) }
    var peso by rememberSaveable { mutableStateOf("") }
    var tipoEnvio by rememberSaveable { mutableStateOf<TipoEnvio?>(null) }
    var confirmar by remember { mutableStateOf<String?>(null) } // "sellar" o "eliminar"
    var errores by remember { mutableStateOf(mapOf<String, String>()) }
    var trabajando by remember { mutableStateOf(false) }
    val abierta = caja.estado == EstadoCaja.ABIERTA

    // Lo que falta empacar de los pedidos que pueden ir en esta caja.
    val pendientes = estado.pedidos
        .filter { p ->
            (p.estado == EstadoPedido.PAGADO || p.estado == EstadoPedido.COMPRADO) &&
                (if (caja.tipo == TipoCaja.MAYORISTA) p.clienteId == caja.clienteId else p.moneda == Moneda.COP)
        }
        .sortedBy { it.numero }
        .flatMap { p -> p.lineas.filter { it.porEmpacar > 0 }.map { PorEmpacar(p.numero, it) } }

    fun agregar(productoId: String, talla: String, nombre: String) {
        trabajando = true
        scope.launch {
            estado.agregarPrenda(caja, productoId, talla)
                .onSuccess { mensaje("Agregada: $nombre $talla → pedido #${it.numeroPedido}") }
                .onFailure { mensaje(it.mensaje()) }
            trabajando = false
        }
    }

    UpScreen(
        titulo = "Empacar ${caja.codigo}",
        onClickBack = onClickBack,
        subtitulo = if (caja.tipo == TipoCaja.MAYORISTA) "Mayorista · ${caja.clienteNombre ?: ""}" else "Detal · caja compartida",
        barraInferior = if (!abierta) null else ({
            BotonSecundario("Escanear", { escaner = true }, Modifier.weight(1f), Icons.Outlined.QrCodeScanner, habilitado = !trabajando)
            BotonPrincipal("Sellar y enviar", {
                val e = mutableMapOf<String, String>()
                if (caja.contenido.isEmpty()) e["prendas"] = "Escanea al menos una prenda"
                if (caja.tipo == TipoCaja.MAYORISTA) {
                    val p = peso.replace(",", ".").toDoubleOrNull() ?: 0.0
                    if (p <= 0 || p > 500) e["peso"] = "Pesa la caja (en libras)"
                    if (tipoEnvio == null) e["envio"] = "Elige el tipo de envío"
                }
                errores = e
                if (e.isEmpty()) confirmar = "sellar"
            }, Modifier.weight(1.3f), Icons.Outlined.Lock, cargando = trabajando)
        })
    ) {
        ContenidoScroll {
            if (!abierta) Aviso("Esta caja ya se selló (${caja.estado.etiqueta.lowercase()}).", TonoChip.AZUL, Icons.Outlined.Info)
            errores["prendas"]?.let { Aviso(it, TonoChip.ROJO) }

            TituloSeccion("Prendas en la caja (${caja.prendas})")
            if (caja.contenido.isEmpty()) Text("Todavía no has escaneado prendas.", color = Muted)
            caja.contenido.forEach { p ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .border(1.dp, Line, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("${p.nombre} · ${p.talla}")
                        Text("Pedido #${p.numeroPedido}", color = Muted, fontSize = 13.sp)
                    }
                    if (abierta) IconButton(onClick = {
                        trabajando = true
                        scope.launch {
                            estado.quitarPrenda(caja, p.id).onFailure { mensaje(it.mensaje()) }
                            trabajando = false
                        }
                    }, enabled = !trabajando) { Icon(Icons.Outlined.Close, "Quitar", tint = Muted) }
                }
            }

            if (abierta) {
                TituloSeccion("Por empacar (${pendientes.sumOf { it.linea.porEmpacar }})")
                if (pendientes.isEmpty()) Text("No hay prendas compradas esperando caja.", color = Muted)
                else Text("Escanea cada prenda o tócala aquí si el código no se deja leer.", color = Muted, fontSize = 13.sp)
                pendientes.forEach { pe ->
                    Tarjeta(alPulsar = { if (!trabajando) agregar(pe.linea.producto.id, pe.linea.talla, pe.linea.producto.nombre) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("${pe.linea.producto.nombre} · ${pe.linea.talla}", fontWeight = FontWeight.SemiBold)
                                Text("Pedido #${pe.numeroPedido}", color = Muted, fontSize = 13.sp)
                            }
                            Text("× ${pe.linea.porEmpacar}", fontFamily = Mono, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ── Solo cajas de mayorista: peso y envío ──
            if (abierta && caja.tipo == TipoCaja.MAYORISTA) {
                Tarjeta {
                    Text("Envío internacional", style = MaterialTheme.typography.titleMedium)
                    CampoTexto("Peso real (lb)", peso, { errores = errores - "peso"; peso = it }, teclado = KeyboardType.Decimal, error = errores["peso"])
                    Selector("Tipo de envío", TipoEnvio.entries.map { "${it.etiqueta} · $${ajustes.tarifaDe(it)}/lb" },
                        tipoEnvio?.let { "${it.etiqueta} · $${ajustes.tarifaDe(it)}/lb" },
                        { elegido -> errores = errores - "envio"; tipoEnvio = TipoEnvio.entries.firstOrNull { elegido.startsWith(it.etiqueta) } },
                        error = errores["envio"])
                    val t = tipoEnvio
                    val p = peso.replace(",", ".").toDoubleOrNull()
                    if (t != null && p != null) Text(
                        "$p lb × $${ajustes.tarifaDe(t)} + seguro $${formatoDinero(ajustes.seguroUsd, Moneda.USD).removePrefix("$")} = " +
                            "${formatoDinero(Precios.redondear(p * ajustes.tarifaDe(t) + ajustes.seguroUsd, Moneda.USD), Moneda.USD)} USD",
                        color = Muted
                    )
                    Text("El cliente ya pagó un envío estimado; la diferencia la asume el negocio.", color = Muted, fontSize = 13.sp)
                }
            }
            if (abierta && caja.contenido.isEmpty())
                BotonSecundario("Eliminar caja", { confirmar = "eliminar" }, Modifier.fillMaxWidth(), Icons.Outlined.Delete, Red, habilitado = !trabajando)
        }
    }

    if (escaner) DialogoEscaner(alLeer = { codigo ->
        escaner = false
        val encontrado = buscarPorCodigo(estado.productos, codigo)
        if (encontrado == null) mensaje("Ese código no está guardado en ningún producto. Toca la prenda en \"Por empacar\".")
        else agregar(encontrado.producto.id, encontrado.talla, encontrado.producto.nombre)
    }, alCerrar = { escaner = false })

    when (confirmar) {
        "sellar" -> DialogoConfirmar(
            "Sellar caja", "¿Sellar y enviar ${caja.codigo} con ${cantidad(caja.prendas, "prenda")}? Ya no se le podrán agregar más.",
            "Sellar y enviar", {
                trabajando = true
                scope.launch {
                    val pesoLb = peso.replace(",", ".").toDoubleOrNull()?.takeIf { caja.tipo == TipoCaja.MAYORISTA }
                    estado.sellarCaja(caja, pesoLb, tipoEnvio.takeIf { caja.tipo == TipoCaja.MAYORISTA })
                        .onSuccess { mensaje("Caja ${caja.codigo} enviada a Cúcuta"); onSellada() }
                        .onFailure { mensaje(it.mensaje()) }
                    trabajando = false
                }
            }, { confirmar = null }
        )
        "eliminar" -> DialogoConfirmar(
            "Eliminar caja", "La caja ${caja.codigo} está vacía y se va a la papelera.", "Eliminar", {
                trabajando = true
                scope.launch {
                    estado.eliminarCaja(caja)
                        .onSuccess { mensaje("Caja eliminada"); onSellada() }
                        .onFailure { mensaje(it.mensaje()) }
                    trabajando = false
                }
            }, { confirmar = null }, peligro = true
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EmpacarCajaScreenPreview() {
    VistaPrevia(Rol.OPERADOR) { EmpacarCajaScreen(codigo = "CJ-0233", onClickBack = {}, onSellada = {}) }
}
