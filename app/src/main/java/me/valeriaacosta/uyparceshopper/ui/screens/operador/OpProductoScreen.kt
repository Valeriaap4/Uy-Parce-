package me.valeriaacosta.uyparceshopper.ui.screens.operador

import me.valeriaacosta.uyparceshopper.data.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.valeriaacosta.uyparceshopper.data.DatosEjemplo
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── OpProductoScreen ────────────────────────────────────────────────────────

/** Pantalla 19. Formulario validado para crear o editar un producto del catálogo. */
@Composable
fun OpProductoScreen(
    id: String?,
    onClickBack: () -> Unit,
    onGuardado: () -> Unit
) {
    val existente = DatosEjemplo.productos.firstOrNull { it.id == id }
    val mensaje = LocalMensaje.current
    var fotos by remember { mutableIntStateOf(if (existente != null) 2 else 0) }
    var nombre by remember { mutableStateOf(existente?.nombre ?: "") }
    var marca by remember { mutableStateOf(existente?.marca ?: "") }
    var descripcion by remember { mutableStateOf(existente?.descripcion ?: "") }
    var categoria by remember { mutableStateOf(existente?.categoria?.nombre) }
    var peso by remember { mutableStateOf(existente?.pesoLb?.toString() ?: "") }
    var precio by remember { mutableStateOf(existente?.precioUsd?.toString() ?: "") }
    var tienda by remember { mutableStateOf(existente?.tienda ?: "") }
    // Cada talla con su código de barras (puede quedar vacío y guardarse al comprar).
    val tallas = remember { mutableStateListOf<Pair<String, String>>().apply { existente?.tallas?.forEach { add(it to "") } } }
    var nuevaTalla by remember { mutableStateOf("") }
    var escanearTalla by remember { mutableStateOf<Int?>(null) }
    var agotar by remember { mutableStateOf(false) }
    var errores by remember { mutableStateOf(mapOf<String, String>()) }

    UpScreen(if (existente == null) "Nuevo producto" else "Editar producto", onClickBack, barraInferior = {
        BotonPrincipal("Guardar producto", {
            val e = mutableMapOf<String, String>()
            if (fotos == 0) e["fotos"] = "Agrega al menos una foto"
            if (nombre.length < 3) e["nombre"] = "Escribe el nombre del producto"
            if (marca.isBlank()) e["marca"] = "Escribe la marca"
            if (categoria == null) e["categoria"] = "Elige una categoría"
            if ((peso.toDoubleOrNull() ?: 0.0) <= 0) e["peso"] = "El peso debe ser mayor que 0"
            if ((precio.toDoubleOrNull() ?: 0.0) <= 0) e["precio"] = "Escribe el precio en USD"
            if (tienda.isBlank()) e["tienda"] = "¿En qué outlet está?"
            if (tallas.isEmpty()) e["tallas"] = "Agrega al menos una talla"
            errores = e
            if (e.isEmpty()) {
                mensaje("Producto guardado en el catálogo")
                onGuardado()
            }
        }, Modifier.fillMaxWidth(), Icons.Outlined.Check)
    }) {
        ContenidoScroll {
            // ── Fotos (cámara o galería en la siguiente fase) ──
            EtiquetaCampo("Fotos")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(fotos) {
                    Box(
                        modifier = Modifier.size(80.dp).clip(RoundedCornerShape(14.dp)).background(Color(existente?.colorFoto ?: 0xFF2B3A67)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Outlined.Image, null, tint = Color.White) }
                }
                if (fotos < 4) {
                    Column(
                        modifier = Modifier.size(80.dp).clip(RoundedCornerShape(14.dp)).background(Field).clickable { fotos++; errores = errores - "fotos" },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Outlined.AddAPhoto, null, tint = Blue)
                        Text("Agregar", color = Blue, fontSize = 12.sp)
                    }
                }
            }
            errores["fotos"]?.let { Text(it, color = Red) }

            // ── Datos ──
            CampoTexto("Nombre", nombre, { errores = errores - "nombre"; nombre = it }, placeholder = "Ej: Camiseta Nike Dri-FIT", error = errores["nombre"])
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CampoTexto("Marca", marca, { errores = errores - "marca"; marca = it }, Modifier.weight(1f), error = errores["marca"])
                CampoTexto("Tienda / outlet", tienda, { errores = errores - "tienda"; tienda = it }, Modifier.weight(1f), error = errores["tienda"])
            }
            CampoTexto("Descripción", descripcion, { descripcion = it }, lineas = 2, placeholder = "Color, material, detalles")
            Selector("Categoría", DatosEjemplo.categorias.map { it.nombre }, categoria, { c -> errores = errores - "categoria"; 
                categoria = c
                peso = DatosEjemplo.categorias.first { it.nombre == c }.pesoLb.toString()
            }, error = errores["categoria"])
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CampoTexto("Peso (lb)", peso, { errores = errores - "peso"; peso = it }, Modifier.weight(1f), teclado = KeyboardType.Decimal, error = errores["peso"])
                CampoTexto("Precio (USD)", precio, { errores = errores - "precio"; precio = it }, Modifier.weight(1f), teclado = KeyboardType.Decimal,
                    placeholder = "0.00", error = errores["precio"])
            }
            Text("El peso se llena según la categoría. Ajústalo si la prenda es más pesada.", color = Muted, fontSize = 13.sp)

            // ── Tallas y códigos de barras ──
            EtiquetaCampo("Tallas y códigos de barras")
            tallas.forEachIndexed { i, (t, codigo) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .border(1.dp, Line, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(t, fontWeight = FontWeight.Bold, modifier = Modifier.width(56.dp))
                    Text(
                        codigo.ifBlank { "Sin código (se guarda al comprar)" },
                        fontFamily = if (codigo.isBlank()) null else Mono,
                        color = if (codigo.isBlank()) Muted else Ink, fontSize = 14.sp, modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { escanearTalla = i }) { Icon(Icons.Outlined.QrCodeScanner, "Escanear", tint = Blue) }
                    IconButton(onClick = { tallas.removeAt(i) }) { Icon(Icons.Outlined.Close, "Quitar", tint = Muted) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                CampoTexto("", nuevaTalla, { errores = errores - "tallas"; nuevaTalla = it.uppercase().take(6) }, Modifier.weight(1f),
                    placeholder = "Nueva talla (S, M, 32…)", error = errores["tallas"])
                BotonSecundario("Agregar", {
                    if (nuevaTalla.isNotBlank() && tallas.none { it.first == nuevaTalla }) {
                        tallas += nuevaTalla to ""
                        errores = errores - "tallas"
                        nuevaTalla = ""
                    }
                })
            }
            if (existente != null && !existente.agotado)
                BotonSecundario("Marcar agotado", { agotar = true }, Modifier.fillMaxWidth(), Icons.Outlined.RemoveShoppingCart, Red)
        }
    }

    escanearTalla?.let { i ->
        DialogoEscaner(
            alLeer = { c ->
                // Si el código ya existe en otro producto, se avisa en vez de duplicarlo.
                if (c == "0888407612345" && existente?.id != "p1") mensaje("Este código ya está en el catálogo: Camiseta Nike Dri-FIT")
                else tallas[i] = tallas[i].first to c
                escanearTalla = null
            },
            alCerrar = { escanearTalla = null },
        )
    }
    if (agotar) DialogoConfirmar(
        "Marcar agotado", "El producto deja de mostrarse y avisamos a los clientes que lo tienen en el carrito.",
        "Marcar agotado", { mensaje("Producto marcado como agotado"); onGuardado() }, { agotar = false }, peligro = true
    )
}

@Preview(showBackground = true)
@Composable
fun OpProductoScreenPreview() {
    UyParceTheme { OpProductoScreen(id = null, onClickBack = {}, onGuardado = {}) }
}
