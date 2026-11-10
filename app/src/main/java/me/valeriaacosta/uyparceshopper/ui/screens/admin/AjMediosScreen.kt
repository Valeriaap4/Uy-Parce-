package me.valeriaacosta.uyparceshopper.ui.screens.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.valeriaacosta.uyparceshopper.data.Listas
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.mensaje
import me.valeriaacosta.uyparceshopper.data.models.MedioPago
import me.valeriaacosta.uyparceshopper.data.models.Moneda
import me.valeriaacosta.uyparceshopper.data.models.Rol
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*
import java.util.UUID

// ─── AjMediosScreen ──────────────────────────────────────────────────────────

/**
 * Pantalla 51. Cuentas del negocio que ve el cliente al pagar, cada una con su moneda
 * (las cuentas en pesos le muestran al mayorista el equivalente con la tasa de su pedido).
 */
@Composable
fun AjMediosScreen(
    onClickBack: () -> Unit
) {
    val estado = LocalEstadoApp.current
    val mensaje = LocalMensaje.current
    val scope = rememberCoroutineScope()
    val medios = estado.ajustes.mediosPago
    var nombre by rememberSaveable { mutableStateOf("") }
    var numero by rememberSaveable { mutableStateOf("") }
    var titular by rememberSaveable { mutableStateOf("") }
    var moneda by rememberSaveable { mutableStateOf(Moneda.COP) }
    var errores by remember { mutableStateOf(mapOf<String, String>()) }
    var borrar by remember { mutableStateOf<MedioPago?>(null) }
    var guardando by remember { mutableStateOf(false) }

    fun guardar(nuevos: List<MedioPago>, accion: String, detalle: String, alTerminar: () -> Unit) {
        guardando = true
        scope.launch {
            estado.actualizarAjustes(mapOf("mediosPago" to nuevos), accion, detalle)
                .onSuccess { alTerminar() }
                .onFailure { mensaje(it.mensaje()) }
            guardando = false
        }
    }

    UpScreen("Medios de pago", onClickBack) {
        ContenidoScroll {
            if (estado.cargado(Listas.AJUSTES) && !estado.hayAjustes) {
                Aviso("Primero prepara la base de datos desde Ajustes.", TonoChip.AMARILLO, Icons.Outlined.Info)
                return@ContenidoScroll
            }
            Text("El cliente ve estas cuentas al pagar.", color = Muted)
            if (medios.isEmpty()) Aviso("Todavía no hay cuentas: los clientes no sabrán dónde pagar.", TonoChip.AMARILLO)
            medios.forEach { m ->
                Tarjeta {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${m.nombre} · ${m.moneda}", fontWeight = FontWeight.Bold)
                            Text("${m.numero} · ${m.titular}", color = Muted, fontFamily = Mono, fontSize = 14.sp)
                        }
                        IconButton(onClick = { borrar = m }, enabled = !guardando) { Icon(Icons.Outlined.Delete, "Quitar", tint = Red) }
                    }
                }
            }
            TituloSeccion("Agregar cuenta")
            CampoTexto("Banco o billetera", nombre, { errores = errores - "n"; nombre = it }, placeholder = "Ej: Daviplata", error = errores["n"])
            CampoTexto("Número", numero, { errores = errores - "num"; numero = it.filter { c -> c.isDigit() || c == ' ' || c == '-' } },
                teclado = KeyboardType.Number, error = errores["num"])
            CampoTexto("Titular", titular, { errores = errores - "t"; titular = it }, error = errores["t"])
            Selector("Moneda que recibe", Moneda.entries.map { it.name }, moneda.name, { moneda = Moneda.valueOf(it) })
            BotonPrincipal("Agregar", {
                val e = mutableMapOf<String, String>()
                if (nombre.isBlank()) e["n"] = "Escribe el banco o la billetera"
                if (numero.filter(Char::isDigit).length < 6) e["num"] = "Número no válido"
                if (titular.isBlank()) e["t"] = "Escribe el titular"
                errores = e
                if (e.isEmpty()) {
                    val nuevo = MedioPago(UUID.randomUUID().toString(), nombre.trim(), titular.trim(), numero.trim(), moneda)
                    guardar(medios + nuevo, "Agregó medio de pago", "${nuevo.nombre} · ${nuevo.moneda}") {
                        nombre = ""; numero = ""; titular = ""
                        mensaje("Cuenta agregada")
                    }
                }
            }, Modifier.fillMaxWidth(), Icons.Outlined.Add, cargando = guardando)
        }
    }

    borrar?.let { m ->
        DialogoConfirmar(
            "Quitar cuenta", "Los clientes ya no verán ${m.nombre} (${m.numero}) al pagar.", "Quitar",
            { guardar(medios.filter { it.id != m.id }, "Quitó medio de pago", "${m.nombre} · ${m.moneda}") { mensaje("Cuenta quitada") } },
            { borrar = null }, peligro = true
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AjMediosScreenPreview() {
    VistaPrevia(Rol.ADMIN) { AjMediosScreen(onClickBack = {}) }
}
