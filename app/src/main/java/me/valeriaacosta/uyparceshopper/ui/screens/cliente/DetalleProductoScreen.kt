package me.valeriaacosta.uyparceshopper.ui.screens.cliente

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.RemoveShoppingCart
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.valeriaacosta.uyparceshopper.data.EstadoApp
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.models.*
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── DetalleProductoScreen ───────────────────────────────────────────────────

/** Pantalla 8. Fotos, precio, talla y cantidad. Agrega el producto al carrito guardado. */
@Composable
fun DetalleProductoScreen(
    id: String,
    onClickBack: () -> Unit,
    onAgregado: () -> Unit
) {
    SegunCarga(rememberProducto(id), "Producto", onClickBack) { p -> DetalleProductoContenido(p, onClickBack, onAgregado) }
}

@Composable
private fun DetalleProductoContenido(p: Producto, onClickBack: () -> Unit, onAgregado: () -> Unit) {
    val estado = LocalEstadoApp.current
    val mensaje = LocalMensaje.current
    val tipo = estado.usuario?.tipoCliente ?: TipoCliente.DETAL
    val precio = Precios.precioUnitario(p, tipo, estado.ajustes)
    var talla by rememberSaveable(p.id) { mutableStateOf(if (p.tallas.size == 1) p.tallas.first() else null) }
    var cantidad by rememberSaveable(p.id) { mutableIntStateOf(1) }
    var error by remember { mutableStateOf<String?>(null) }

    UpScreen(p.marca, onClickBack, barraInferior = {
        Column(modifier = Modifier.weight(1f)) {
            Text("Total", color = Muted, fontSize = 13.sp)
            Dinero(precio * cantidad, tipo.moneda, 18.sp)
        }
        BotonPrincipal(if (p.agotado) "Agotado" else "Agregar", {
            val t = talla
            if (t == null) error = "Elige una talla"
            else {
                estado.agregarAlCarrito(p, t, cantidad)
                mensaje("Agregado al carrito")
                onAgregado()
            }
        }, Modifier.weight(1.3f), Icons.Outlined.AddShoppingCart, habilitado = !p.agotado)
    }) {
        ContenidoScroll {
            GaleriaFotos(p)
            Text(p.nombre, style = MaterialTheme.typography.titleLarge)
            Dinero(precio, tipo.moneda, 24.sp)
            if (p.descripcion.isNotBlank()) Text(p.descripcion, color = Muted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(p.categoria.nombre, TonoChip.GRIS)
                Chip(p.tienda, TonoChip.AZUL)
            }
            if (p.agotado) Aviso("Este producto se agotó en la tienda.", TonoChip.ROJO, Icons.Outlined.RemoveShoppingCart)

            EtiquetaCampo("Talla")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                p.tallas.forEach { t ->
                    val sel = t == talla
                    Text(
                        t, fontWeight = FontWeight.Bold, color = if (sel) Color.White else Navy,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (sel) Navy else Field)
                            .clickable { talla = t; error = null }
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                    )
                }
            }
            error?.let { Text(it, color = Red) }

            EtiquetaCampo("Cantidad")
            SelectorCantidad(cantidad, { cantidad = it }, maximo = EstadoApp.MAX_UNIDADES)
        }
    }
}

/** Fotos del producto: se deslizan con el dedo; los puntos muestran en cuál vas. */
@Composable
private fun GaleriaFotos(p: Producto) {
    val total = p.fotos.size.coerceAtLeast(1)
    val pager = rememberPagerState { total }
    Box {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth().aspectRatio(1.1f).clip(RoundedCornerShape(24.dp))) { i ->
            FotoProducto(p, Modifier.fillMaxSize(), 0.dp, marcarAgotado = true, indice = i, completa = true)
        }
        if (total > 1) Row(
            Modifier.align(Alignment.BottomCenter).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(total) { i ->
                Box(Modifier.size(8.dp).clip(CircleShape).background(if (i == pager.currentPage) Yellow else Color.White.copy(alpha = 0.6f)))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DetalleProductoScreenPreview() {
    VistaPrevia { DetalleProductoScreen(id = "p1", onClickBack = {}, onAgregado = {}) }
}
