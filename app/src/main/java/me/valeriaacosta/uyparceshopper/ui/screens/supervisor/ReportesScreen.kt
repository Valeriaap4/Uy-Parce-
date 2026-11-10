package me.valeriaacosta.uyparceshopper.ui.screens.supervisor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.Periodo
import me.valeriaacosta.uyparceshopper.data.inicioDe
import me.valeriaacosta.uyparceshopper.data.mensaje
import me.valeriaacosta.uyparceshopper.data.models.*
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── ReportesScreen ──────────────────────────────────────────────────────────

/** Datos de un período para los reportes: pedidos pagados (sin los cancelados) y gastos. */
class DatosPeriodo(val pedidos: List<Pedido>, val gastos: List<Gasto>)

/**
 * Pantalla 39. Ventas, ganancia y gastos por período, separados en USD (mayoristas) y COP (detal).
 * Se calculan con los pedidos pagados en el período y los gastos registrados.
 */
@Composable
fun ReportesScreen(
    onClickBack: () -> Unit
) {
    val estado = LocalEstadoApp.current
    val mensaje = LocalMensaje.current
    val periodos = listOf(Periodo.SEMANA, Periodo.MES, Periodo.ANIO)
    var periodo by rememberSaveable { mutableIntStateOf(1) }
    val datos by produceState<DatosPeriodo?>(null, periodo) {
        value = null
        val desde = inicioDe(periodos[periodo])
        val pedidos = estado.pedidosPagadosDesde(desde).onFailure { mensaje(it.mensaje()) }.getOrDefault(emptyList())
        val gastos = estado.gastosDesde(desde).onFailure { mensaje(it.mensaje()) }.getOrDefault(emptyList())
        value = DatosPeriodo(pedidos.filter { it.estado != EstadoPedido.CANCELADO }, gastos)
    }

    UpScreen("Reportes", onClickBack) {
        ContenidoScroll {
            PestanasPildora(periodos.map { it.etiqueta }, periodo, { periodo = it })
            val d = datos
            if (d == null) Cargando()
            else {
                listOf(Moneda.USD, Moneda.COP).forEach { m ->
                    val ventas = d.pedidos.filter { it.moneda == m }
                    val gastos = d.gastos.filter { it.moneda == m }.sumOf { it.monto }
                    val ganancia = ventas.sumOf { it.desglose.ganancia }
                    Tarjeta {
                        Text(if (m == Moneda.USD) "Mayoristas (USD)" else "Detal (COP)", style = MaterialTheme.typography.titleMedium)
                        Text(cantidad(ventas.size, "pedido") + " pagados", color = Muted, fontSize = 13.sp)
                        FilaDinero("Ventas", ventas.sumOf { it.desglose.total }, m)
                        FilaDinero(if (m == Moneda.USD) "Comisiones (ganancia)" else "Margen (ganancia)", ganancia, m)
                        FilaDinero("Gastos", gastos, m)
                        HorizontalDivider(color = Line)
                        FilaDinero("Ganancia neta", Precios.redondear(ganancia - gastos, m), m, total = true)
                    }
                }
                // Para comparar categorías, los gastos en dólares se pasan a pesos con la tasa actual.
                val tasa = estado.ajustes.tasaDolar
                val porCategoria = d.gastos.groupBy { it.categoria }
                    .mapValues { (_, lista) -> lista.sumOf { if (it.moneda == Moneda.USD) it.monto * tasa else it.monto } }
                    .entries.sortedByDescending { it.value }
                val total = porCategoria.sumOf { it.value }
                Tarjeta {
                    Text("Gastos por categoría", style = MaterialTheme.typography.titleMedium)
                    if (porCategoria.isEmpty()) Text("No hay gastos en este período.", color = Muted)
                    porCategoria.forEach { (categoria, valor) ->
                        Text("$categoria · ${formatoDinero(valor, Moneda.COP)} COP", color = Muted, fontSize = 14.sp)
                        LinearProgressIndicator(
                            progress = { if (total > 0) (valor / total).toFloat() else 0f },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                            color = Blue, trackColor = GreySoft
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReportesScreenPreview() {
    VistaPrevia(Rol.SUPERVISOR) { ReportesScreen(onClickBack = {}) }
}
