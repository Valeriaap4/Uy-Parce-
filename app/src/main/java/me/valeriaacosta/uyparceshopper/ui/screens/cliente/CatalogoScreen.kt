package me.valeriaacosta.uyparceshopper.ui.screens.cliente

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.valeriaacosta.uyparceshopper.data.DatosEjemplo
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.models.Precios
import me.valeriaacosta.uyparceshopper.data.models.Producto
import me.valeriaacosta.uyparceshopper.data.models.TipoCliente
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── CatalogoScreen ──────────────────────────────────────────────────────────

/** Pantalla 7. Catálogo en cuadrícula con buscador y filtro por categoría. */
@Composable
fun CatalogoScreen(
    onClickProducto: (id: String) -> Unit,
    onClickNotificaciones: () -> Unit
) {
    val estado = LocalEstadoApp.current
    val tipo = estado.usuario?.tipoCliente ?: TipoCliente.DETAL
    var buscar by remember { mutableStateOf("") }
    var categoria by remember { mutableIntStateOf(0) }
    val categorias = listOf("Todo") + DatosEjemplo.categorias.map { it.nombre }
    val lista = DatosEjemplo.productos.filter {
        (categoria == 0 || it.categoria.nombre == categorias[categoria]) &&
            (buscar.isBlank() || it.nombre.contains(buscar, true) || it.marca.contains(buscar, true))
    }

    UpScreen(
        titulo = "Catálogo",
        subtitulo = "Hola, ${estado.usuario?.nombre?.substringBefore(" ") ?: "parce"} · ${tipo.etiqueta}",
        acciones = {
            IconButton(onClick = onClickNotificaciones) {
                BadgedBox(badge = { Badge(containerColor = Yellow, contentColor = Navy) { Text("2") } }) {
                    Icon(Icons.Outlined.Notifications, "Notificaciones", tint = Color.White)
                }
            }
        }
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(160.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BarraBuscar(buscar, { buscar = it }, "Buscar por nombre o marca")
                    PestanasPildora(categorias, categoria, { categoria = it })
                    if (tipo.esMayorista) Aviso(
                        "Precios del outlet en USD. Al pagar se suma tu comisión (${(tipo.comision * 100).toInt()} %) y el envío.",
                        TonoChip.AZUL
                    )
                }
            }
            items(lista, key = { it.id }) { p ->
                TarjetaProducto(p, Precios.precioUnitario(p, tipo, estado.ajustes), tipo) { onClickProducto(p.id) }
            }
            if (lista.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                EstadoVacio(Icons.Outlined.SearchOff, "No encontramos productos con esa búsqueda")
            }
        }
    }
}

/** Tarjeta de un producto en la cuadrícula del catálogo. */
@Composable
private fun TarjetaProducto(p: Producto, precio: Double, tipo: TipoCliente, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, Line, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        FotoProducto(p, Modifier.fillMaxWidth().aspectRatio(1f), redondeo = 0.dp, marcarAgotado = true)
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(p.marca.uppercase(), color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
            Text(p.nombre, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2)
            Dinero(precio, tipo.moneda, 15.sp)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CatalogoScreenPreview() {
    UyParceTheme { CatalogoScreen(onClickProducto = {}, onClickNotificaciones = {}) }
}
