package me.valeriaacosta.uyparceshopper.ui.screens.admin

import me.valeriaacosta.uyparceshopper.data.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.valeriaacosta.uyparceshopper.data.DatosEjemplo
import me.valeriaacosta.uyparceshopper.data.models.EstadoPedido
import me.valeriaacosta.uyparceshopper.data.models.Moneda
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── AdminInicioScreen ───────────────────────────────────────────────────────

/** Pantalla 40. Stats: ganancias, estado de la operación, ventas, top clientes y productos. */
@Composable
fun AdminInicioScreen() {
    var periodo by remember { mutableIntStateOf(0) }
    val conteo = EstadoPedido.entries.associateWith { e -> DatosEjemplo.pedidos.count { it.estado == e } }

    UpScreen("Resumen", subtitulo = "UyParce Shopper", cabecera = { TarjetasGanancia() }) {
        ContenidoScroll {
            PestanasPildora(listOf("Hoy", "Semana", "Mes"), periodo, { periodo = it })

            TituloSeccion("Estado de la operación")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    "Por pagar" to (conteo.getValue(EstadoPedido.PENDIENTE_PAGO) + conteo.getValue(EstadoPedido.PAGO_EN_REVISION)),
                    "Por comprar" to conteo.getValue(EstadoPedido.PAGADO),
                    "En camino" to conteo.getValue(EstadoPedido.EN_CAMINO),
                    "Por despachar" to conteo.getValue(EstadoPedido.EN_CUCUTA)
                ).forEach { (titulo, n) ->
                    Column(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(Color.White).padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("$n", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                        Text(titulo, color = Muted, fontSize = 11.sp)
                    }
                }
            }

            Tarjeta {
                Text("Ventas y pedidos", style = MaterialTheme.typography.titleMedium)
                FilaDato("Pedidos", "${DatosEjemplo.pedidos.size}")
                FilaDato("Prendas vendidas", "${DatosEjemplo.pedidos.sumOf { it.prendas }}")
                FilaDinero("Ventas mayoristas", 1214.40, Moneda.USD)
                FilaDinero("Ventas detal", 3865000.0, Moneda.COP)
            }
            GraficaSemana()
            Tarjeta {
                Text("Top clientes", style = MaterialTheme.typography.titleMedium)
                DatosEjemplo.clientes.take(3).forEachIndexed { i, c -> FilaDato("${i + 1}. ${c.nombre}", c.tipoCliente?.etiqueta ?: "") }
            }
            Tarjeta {
                Text("Productos más pedidos", style = MaterialTheme.typography.titleMedium)
                DatosEjemplo.productos.take(3).forEachIndexed { i, p -> FilaDato("${i + 1}. ${p.nombre}", "${8 - i * 2} und.") }
            }
        }
    }
}

// ── Ganancias en la cabecera (comisiones USD y márgenes COP) ──
@Composable
private fun TarjetasGanancia() {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(end = 8.dp)) {
        listOf(Triple("Ganancia mayoristas", "$248.30", "USD"), Triple("Ganancia detal", "$892.000", "COP")).forEach { (t, v, m) ->
            Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(NavySoft).padding(14.dp)) {
                Text(t, color = NavyText, fontSize = 13.sp)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(v, color = Color.White, fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                    Text(" $m", color = Yellow, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

// ── Barras de ganancia por día ──
@Composable
private fun GraficaSemana() {
    Tarjeta {
        Text("Ganancia por día", style = MaterialTheme.typography.titleMedium)
        val valores = listOf(0.4f, 0.65f, 0.3f, 0.85f, 0.5f, 1f, 0.7f)
        Row(
            modifier = Modifier.fillMaxWidth().height(110.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            valores.forEach { v ->
                Box(Modifier.weight(1f).fillMaxHeight(v).clip(RoundedCornerShape(6.dp)).background(if (v == 1f) Yellow else Blue))
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("L", "M", "M", "J", "V", "S", "D").forEach { Text(it, color = Muted, fontSize = 12.sp, modifier = Modifier.weight(1f)) }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AdminInicioScreenPreview() {
    UyParceTheme { AdminInicioScreen() }
}
