package me.valeriaacosta.uyparceshopper.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.Periodo
import me.valeriaacosta.uyparceshopper.data.aTimestamp
import me.valeriaacosta.uyparceshopper.data.enColombia
import me.valeriaacosta.uyparceshopper.data.inicioDe
import me.valeriaacosta.uyparceshopper.data.zonaColombia
import me.valeriaacosta.uyparceshopper.data.models.*
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*
import java.time.LocalDate

// ─── AdminInicioScreen ───────────────────────────────────────────────────────

/**
 * Pantalla 40. Estado de la operación (en vivo) y, para el período elegido, ganancias, ventas,
 * top clientes y productos. Se calculan con los pedidos pagados (sin los cancelados).
 */
@Composable
fun AdminInicioScreen() {
    val estado = LocalEstadoApp.current
    val periodos = listOf(Periodo.HOY, Periodo.SEMANA, Periodo.MES)
    var periodo by rememberSaveable { mutableIntStateOf(0) }
    val pagados by produceState<List<Pedido>?>(null, periodo) {
        value = null
        value = estado.pedidosPagadosDesde(inicioDe(periodos[periodo])).getOrDefault(emptyList())
            .filter { it.estado != EstadoPedido.CANCELADO }
    }
    // La gráfica siempre muestra los últimos 7 días.
    val hoy = LocalDate.now(zonaColombia)
    val semana by produceState<List<Pedido>?>(null) {
        value = estado.pedidosPagadosDesde(hoy.minusDays(6).atStartOfDay(zonaColombia).aTimestamp()).getOrDefault(emptyList())
            .filter { it.estado != EstadoPedido.CANCELADO }
    }
    val activos = estado.pedidos
    val lista = pagados.orEmpty()

    UpScreen("Resumen", subtitulo = "UyParce Shopper", cabecera = { TarjetasGanancia(lista) }) {
        ContenidoScroll {
            PestanasPildora(periodos.map { it.etiqueta }, periodo, { periodo = it })

            TituloSeccion("Estado de la operación")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    "Por pagar" to activos.count { it.estado == EstadoPedido.PENDIENTE_PAGO || it.estado == EstadoPedido.PAGO_EN_REVISION },
                    "Por comprar" to activos.count { it.estado == EstadoPedido.PAGADO },
                    "En camino" to activos.count { it.estado == EstadoPedido.COMPRADO || it.estado == EstadoPedido.EN_CAMINO },
                    "Por despachar" to activos.count { it.estado == EstadoPedido.EN_CUCUTA }
                ).forEach { (titulo, n) ->
                    Column(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(Color.White).padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("$n", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                        Text(titulo, color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                }
            }

            if (pagados == null) Cargando()
            else {
                Tarjeta {
                    Text("Ventas y pedidos", style = MaterialTheme.typography.titleMedium)
                    FilaDato("Pedidos pagados", "${lista.size}")
                    FilaDato("Prendas vendidas", "${lista.sumOf { it.prendas }}")
                    FilaDinero("Ventas mayoristas", lista.filter { it.moneda == Moneda.USD }.sumOf { it.desglose.total }, Moneda.USD)
                    FilaDinero("Ventas detal", lista.filter { it.moneda == Moneda.COP }.sumOf { it.desglose.total }, Moneda.COP)
                }
                Tarjeta {
                    Text("Top clientes", style = MaterialTheme.typography.titleMedium)
                    val top = lista.groupBy { it.clienteId }.values
                        .sortedWith(compareByDescending<List<Pedido>> { it.size }.thenByDescending { g -> g.sumOf { it.desglose.total } })
                        .take(3)
                    if (top.isEmpty()) Text("Sin pedidos pagados en este período.", color = Muted)
                    top.forEachIndexed { i, g ->
                        val p = g.first()
                        FilaDato("${i + 1}. ${p.cliente.nombre}", "${cantidad(g.size, "pedido")} · ${formatoDinero(g.sumOf { it.desglose.total }, p.moneda)} ${p.moneda}")
                    }
                }
                Tarjeta {
                    Text("Productos más pedidos", style = MaterialTheme.typography.titleMedium)
                    val top = lista.flatMap { it.lineas }.groupBy { it.producto.id }
                        .map { (_, ls) -> ls.first().producto.nombre to ls.sumOf { it.cantidad } }
                        .sortedByDescending { it.second }.take(3)
                    if (top.isEmpty()) Text("Sin pedidos pagados en este período.", color = Muted)
                    top.forEachIndexed { i, (nombre, n) -> FilaDato("${i + 1}. $nombre", "$n und.") }
                }
            }
            semana?.let { GraficaSemana(it, hoy, estado.ajustes.tasaDolar) }
        }
    }
}

// ── Ganancias en la cabecera (comisiones USD y márgenes COP) ──
@Composable
private fun TarjetasGanancia(pedidos: List<Pedido>) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(end = 8.dp)) {
        listOf(
            Triple("Ganancia mayoristas", pedidos.filter { it.moneda == Moneda.USD }.sumOf { it.desglose.ganancia }, Moneda.USD),
            Triple("Ganancia detal", pedidos.filter { it.moneda == Moneda.COP }.sumOf { it.desglose.ganancia }, Moneda.COP)
        ).forEach { (t, v, m) ->
            Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(NavySoft).padding(14.dp)) {
                Text(t, color = NavyText, fontSize = 13.sp)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(formatoDinero(v, m), color = Color.White, fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                    Text(" ${m.name}", color = Yellow, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

// ── Barras de ganancia por día (últimos 7 días; los dólares se pasan a pesos con la tasa actual) ──
@Composable
private fun GraficaSemana(pedidos: List<Pedido>, hoy: LocalDate, tasa: Double) {
    val dias = (6 downTo 0).map { hoy.minusDays(it.toLong()) }
    val porDia = dias.map { dia ->
        pedidos.filter { it.pagadoEn?.enColombia()?.toLocalDate() == dia }
            .sumOf { if (it.moneda == Moneda.USD) it.desglose.ganancia * tasa else it.desglose.ganancia }
    }
    val maximo = porDia.maxOrNull() ?: 0.0
    val letras = listOf("L", "M", "M", "J", "V", "S", "D")
    Tarjeta {
        Text("Ganancia por día (últimos 7 días)", style = MaterialTheme.typography.titleMedium)
        if (maximo <= 0) Text("Todavía no hay ganancias esta semana.", color = Muted)
        Row(
            modifier = Modifier.fillMaxWidth().height(110.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            porDia.forEach { v ->
                val alto = if (maximo > 0) (v / maximo).toFloat().coerceIn(0.03f, 1f) else 0.03f
                Box(Modifier.weight(1f).fillMaxHeight(alto).clip(RoundedCornerShape(6.dp)).background(if (v == maximo && v > 0) Yellow else Blue))
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            dias.forEach { Text(letras[it.dayOfWeek.value - 1], color = Muted, fontSize = 12.sp, modifier = Modifier.weight(1f)) }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AdminInicioScreenPreview() {
    VistaPrevia(Rol.ADMIN) { AdminInicioScreen() }
}
