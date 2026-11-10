package me.valeriaacosta.uyparceshopper.ui.screens.supervisor

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.hace
import me.valeriaacosta.uyparceshopper.data.mensaje
import me.valeriaacosta.uyparceshopper.data.models.*
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── CarteraScreen ───────────────────────────────────────────────────────────

/** Una devolución que el negocio todavía debe hacer. */
private data class Devolucion(val pedido: Pedido, val faltante: Faltante, val cuenta: String)

/**
 * Pantalla 36. Saldos a favor de los clientes y devoluciones pendientes, separados en USD y COP.
 * No se guardan como números aparte: se calculan con los faltantes y el saldo usado en cada pedido.
 */
@Composable
fun CarteraScreen(
    onClickBack: () -> Unit,
    onClickCliente: (id: String) -> Unit
) {
    val estado = LocalEstadoApp.current
    val mensaje = LocalMensaje.current
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableIntStateOf(1) }
    var recargar by remember { mutableIntStateOf(0) }
    var marcando by remember { mutableStateOf<String?>(null) }
    val pedidos by produceState<List<Pedido>?>(null, recargar) {
        value = estado.pedidosConSaldo().onFailure { mensaje(it.mensaje()) }.getOrDefault(emptyList())
    }
    val moneda = if (tab == 0) Moneda.USD else Moneda.COP
    val lista = pedidos.orEmpty()

    // Saldo de cada cliente en esta moneda.
    val saldos = lista.groupBy { it.clienteId }.mapNotNull { (_, delCliente) ->
        val saldo = Precios.saldoAFavor(delCliente, moneda)
        if (saldo > 0) delCliente.maxBy { it.numero } to saldo else null
    }.sortedByDescending { it.second }
    // Devoluciones elegidas por el cliente que todavía no se han hecho.
    val devoluciones = lista.flatMap { p ->
        p.faltantes.filter { f -> f.moneda == moneda && !f.devuelto && p.resoluciones[f.id]?.tipo == TipoResolucion.DEVOLUCION }
            .map { f -> Devolucion(p, f, p.resoluciones[f.id]?.cuenta ?: "") }
    }
    val clientes = (saldos.map { it.first.clienteId } + devoluciones.map { it.pedido.clienteId }).distinct().size

    UpScreen("Cartera", onClickBack, cabecera = {
        ResumenCabecera(
            formatoDinero(saldos.sumOf { it.second }, moneda) to "Saldos a favor",
            formatoDinero(devoluciones.sumOf { it.faltante.valor }, moneda) to "Devoluciones",
            "$clientes" to "Clientes"
        )
    }) {
        ContenidoScroll {
            PestanasPildora(listOf("USD · Mayoristas", "COP · Detal"), tab, { tab = it })
            when {
                pedidos == null -> Cargando()
                saldos.isEmpty() && devoluciones.isEmpty() ->
                    EstadoVacio(Icons.Outlined.AccountBalanceWallet, "Sin saldos ni devoluciones en $moneda")
                else -> {
                    if (saldos.isNotEmpty()) TituloSeccion("Saldos a favor")
                    saldos.forEach { (ultimo, saldo) ->
                        Tarjeta(alPulsar = { onClickCliente(ultimo.clienteId) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ultimo.cliente.nombre, fontWeight = FontWeight.Bold)
                                    Text("Se descuenta en su próximo pedido", color = Muted, fontSize = 14.sp)
                                }
                                Dinero(saldo, moneda, 15.sp)
                            }
                        }
                    }
                    if (devoluciones.isNotEmpty()) TituloSeccion("Devoluciones pendientes")
                    devoluciones.forEach { d ->
                        Tarjeta {
                            Text("${d.pedido.cliente.nombre} · ${d.cuenta.ifBlank { "sin cuenta" }}", fontWeight = FontWeight.Bold)
                            Text("${d.faltante.detalle} · pedido #${d.pedido.numero} · ${hace(d.faltante.fecha)}", color = Muted, fontSize = 14.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Dinero(d.faltante.valor, moneda, 15.sp)
                                Spacer(modifier = Modifier.weight(1f))
                                TextButton(onClick = {
                                    marcando = d.faltante.id
                                    scope.launch {
                                        estado.marcarDevuelto(d.pedido.numero, d.faltante.id)
                                            .onSuccess { mensaje("Devolución marcada como hecha"); recargar++ }
                                            .onFailure { mensaje(it.mensaje()) }
                                        marcando = null
                                    }
                                }, enabled = marcando == null) {
                                    Text(if (marcando == d.faltante.id) "Guardando…" else "Marcar hecha", color = Blue, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CarteraScreenPreview() {
    VistaPrevia(Rol.SUPERVISOR) { CarteraScreen(onClickBack = {}, onClickCliente = {}) }
}
