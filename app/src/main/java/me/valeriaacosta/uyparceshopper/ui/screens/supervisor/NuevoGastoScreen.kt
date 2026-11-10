package me.valeriaacosta.uyparceshopper.ui.screens.supervisor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.fechaCampo
import me.valeriaacosta.uyparceshopper.data.leerFechaCampo
import me.valeriaacosta.uyparceshopper.data.masHoras
import me.valeriaacosta.uyparceshopper.data.mensaje
import me.valeriaacosta.uyparceshopper.data.models.Gasto
import me.valeriaacosta.uyparceshopper.data.models.Moneda
import me.valeriaacosta.uyparceshopper.data.models.Precios
import me.valeriaacosta.uyparceshopper.data.models.Rol
import me.valeriaacosta.uyparceshopper.ui.components.*
import java.util.UUID

// ─── NuevoGastoScreen ────────────────────────────────────────────────────────

/** Pantalla 38. Formulario validado para registrar un gasto del negocio. */
@Composable
fun NuevoGastoScreen(
    onClickBack: () -> Unit,
    onGuardado: () -> Unit
) {
    val estado = LocalEstadoApp.current
    val mensaje = LocalMensaje.current
    val scope = rememberCoroutineScope()
    // Id creado una vez: si se reintenta, el gasto no queda repetido.
    val idGasto = rememberSaveable { UUID.randomUUID().toString() }
    var categoria by rememberSaveable { mutableStateOf<String?>(null) }
    var descripcion by rememberSaveable { mutableStateOf("") }
    var monto by rememberSaveable { mutableStateOf("") }
    var moneda by rememberSaveable { mutableStateOf(Moneda.COP) }
    var proveedor by rememberSaveable { mutableStateOf("") }
    var fecha by rememberSaveable { mutableStateOf(fechaCampo()) }
    var errores by remember { mutableStateOf(mapOf<String, String>()) }
    var guardando by remember { mutableStateOf(false) }

    UpScreen("Registrar gasto", onClickBack, barraInferior = {
        BotonPrincipal("Guardar gasto", {
            val e = mutableMapOf<String, String>()
            val valor = monto.replace(",", ".").toDoubleOrNull() ?: 0.0
            val dia = leerFechaCampo(fecha)
            if (categoria == null) e["c"] = "Elige una categoría"
            if (descripcion.trim().length < 3) e["d"] = "Describe el gasto"
            if (valor <= 0) e["m"] = "El monto debe ser mayor que 0"
            when {
                dia == null -> e["f"] = "Escribe una fecha real con el formato dd/mm/aaaa"
                dia > Timestamp.now().masHoras(12) -> e["f"] = "La fecha no puede ser futura"
            }
            errores = e
            val cat = categoria
            if (e.isEmpty() && cat != null && dia != null) {
                guardando = true
                scope.launch {
                    estado.crearGasto(
                        Gasto(idGasto, cat, descripcion.trim(), Precios.redondear(valor, moneda), moneda, proveedor.trim(), dia)
                    ).onSuccess { mensaje("Gasto registrado"); onGuardado() }
                        .onFailure { mensaje(it.mensaje()); guardando = false }
                }
            }
        }, Modifier.fillMaxWidth(), cargando = guardando)
    }) {
        ContenidoScroll {
            Selector("Categoría", Gasto.categorias, categoria, { errores = errores - "c"; categoria = it }, error = errores["c"])
            CampoTexto("Descripción", descripcion, { errores = errores - "d"; descripcion = it }, error = errores["d"])
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CampoTexto("Monto", monto, { errores = errores - "m"; monto = it }, Modifier.weight(1.4f), teclado = KeyboardType.Decimal, error = errores["m"])
                Selector("Moneda", Moneda.entries.map { it.name }, moneda.name, { moneda = Moneda.valueOf(it) }, Modifier.weight(1f))
            }
            CampoTexto("Proveedor (opcional)", proveedor, { proveedor = it })
            CampoTexto("Fecha", fecha, { errores = errores - "f"; fecha = it }, placeholder = "dd/mm/aaaa", error = errores["f"])
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NuevoGastoScreenPreview() {
    VistaPrevia(Rol.SUPERVISOR) { NuevoGastoScreen(onClickBack = {}, onGuardado = {}) }
}
