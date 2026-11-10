package me.valeriaacosta.uyparceshopper.ui.screens.operador

import me.valeriaacosta.uyparceshopper.data.*

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import me.valeriaacosta.uyparceshopper.data.DatosEjemplo
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.Blue
import me.valeriaacosta.uyparceshopper.ui.theme.Muted
import me.valeriaacosta.uyparceshopper.ui.theme.UyParceTheme

// ─── CrearCajaScreen ─────────────────────────────────────────────────────────

/** Pantalla 23. Crear una caja: mayorista (una por cliente) o detal (compartida). */
@Composable
fun CrearCajaScreen(
    onClickBack: () -> Unit,
    onCreada: (codigo: String) -> Unit
) {
    var tipo by remember { mutableStateOf<String?>(null) }
    var cliente by remember { mutableStateOf<String?>(null) }
    var codigo by remember { mutableStateOf("CJ-0234") }
    var errores by remember { mutableStateOf(mapOf<String, String>()) }
    val mayoristas = DatosEjemplo.clientes.filter { it.tipoCliente?.esMayorista == true }

    UpScreen("Crear caja", onClickBack, barraInferior = {
        BotonPrincipal("Crear y empacar", {
            val e = mutableMapOf<String, String>()
            if (tipo == null) e["tipo"] = "Elige el tipo de caja"
            if (tipo == "Mayorista" && cliente == null) e["cliente"] = "Elige el cliente mayorista"
            if (!Regex("""CJ-\d{4}""").matches(codigo)) e["codigo"] = "Formato CJ-0000"
            errores = e
            // Demo: se abre una caja de ejemplo del mismo tipo.
            if (e.isEmpty()) onCreada(if (tipo == "Mayorista") "CJ-0233" else "CJ-0232")
        }, Modifier.fillMaxWidth())
    }) {
        ContenidoScroll {
            Selector("Tipo de caja", listOf("Mayorista", "Detal"), tipo, { errores = errores - "tipo"; tipo = it }, error = errores["tipo"])
            if (tipo == "Mayorista") {
                Selector("Cliente mayorista", mayoristas.map { it.nombre }, cliente, { errores = errores - "cliente"; cliente = it }, error = errores["cliente"])
                Text("Una caja por cliente. Al sellarla se pesa y se registra el envío.", color = Muted)
            }
            if (tipo == "Detal") Text("Caja compartida: lleva prendas de varios pedidos detal. En Cúcuta se separan por pedido.", color = Muted)
            CampoTexto("Código de la caja", codigo, { errores = errores - "codigo"; codigo = it.uppercase() }, error = errores["codigo"],
                iconoFin = { Icon(Icons.Outlined.QrCodeScanner, null, tint = Blue) })
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CrearCajaScreenPreview() {
    UyParceTheme { CrearCajaScreen(onClickBack = {}, onCreada = {}) }
}
