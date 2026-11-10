package me.valeriaacosta.uyparceshopper.ui.screens.supervisor

import me.valeriaacosta.uyparceshopper.data.*

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import me.valeriaacosta.uyparceshopper.data.DatosEjemplo
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.Blue
import me.valeriaacosta.uyparceshopper.ui.theme.UyParceTheme

// ─── DespacharScreen ─────────────────────────────────────────────────────────

/** Pantalla 31. Registrar transportadora y número de guía de un pedido. */
@Composable
fun DespacharScreen(
    numero: Int,
    onClickBack: () -> Unit,
    onDespachado: () -> Unit
) {
    val p = DatosEjemplo.buscarPedido(numero) ?: return NoEncontrado("Pedido #$numero", onClickBack)
    val mensaje = LocalMensaje.current
    var transportadora by remember { mutableStateOf<String?>(null) }
    var guia by remember { mutableStateOf("") }
    var errores by remember { mutableStateOf(mapOf<String, String>()) }

    UpScreen("Despachar #$numero", onClickBack, barraInferior = {
        BotonPrincipal("Registrar envío", {
            val e = mutableMapOf<String, String>()
            if (transportadora == null) e["t"] = "Elige la transportadora"
            if (guia.filter(Char::isDigit).length < 8) e["g"] = "La guía tiene al menos 8 dígitos"
            errores = e
            if (e.isEmpty()) {
                mensaje("Enviado · avisamos a ${p.cliente.nombre.substringBefore(" ")}")
                onDespachado()
            }
        }, Modifier.fillMaxWidth(), Icons.Outlined.LocalShipping)
    }) {
        ContenidoScroll {
            Tarjeta {
                FilaDato("Cliente", p.cliente.nombre, true)
                FilaDato("Teléfono", p.cliente.telefono)
                FilaDato("Ciudad", p.cliente.ciudad)
                FilaDato("Dirección", p.cliente.direccion)
                FilaDato("Prendas", "${p.prendas}")
            }
            Selector("Transportadora", DatosEjemplo.transportadoras, transportadora, { errores = errores - "t"; transportadora = it }, error = errores["t"])
            CampoTexto("Número de guía", guia, { errores = errores - "g"; guia = it.filter { c -> c.isDigit() || c == ' ' } },
                teclado = KeyboardType.Number, error = errores["g"],
                iconoFin = { Icon(Icons.Outlined.QrCodeScanner, null, tint = Blue) })
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DespacharScreenPreview() {
    UyParceTheme { DespacharScreen(numero = 1012, onClickBack = {}, onDespachado = {}) }
}
