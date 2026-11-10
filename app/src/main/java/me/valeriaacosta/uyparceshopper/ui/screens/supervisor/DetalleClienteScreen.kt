package me.valeriaacosta.uyparceshopper.ui.screens.supervisor

import androidx.compose.foundation.layout.*
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
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.fecha
import me.valeriaacosta.uyparceshopper.data.mensaje
import me.valeriaacosta.uyparceshopper.data.models.*
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── DetalleClienteScreen ────────────────────────────────────────────────────

/** Pantalla 34. Datos del cliente (teléfono, ciudad y dirección editables), saldo a favor y sus pedidos. */
@Composable
fun DetalleClienteScreen(
    id: String,
    onClickBack: () -> Unit,
    onClickPedido: (numero: Int) -> Unit
) {
    SegunCarga(rememberUsuario(id), "Cliente", onClickBack) { c -> DetalleClienteContenido(c, onClickBack, onClickPedido) }
}

@Composable
private fun DetalleClienteContenido(c: Usuario, onClickBack: () -> Unit, onClickPedido: (Int) -> Unit) {
    val estado = LocalEstadoApp.current
    val mensaje = LocalMensaje.current
    val scope = rememberCoroutineScope()
    var telefono by rememberSaveable(c.id) { mutableStateOf(c.telefono) }
    var ciudad by rememberSaveable(c.id) { mutableStateOf(c.ciudad) }
    var direccion by rememberSaveable(c.id) { mutableStateOf(c.direccion) }
    var errores by remember { mutableStateOf(mapOf<String, String>()) }
    var guardando by remember { mutableStateOf(false) }
    // Su historial se consulta al abrir la pantalla.
    val pedidos by produceState<List<Pedido>?>(null, c.id) {
        value = estado.pedidosDeCliente(c.id).getOrDefault(emptyList()).sortedByDescending { it.numero }
    }
    val saldo = pedidos?.let { Precios.saldoAFavor(it, c.moneda) } ?: 0.0
    val hayCambios = telefono.trim() != c.telefono || ciudad != c.ciudad || direccion.trim() != c.direccion

    UpScreen(c.nombre, onClickBack, subtitulo = c.tipoCliente?.etiqueta ?: "Sin tipo de cliente") {
        ContenidoScroll {
            if (saldo > 0) Tarjeta { FilaDinero("Saldo a favor", saldo, c.moneda, total = true) }
            Tarjeta {
                FilaDato("Cédula", c.cedula)
                FilaDato("Correo", c.correo)
                FilaDato("Cliente desde", fecha(c.creadoEn))
            }
            CampoTexto("Teléfono", telefono, { errores = errores - "telefono"; telefono = it.filter { ch -> ch.isDigit() || ch == ' ' } },
                teclado = KeyboardType.Phone, error = errores["telefono"])
            Selector("Ciudad", Zonas.ciudades, ciudad.ifBlank { null }, { errores = errores - "ciudad"; ciudad = it }, error = errores["ciudad"])
            CampoTexto("Dirección", direccion, { errores = errores - "direccion"; direccion = it }, lineas = 2, error = errores["direccion"])
            BotonPrincipal("Guardar datos", {
                val e = mutableMapOf<String, String>()
                if (telefono.filter(Char::isDigit).length != 10) e["telefono"] = "El celular tiene 10 dígitos"
                if (ciudad.isBlank()) e["ciudad"] = "Elige la ciudad"
                if (direccion.trim().length < 8) e["direccion"] = "Escribe la dirección completa"
                errores = e
                if (e.isEmpty()) {
                    guardando = true
                    scope.launch {
                        estado.actualizarContactoCliente(c, telefono, ciudad, direccion)
                            .onSuccess { mensaje("Datos del cliente guardados") }
                            .onFailure { mensaje(it.mensaje()) }
                        guardando = false
                    }
                }
            }, Modifier.fillMaxWidth(), habilitado = hayCambios, cargando = guardando)
            Text("El tipo de cliente solo lo cambia el administrador. Los pedidos ya hechos conservan la dirección de cuando se confirmaron.",
                color = Muted, fontSize = 13.sp)

            TituloSeccion("Pedidos (${pedidos?.size ?: 0})")
            when {
                pedidos == null -> Cargando()
                pedidos.orEmpty().isEmpty() -> Text("Todavía no tiene pedidos.", color = Muted)
            }
            pedidos.orEmpty().forEach { p ->
                Tarjeta(alPulsar = { onClickPedido(p.numero) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("#${p.numero} · ${fecha(p.fecha)}", fontFamily = Mono, color = Blue, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Dinero(p.desglose.total, p.moneda, 15.sp)
                    }
                    ChipEstado(p.estado)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DetalleClienteScreenPreview() {
    VistaPrevia(Rol.SUPERVISOR) { DetalleClienteScreen(id = "c2", onClickBack = {}, onClickPedido = {}) }
}
