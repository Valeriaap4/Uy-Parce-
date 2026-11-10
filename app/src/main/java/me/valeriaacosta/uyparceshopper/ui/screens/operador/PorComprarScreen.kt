package me.valeriaacosta.uyparceshopper.ui.screens.operador

import me.valeriaacosta.uyparceshopper.data.*

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.valeriaacosta.uyparceshopper.data.DatosEjemplo
import me.valeriaacosta.uyparceshopper.data.models.EstadoPedido
import me.valeriaacosta.uyparceshopper.data.models.Producto
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── PorComprarScreen ────────────────────────────────────────────────────────

/** Lo que falta comprar de un producto en una talla, sumando todos los pedidos pagados. */
data class Pendiente(val producto: Producto, val talla: String, val cantidad: Int, val pedidos: List<Int>)

fun pendientesPorComprar(): List<Pendiente> =
    DatosEjemplo.pedidos.filter { it.estado == EstadoPedido.PAGADO }
        .flatMap { p -> p.lineas.filter { it.compradas < it.cantidad }.map { Triple(p.numero, it, it.cantidad - it.compradas) } }
        .groupBy { it.second.producto.id + "|" + it.second.talla }
        .map { (_, g) -> Pendiente(g.first().second.producto, g.first().second.talla, g.sumOf { it.third }, g.map { it.first }) }

/** Pantalla 20. Lista de compras agrupada por tienda. */
@Composable
fun PorComprarScreen(
    onClickBack: (() -> Unit)? = null,
    onClickRegistrar: () -> Unit
) {
    val pendientes = pendientesPorComprar()
    UpScreen("Por comprar", onClickBack, cabecera = {
        ResumenCabecera(
            "${pendientes.sumOf { it.cantidad }}" to "Prendas",
            "${pendientes.flatMap { it.pedidos }.distinct().size}" to "Pedidos",
            "${pendientes.map { it.producto.tienda }.distinct().size}" to "Tiendas"
        )
    }, barraInferior = {
        BotonAmarillo("Registrar compra", onClickRegistrar, Modifier.fillMaxWidth(), Icons.Outlined.QrCodeScanner)
    }) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("Solo aparecen pedidos con el pago aprobado.", color = Muted) }
            pendientes.groupBy { it.producto.tienda }.forEach { (tienda, lista) ->
                item { TituloSeccion(tienda) }
                items(lista, key = { it.producto.id + it.talla }) { p ->
                    Tarjeta {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            FotoProducto(p.producto, Modifier.size(56.dp), 12.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(p.producto.nombre, fontWeight = FontWeight.Bold)
                                Text("Talla ${p.talla} · pedidos ${p.pedidos.joinToString { "#$it" }}", color = Muted, fontSize = 14.sp)
                            }
                            Text("× ${p.cantidad}", fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                    }
                }
            }
            if (pendientes.isEmpty()) item { EstadoVacio(Icons.Outlined.TaskAlt, "No hay nada pendiente por comprar") }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PorComprarScreenPreview() {
    UyParceTheme { PorComprarScreen(onClickRegistrar = {}) }
}
