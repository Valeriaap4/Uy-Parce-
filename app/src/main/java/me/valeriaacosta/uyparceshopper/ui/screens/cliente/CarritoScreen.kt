package me.valeriaacosta.uyparceshopper.ui.screens.cliente

import me.valeriaacosta.uyparceshopper.data.*

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.models.Precios
import me.valeriaacosta.uyparceshopper.data.models.TipoCliente
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── CarritoScreen ───────────────────────────────────────────────────────────

/** Pantalla 9. Prendas del carrito, cantidades y aviso de agotados. */
@Composable
fun CarritoScreen(
    onClickProducto: (id: String) -> Unit,
    onClickContinuar: () -> Unit,
    onClickVerCatalogo: () -> Unit
) {
    val estado = LocalEstadoApp.current
    val tipo = estado.usuario?.tipoCliente ?: TipoCliente.DETAL
    val carrito = estado.carrito
    val hayAgotados = carrito.any { it.producto.agotado }
    val subtotal = carrito.filterNot { it.producto.agotado }
        .sumOf { Precios.precioUnitario(it.producto, tipo, estado.ajustes) * it.cantidad }

    UpScreen(
        titulo = "Carrito",
        subtitulo = cantidad(carrito.sumOf { it.cantidad }, "prenda"),
        barraInferior = if (carrito.isEmpty()) null else ({
            Column(modifier = Modifier.weight(1f)) {
                Text("Subtotal", color = Muted, fontSize = 13.sp)
                Dinero(subtotal, tipo.moneda, 18.sp)
            }
            BotonPrincipal("Continuar", onClickContinuar, Modifier.weight(1.2f), habilitado = !hayAgotados)
        })
    ) {
        if (carrito.isEmpty()) {
            EstadoVacio(Icons.Outlined.ShoppingBag, "Tu carrito está vacío")
            BotonAmarillo("Ver catálogo", onClickVerCatalogo, Modifier.padding(horizontal = 16.dp).fillMaxWidth())
        } else LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (hayAgotados) item {
                Aviso("Hay productos agotados. Quítalos para poder continuar.", TonoChip.ROJO, Icons.Outlined.Warning)
            }
            itemsIndexed(carrito, key = { _, l -> l.producto.id + l.talla }) { i, l ->
                Tarjeta(alPulsar = { onClickProducto(l.producto.id) }) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FotoProducto(l.producto, Modifier.size(72.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(l.producto.nombre, fontWeight = FontWeight.Bold)
                            Text("Talla ${l.talla}", color = Muted)
                            Dinero(Precios.precioUnitario(l.producto, tipo, estado.ajustes) * l.cantidad, tipo.moneda, 15.sp)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (l.producto.agotado) Chip("Agotado", TonoChip.ROJO)
                        else SelectorCantidad(l.cantidad, { estado.cambiarCantidad(i, it) })
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = { estado.cambiarCantidad(i, 0) }) {
                            Icon(Icons.Outlined.Delete, null, tint = Red)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Quitar", color = Red)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CarritoScreenPreview() {
    UyParceTheme { CarritoScreen(onClickProducto = {}, onClickContinuar = {}, onClickVerCatalogo = {}) }
}
