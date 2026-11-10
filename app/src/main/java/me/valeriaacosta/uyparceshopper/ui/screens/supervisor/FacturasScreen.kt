package me.valeriaacosta.uyparceshopper.ui.screens.supervisor

import me.valeriaacosta.uyparceshopper.data.*

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.valeriaacosta.uyparceshopper.data.DatosEjemplo
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.models.Precios
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── FacturasScreen ──────────────────────────────────────────────────────────

/** Pantalla 35. Repositorio de facturas con buscador. */
@Composable
fun FacturasScreen(
    onClickBack: () -> Unit,
    onClickFactura: (numeroPedido: Int) -> Unit
) {
    val ajustes = LocalEstadoApp.current.ajustes
    var buscar by remember { mutableStateOf("") }
    val lista = DatosEjemplo.facturas.filter {
        it.pedido.cliente.nombre.contains(buscar, true) || it.numero.contains(buscar, true)
    }

    UpScreen("Facturas", onClickBack) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { BarraBuscar(buscar, { buscar = it }, "Buscar por cliente o número") }
            items(lista, key = { it.numero }) { f ->
                val d = Precios.desglose(f.pedido, ajustes)
                Tarjeta(alPulsar = { onClickFactura(f.pedido.numero) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(f.numero, fontFamily = Mono, color = Blue, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                            modifier = Modifier.weight(1f))
                        Dinero(d.total, d.moneda, 15.sp)
                    }
                    Text("${f.pedido.cliente.nombre} · ${fecha(f.fecha)}", color = Muted)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FacturasScreenPreview() {
    UyParceTheme { FacturasScreen(onClickBack = {}, onClickFactura = {}) }
}
