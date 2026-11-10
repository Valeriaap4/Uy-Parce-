package me.valeriaacosta.uyparceshopper.ui.screens.supervisor

import me.valeriaacosta.uyparceshopper.data.*

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.valeriaacosta.uyparceshopper.data.DatosEjemplo
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.UyParceTheme

// ─── NuevoGastoScreen ────────────────────────────────────────────────────────

/** Pantalla 38. Formulario validado para registrar un gasto. */
@Composable
fun NuevoGastoScreen(
    onClickBack: () -> Unit,
    onGuardado: () -> Unit
) {
    val mensaje = LocalMensaje.current
    var categoria by remember { mutableStateOf<String?>(null) }
    var descripcion by remember { mutableStateOf("") }
    var monto by remember { mutableStateOf("") }
    var moneda by remember { mutableStateOf<String?>("COP") }
    var proveedor by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf("09/11/2026") }
    var errores by remember { mutableStateOf(mapOf<String, String>()) }

    UpScreen("Registrar gasto", onClickBack, barraInferior = {
        BotonPrincipal("Guardar gasto", {
            val e = mutableMapOf<String, String>()
            if (categoria == null) e["c"] = "Elige una categoría"
            if (descripcion.length < 3) e["d"] = "Describe el gasto"
            if ((monto.toDoubleOrNull() ?: 0.0) <= 0) e["m"] = "El monto debe ser mayor que 0"
            if (!Regex("""\d{2}/\d{2}/\d{4}""").matches(fecha)) e["f"] = "Usa el formato dd/mm/aaaa"
            errores = e
            if (e.isEmpty()) {
                mensaje("Gasto registrado")
                onGuardado()
            }
        }, Modifier.fillMaxWidth())
    }) {
        ContenidoScroll {
            Selector("Categoría", DatosEjemplo.categoriasGasto, categoria, { errores = errores - "c"; categoria = it }, error = errores["c"])
            CampoTexto("Descripción", descripcion, { errores = errores - "d"; descripcion = it }, error = errores["d"])
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CampoTexto("Monto", monto, { errores = errores - "m"; monto = it }, Modifier.weight(1.4f), teclado = KeyboardType.Decimal, error = errores["m"])
                Selector("Moneda", listOf("COP", "USD"), moneda, { moneda = it }, Modifier.weight(1f))
            }
            CampoTexto("Proveedor (opcional)", proveedor, { proveedor = it })
            CampoTexto("Fecha", fecha, { errores = errores - "f"; fecha = it }, error = errores["f"])
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NuevoGastoScreenPreview() {
    UyParceTheme { NuevoGastoScreen(onClickBack = {}, onGuardado = {}) }
}
