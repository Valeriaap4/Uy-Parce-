package me.valeriaacosta.uyparceshopper.ui.screens.supervisor

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.mensaje
import me.valeriaacosta.uyparceshopper.data.models.*
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.Blue

// ─── DespacharScreen ─────────────────────────────────────────────────────────

/**
 * Pantalla 31. Registrar transportadora y número de guía (se puede escanear de la etiqueta).
 * El pedido pasa a "Enviado" y se le avisa al cliente.
 */
@Composable
fun DespacharScreen(
    numero: Int,
    onClickBack: () -> Unit,
    onDespachado: () -> Unit
) {
    SegunCarga(rememberPedido(numero), "Despachar #$numero", onClickBack) { p -> DespacharContenido(p, onClickBack, onDespachado) }
}

@Composable
private fun DespacharContenido(p: Pedido, onClickBack: () -> Unit, onDespachado: () -> Unit) {
    val estado = LocalEstadoApp.current
    val mensaje = LocalMensaje.current
    val scope = rememberCoroutineScope()
    var transportadora by rememberSaveable { mutableStateOf<String?>(null) }
    var guia by rememberSaveable { mutableStateOf("") }
    var errores by remember { mutableStateOf(mapOf<String, String>()) }
    var escanear by remember { mutableStateOf(false) }
    var enviando by remember { mutableStateOf(false) }
    val listo = p.estado == EstadoPedido.EN_CUCUTA

    UpScreen("Despachar #${p.numero}", onClickBack, barraInferior = if (!listo) null else ({
        BotonPrincipal("Registrar envío", {
            val e = mutableMapOf<String, String>()
            if (transportadora == null) e["t"] = "Elige la transportadora"
            if (guia.filter(Char::isLetterOrDigit).length < 8) e["g"] = "La guía tiene al menos 8 dígitos"
            errores = e
            val t = transportadora
            if (e.isEmpty() && t != null) {
                enviando = true
                scope.launch {
                    estado.despachar(p.numero, t, guia)
                        .onSuccess { mensaje("Enviado · avisamos a ${p.cliente.nombre.substringBefore(" ")}"); onDespachado() }
                        .onFailure { mensaje(it.mensaje()); enviando = false }
                }
            }
        }, Modifier.fillMaxWidth(), Icons.Outlined.LocalShipping, cargando = enviando)
    })) {
        ContenidoScroll {
            if (!listo) Aviso("Este pedido no está listo para despachar (${p.estado.etiqueta.lowercase()}).", TonoChip.AZUL, Icons.Outlined.Info)
            if (p.faltantesSinResolver.isNotEmpty()) Aviso(
                "El cliente todavía no ha elegido qué hacer con un faltante. Se puede despachar igual.", TonoChip.AMARILLO
            )
            Tarjeta {
                FilaDato("Cliente", p.cliente.nombre, true)
                FilaDato("Teléfono", p.cliente.telefono)
                FilaDato("Ciudad", p.cliente.ciudad)
                FilaDato("Dirección", p.cliente.direccion)
                FilaDato("Prendas", "${p.recibidas}")
            }
            if (listo) {
                Selector("Transportadora", Pedido.transportadoras, transportadora, { errores = errores - "t"; transportadora = it }, error = errores["t"])
                CampoTexto(
                    "Número de guía", guia, { errores = errores - "g"; guia = it.filter { c -> c.isLetterOrDigit() || c == ' ' }.take(30) },
                    teclado = KeyboardType.Number, error = errores["g"],
                    iconoFin = { IconButton(onClick = { escanear = true }) { Icon(Icons.Outlined.QrCodeScanner, "Escanear guía", tint = Blue) } }
                )
            }
        }
    }

    if (escanear) DialogoEscaner(
        titulo = "Guía de la transportadora",
        alLeer = { c -> escanear = false; guia = c; errores = errores - "g" },
        alCerrar = { escanear = false }
    )
}

@Preview(showBackground = true)
@Composable
fun DespacharScreenPreview() {
    VistaPrevia(Rol.SUPERVISOR) { DespacharScreen(numero = 1012, onClickBack = {}, onDespachado = {}) }
}
