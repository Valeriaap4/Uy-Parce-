package me.valeriaacosta.uyparceshopper.ui.screens.supervisor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.fecha
import me.valeriaacosta.uyparceshopper.data.mensaje
import me.valeriaacosta.uyparceshopper.data.models.Factura
import me.valeriaacosta.uyparceshopper.data.models.Rol
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── FacturasScreen ──────────────────────────────────────────────────────────

/** Pantalla 35. Facturas recientes (se consultan al abrir la pantalla) con buscador. */
@Composable
fun FacturasScreen(
    onClickBack: () -> Unit,
    onClickFactura: (numeroPedido: Int) -> Unit
) {
    val estado = LocalEstadoApp.current
    var buscar by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val facturas by produceState<List<Factura>?>(null) {
        value = estado.facturasRecientes().onFailure { error = it.mensaje() }.getOrDefault(emptyList())
    }
    val lista = facturas.orEmpty().filter {
        buscar.isBlank() || it.pedido.cliente.nombre.contains(buscar, true) || it.numero.contains(buscar, true) ||
            it.numeroPedido.toString().contains(buscar)
    }

    UpScreen("Facturas", onClickBack) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { BarraBuscar(buscar, { buscar = it }, "Buscar por cliente, factura o pedido") }
            error?.let { item { Aviso(it, TonoChip.ROJO) } }
            items(lista, key = { it.numero }) { f ->
                Tarjeta(alPulsar = { onClickFactura(f.numeroPedido) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(f.numero, fontFamily = Mono, color = Blue, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                            modifier = Modifier.weight(1f))
                        Dinero(f.pedido.totalAPagar, f.pedido.moneda, 15.sp)
                    }
                    Text("${f.pedido.cliente.nombre} · pedido #${f.numeroPedido} · ${fecha(f.fecha)}", color = Muted)
                }
            }
            if (lista.isEmpty()) item {
                if (facturas == null) Cargando() else EstadoVacio(Icons.Outlined.Description, "No hay facturas aquí")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FacturasScreenPreview() {
    VistaPrevia(Rol.SUPERVISOR) { FacturasScreen(onClickBack = {}, onClickFactura = {}) }
}
