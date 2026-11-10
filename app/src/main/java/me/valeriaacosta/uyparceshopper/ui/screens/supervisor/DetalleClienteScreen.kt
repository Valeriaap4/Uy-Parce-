package me.valeriaacosta.uyparceshopper.ui.screens.supervisor

import me.valeriaacosta.uyparceshopper.data.*

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import me.valeriaacosta.uyparceshopper.data.DatosEjemplo
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.models.Moneda
import me.valeriaacosta.uyparceshopper.data.models.Precios
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── DetalleClienteScreen ────────────────────────────────────────────────────

/** Pantalla 34. Datos del cliente (editables), saldo a favor y sus pedidos. */
@Composable
fun DetalleClienteScreen(
    id: String,
    onClickBack: () -> Unit,
    onClickPedido: (numero: Int) -> Unit
) {
    val c = DatosEjemplo.clientes.firstOrNull { it.id == id } ?: return NoEncontrado("Cliente", onClickBack)
    val ajustes = LocalEstadoApp.current.ajustes
    val mensaje = LocalMensaje.current
    var telefono by remember { mutableStateOf(c.telefono) }
    var direccion by remember { mutableStateOf(c.direccion) }
    val pedidos = DatosEjemplo.pedidos.filter { it.cliente.id == id }

    UpScreen(c.nombre, onClickBack, subtitulo = c.tipoCliente?.etiqueta) {
        ContenidoScroll {
            if (c.saldoAFavor > 0) Tarjeta {
                FilaDinero("Saldo a favor", c.saldoAFavor, c.tipoCliente?.moneda ?: Moneda.COP, total = true)
            }
            Tarjeta {
                FilaDato("Cédula", c.cedula)
                FilaDato("Correo", c.correo)
                FilaDato("Ciudad", c.ciudad)
            }
            CampoTexto("Teléfono", telefono, { telefono = it }, teclado = KeyboardType.Phone)
            CampoTexto("Dirección", direccion, { direccion = it }, lineas = 2,
                error = if (direccion.length < 8) "Escribe la dirección completa" else null)
            BotonPrincipal("Guardar datos", { if (direccion.length >= 8) mensaje("Datos del cliente guardados") }, Modifier.fillMaxWidth())
            Text("El tipo de cliente solo lo cambia el administrador.", color = Muted, fontSize = 13.sp)

            TituloSeccion("Pedidos (${pedidos.size})")
            pedidos.forEach { p ->
                val d = Precios.desglose(p, ajustes)
                Tarjeta(alPulsar = { onClickPedido(p.numero) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("#${p.numero}", fontFamily = Mono, color = Blue, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Dinero(d.total, d.moneda, 15.sp)
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
    UyParceTheme { DetalleClienteScreen(id = "c2", onClickBack = {}, onClickPedido = {}) }
}
