package me.valeriaacosta.uyparceshopper.ui.screens.operador

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.valeriaacosta.uyparceshopper.data.Listas
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.models.Moneda
import me.valeriaacosta.uyparceshopper.data.models.Rol
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── OpCatalogoScreen ────────────────────────────────────────────────────────

/** Pantalla 18. Productos que el operador subió desde el outlet (en vivo). */
@Composable
fun OpCatalogoScreen(
    onClickBack: (() -> Unit)? = null,
    onClickProducto: (id: String) -> Unit,
    onClickNuevo: () -> Unit
) {
    val estado = LocalEstadoApp.current
    var filtro by rememberSaveable { mutableIntStateOf(0) }
    var buscar by rememberSaveable { mutableStateOf("") }
    val productos = estado.productos
    val lista = productos.filter {
        (if (filtro == 0) !it.agotado else it.agotado) &&
            (buscar.isBlank() || it.nombre.contains(buscar, true) || it.marca.contains(buscar, true))
    }

    UpScreen("Catálogo", onClickBack, cabecera = {
        ResumenCabecera(
            "${productos.count { !it.agotado }}" to "Activos",
            "${productos.count { it.agotado }}" to "Agotados",
            "${estado.ajustes.categorias.size}" to "Categorías"
        )
    }) {
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { BarraBuscar(buscar, { buscar = it }, "Buscar producto o marca") }
                item { PestanasPildora(listOf("Activos", "Agotados"), filtro, { filtro = it }) }
                items(lista, key = { it.id }) { p ->
                    Tarjeta(alPulsar = { onClickProducto(p.id) }) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            FotoProducto(p, Modifier.size(64.dp), 14.dp)
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(p.nombre, fontWeight = FontWeight.Bold)
                                Text("${p.tienda} · ${p.categoria.nombre} · ${p.pesoLb} lb", color = Muted, fontSize = 14.sp)
                                Text(
                                    "Tallas: ${p.tallas.joinToString()}" +
                                        if (p.codigos.size < p.tallas.size) " · ${p.tallas.size - p.codigos.size} sin código" else "",
                                    color = Muted, fontSize = 14.sp
                                )
                            }
                            Dinero(p.precioUsd, Moneda.USD, 15.sp)
                        }
                    }
                }
                if (lista.isEmpty()) item {
                    if (estado.cargado(Listas.PRODUCTOS)) EstadoVacio(Icons.Outlined.Checkroom, "No hay productos aquí") else Cargando()
                }
            }
            ExtendedFloatingActionButton(
                onClick = onClickNuevo,
                containerColor = Yellow, contentColor = Navy,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                icon = { Icon(Icons.Outlined.AddAPhoto, null) },
                text = { Text("Nuevo producto", fontWeight = FontWeight.Bold) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OpCatalogoScreenPreview() {
    VistaPrevia(Rol.OPERADOR) { OpCatalogoScreen(onClickProducto = {}, onClickNuevo = {}) }
}
