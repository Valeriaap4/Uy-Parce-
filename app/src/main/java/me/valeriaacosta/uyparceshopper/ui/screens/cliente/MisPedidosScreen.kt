package me.valeriaacosta.uyparceshopper.ui.screens.cliente

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.valeriaacosta.uyparceshopper.data.DatosEjemplo
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.models.EstadoPedido
import me.valeriaacosta.uyparceshopper.data.models.Pedido
import me.valeriaacosta.uyparceshopper.data.models.Precios
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── MisPedidosScreen ────────────────────────────────────────────────────────

/** Pantalla 13. Pedidos del cliente con filtro Todos / En curso / Terminados. */
@Composable
fun MisPedidosScreen(
    onClickPedido: (numero: Int) -> Unit
) {
    val estado = LocalEstadoApp.current
    val idCliente = estado.usuario?.id ?: DatosEjemplo.laura.id
    val mios = DatosEjemplo.pedidos.filter { it.cliente.id == idCliente }.sortedByDescending { it.numero }
    var filtro by remember { mutableIntStateOf(0) }
    val terminado = { p: Pedido -> p.estado == EstadoPedido.ENTREGADO || p.estado == EstadoPedido.CANCELADO }
    val lista = when (filtro) {
        1 -> mios.filterNot(terminado)
        2 -> mios.filter(terminado)
        else -> mios
    }

    UpScreen("Mis pedidos") {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { PestanasPildora(listOf("Todos", "En curso", "Terminados"), filtro, { filtro = it }) }
            items(lista, key = { it.numero }) { p ->
                val d = Precios.desglose(p, estado.ajustes)
                Tarjeta(alPulsar = { onClickPedido(p.numero) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("#${p.numero}", fontFamily = Mono, color = Blue, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("  · ${p.fecha}", color = Muted, modifier = Modifier.weight(1f))
                        Dinero(d.total, d.moneda, 15.sp)
                    }
                    Text(p.lineas.joinToString { it.producto.nombre }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChipEstado(p.estado)
                        Chip(cantidad(p.prendas, "prenda"), TonoChip.GRIS)
                    }
                }
            }
            if (lista.isEmpty()) item { EstadoVacio(Icons.AutoMirrored.Outlined.ReceiptLong, "No tienes pedidos aquí") }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MisPedidosScreenPreview() {
    UyParceTheme { MisPedidosScreen(onClickPedido = {}) }
}
