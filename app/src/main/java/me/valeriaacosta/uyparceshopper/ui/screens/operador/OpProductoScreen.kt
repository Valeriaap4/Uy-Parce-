package me.valeriaacosta.uyparceshopper.ui.screens.operador

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.firebase.FotoNueva
import me.valeriaacosta.uyparceshopper.data.mensaje
import me.valeriaacosta.uyparceshopper.data.models.*
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*
import java.util.UUID
import kotlin.math.absoluteValue

// ─── OpProductoScreen ────────────────────────────────────────────────────────

/** Colores de respaldo para productos sin foto (se elige uno según el id). */
private val coloresRespaldo = listOf(0xFF2B3A67, 0xFF3F6E5A, 0xFF2F4E7A, 0xFF8A2E2E, 0xFF8C6A3F, 0xFF7A808C, 0xFF243B6B, 0xFF5B3F8C)

/**
 * Pantalla 19. Formulario validado para crear o editar un producto: hasta 4 fotos (cámara o
 * galería), datos, tallas con su código de barras (escáner), agotado/disponible y eliminar.
 */
@Composable
fun OpProductoScreen(
    id: String?,
    onClickBack: () -> Unit,
    onGuardado: () -> Unit
) {
    // Fotos nuevas ("id|ruta") y la cámara van aquí arriba: así no se pierden si Android cierra la app.
    var fotosNuevas by rememberSaveable { mutableStateOf(listOf<String>()) }
    val selector = rememberSelectorFoto { ruta -> fotosNuevas = fotosNuevas + "${UUID.randomUUID()}|$ruta" }

    if (id == null) FormularioProducto(null, fotosNuevas, { fotosNuevas = it }, selector, onClickBack, onGuardado)
    else SegunCarga(rememberProducto(id), "Editar producto", onClickBack) { p ->
        FormularioProducto(p, fotosNuevas, { fotosNuevas = it }, selector, onClickBack, onGuardado)
    }
}

@Composable
private fun FormularioProducto(
    existente: Producto?,
    fotosNuevas: List<String>,
    cambiarFotosNuevas: (List<String>) -> Unit,
    selector: SelectorFoto,
    onClickBack: () -> Unit,
    onGuardado: () -> Unit
) {
    val estado = LocalEstadoApp.current
    val mensaje = LocalMensaje.current
    val scope = rememberCoroutineScope()
    val clave = existente?.id ?: "nuevo"
    val idProducto = existente?.id ?: rememberSaveable { UUID.randomUUID().toString() }
    var nombre by rememberSaveable(clave) { mutableStateOf(existente?.nombre ?: "") }
    var marca by rememberSaveable(clave) { mutableStateOf(existente?.marca ?: "") }
    var descripcion by rememberSaveable(clave) { mutableStateOf(existente?.descripcion ?: "") }
    var categoria by rememberSaveable(clave) { mutableStateOf(existente?.categoria?.nombre) }
    var peso by rememberSaveable(clave) { mutableStateOf(existente?.pesoLb?.toString() ?: "") }
    var precio by rememberSaveable(clave) { mutableStateOf(existente?.precioUsd?.toString() ?: "") }
    var tienda by rememberSaveable(clave) { mutableStateOf(existente?.tienda ?: "") }
    var fotosGuardadas by rememberSaveable(clave) { mutableStateOf(existente?.fotos ?: emptyList()) }
    // Cada talla con su código de barras (puede quedar vacío y guardarse al registrar la compra).
    val tallas = remember(clave) {
        mutableStateListOf<Pair<String, String>>().apply { existente?.tallas?.forEach { add(it to (existente.codigos[it] ?: "")) } }
    }
    var nuevaTalla by remember { mutableStateOf("") }
    var escanearTalla by remember { mutableStateOf<Int?>(null) }
    var confirmar by remember { mutableStateOf<String?>(null) } // "agotar", "disponible" o "eliminar"
    var errores by remember { mutableStateOf(mapOf<String, String>()) }
    var trabajando by remember { mutableStateOf(false) }
    val totalFotos = fotosGuardadas.size + fotosNuevas.size

    fun accion(bloque: suspend () -> Result<String>) {
        trabajando = true
        scope.launch {
            bloque().onSuccess { mensaje(it); onGuardado() }.onFailure { mensaje(it.mensaje()) }
            trabajando = false
        }
    }

    UpScreen(if (existente == null) "Nuevo producto" else "Editar producto", onClickBack, barraInferior = {
        BotonPrincipal("Guardar producto", {
            val e = mutableMapOf<String, String>()
            val pesoNum = peso.replace(",", ".").toDoubleOrNull() ?: 0.0
            val precioNum = precio.replace(",", ".").toDoubleOrNull() ?: 0.0
            if (totalFotos == 0) e["fotos"] = "Agrega al menos una foto"
            if (nombre.trim().length < 3) e["nombre"] = "Escribe el nombre del producto"
            if (marca.isBlank()) e["marca"] = "Escribe la marca"
            if (categoria == null) e["categoria"] = "Elige una categoría"
            if (pesoNum <= 0 || pesoNum > 50) e["peso"] = "El peso debe estar entre 0 y 50 lb"
            if (precioNum <= 0 || precioNum > 5000) e["precio"] = "Escribe el precio en USD"
            if (tienda.isBlank()) e["tienda"] = "¿En qué outlet está?"
            if (tallas.isEmpty()) e["tallas"] = "Agrega al menos una talla"
            errores = e
            val nombreCategoria = categoria
            if (e.isEmpty() && nombreCategoria != null) {
                val cat = estado.ajustes.categorias.firstOrNull { it.nombre == nombreCategoria } ?: Categoria(nombreCategoria, pesoNum)
                val nuevas = fotosNuevas.map { FotoNueva(it.substringBefore("|"), it.substringAfter("|")) }
                val producto = Producto(
                    id = idProducto, nombre = nombre.trim(), marca = marca.trim(), descripcion = descripcion.trim(),
                    categoria = cat, pesoLb = pesoNum, precioUsd = Precios.redondear(precioNum, Moneda.USD), tienda = tienda.trim(),
                    tallas = tallas.map { it.first }, agotado = existente?.agotado ?: false,
                    fotos = fotosGuardadas + nuevas.map { it.id },
                    codigos = tallas.filter { it.second.isNotBlank() }.associate { it.first to it.second },
                    colorFoto = existente?.colorFoto ?: coloresRespaldo[idProducto.hashCode().absoluteValue % coloresRespaldo.size]
                )
                val quitadas = existente?.fotos.orEmpty().filter { it !in fotosGuardadas }
                accion {
                    estado.guardarProducto(producto, existente == null, nuevas, quitadas)
                        .map { cambiarFotosNuevas(emptyList()); "Producto guardado en el catálogo" }
                }
            }
        }, Modifier.fillMaxWidth(), Icons.Outlined.Check, cargando = trabajando)
    }) {
        ContenidoScroll {
            // ── Fotos ──
            EtiquetaCampo("Fotos (máximo 4)")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                fotosGuardadas.forEach { fotoId ->
                    MiniaturaConQuitar({ fotosGuardadas = fotosGuardadas - fotoId }) {
                        FotoGuardada(fotoId, Modifier.fillMaxSize())
                    }
                }
                fotosNuevas.forEach { f ->
                    MiniaturaConQuitar({ cambiarFotosNuevas(fotosNuevas - f) }) {
                        FotoLocal(f.substringAfter("|"), Modifier.fillMaxSize())
                    }
                }
                if (totalFotos < 4) {
                    BotonFoto(Icons.Outlined.PhotoCamera, "Cámara", !selector.procesando) { errores = errores - "fotos"; selector.tomarFoto() }
                    BotonFoto(Icons.Outlined.PhotoLibrary, "Galería", !selector.procesando) { errores = errores - "fotos"; selector.elegirDeGaleria() }
                }
            }
            if (selector.procesando) Text("Preparando la foto…", color = Muted, fontSize = 13.sp)
            errores["fotos"]?.let { Text(it, color = Red) }

            // ── Datos ──
            CampoTexto("Nombre", nombre, { errores = errores - "nombre"; nombre = it }, placeholder = "Ej: Camiseta Nike Dri-FIT", error = errores["nombre"])
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CampoTexto("Marca", marca, { errores = errores - "marca"; marca = it }, Modifier.weight(1f), error = errores["marca"])
                CampoTexto("Tienda / outlet", tienda, { errores = errores - "tienda"; tienda = it }, Modifier.weight(1f), error = errores["tienda"])
            }
            CampoTexto("Descripción", descripcion, { descripcion = it }, lineas = 2, placeholder = "Color, material, detalles")
            Selector("Categoría", estado.ajustes.categorias.map { it.nombre }, categoria, { c ->
                errores = errores - "categoria" - "peso"
                categoria = c
                estado.ajustes.categorias.firstOrNull { it.nombre == c }?.let { peso = it.pesoLb.toString() }
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
                CampoTexto("", nuevaTalla, { errores = errores - "tallas"; nuevaTalla = it.uppercase().take(8) }, Modifier.weight(1f),
                    placeholder = "Nueva talla (S, M, 32…)", error = errores["tallas"])
                BotonSecundario("Agregar", {
                    val t = nuevaTalla.trim()
                    when {
                        t.isBlank() -> Unit
                        tallas.any { it.first == t } -> mensaje("La talla $t ya está")
                        else -> {
                            tallas += t to ""
                            errores = errores - "tallas"
                            nuevaTalla = ""
                        }
                    }
                })
            }

            // ── Agotado, disponible y eliminar (solo al editar) ──
            if (existente != null) {
                if (existente.agotado) {
                    Aviso("Este producto está agotado: los clientes no lo pueden pedir.", TonoChip.ROJO, Icons.Outlined.RemoveShoppingCart)
                    BotonSecundario("Marcar disponible", { confirmar = "disponible" }, Modifier.fillMaxWidth(), Icons.Outlined.AddShoppingCart, habilitado = !trabajando)
                } else {
                    BotonSecundario("Marcar agotado", { confirmar = "agotar" }, Modifier.fillMaxWidth(), Icons.Outlined.RemoveShoppingCart, Red, habilitado = !trabajando)
                }
                BotonSecundario("Eliminar producto", { confirmar = "eliminar" }, Modifier.fillMaxWidth(), Icons.Outlined.Delete, Red, habilitado = !trabajando)
            }
        }
    }

    escanearTalla?.let { i ->
        DialogoEscaner(
            titulo = "Código de la talla ${tallas.getOrNull(i)?.first ?: ""}",
            alLeer = { c ->
                // El mismo código no puede ser de dos productos ni de dos tallas.
                val otro = estado.productos.firstOrNull { it.id != idProducto && it.tallaDelCodigo(c) != null }
                val otraTalla = tallas.withIndex().firstOrNull { (j, par) -> j != i && par.second == c }
                when {
                    otro != null -> mensaje("Ese código ya es de ${otro.nombre} (talla ${otro.tallaDelCodigo(c)})")
                    otraTalla != null -> mensaje("Ese código ya es de la talla ${otraTalla.value.first}")
                    i < tallas.size -> tallas[i] = tallas[i].first to c
                }
                escanearTalla = null
            },
            alCerrar = { escanearTalla = null }
        )
    }
    if (existente != null) when (confirmar) {
        "agotar" -> DialogoConfirmar(
            "Marcar agotado", "Los clientes ya no lo podrán pedir y avisamos a quienes lo tienen en el carrito.",
            "Marcar agotado", {
                accion { estado.marcarAgotado(existente).map { n -> if (n > 0) "Marcado agotado · avisamos a ${cantidad(n, "cliente")}" else "Marcado agotado" } }
            }, { confirmar = null }, peligro = true
        )
        "disponible" -> DialogoConfirmar(
            "Marcar disponible", "Los clientes lo podrán pedir otra vez.", "Marcar disponible",
            { accion { estado.marcarDisponible(existente).map { "Producto disponible otra vez" } } }, { confirmar = null }
        )
        "eliminar" -> DialogoConfirmar(
            "Eliminar producto", "${existente.nombre} sale del catálogo y queda en la papelera (el admin lo puede restaurar).",
            "Eliminar", { accion { estado.eliminarProducto(existente).map { "Producto enviado a la papelera" } } },
            { confirmar = null }, peligro = true
        )
    }
}

/** Miniatura de 80 dp con una X para quitarla. */
@Composable
private fun MiniaturaConQuitar(alQuitar: () -> Unit, foto: @Composable () -> Unit) {
    Box(Modifier.size(80.dp).clip(RoundedCornerShape(14.dp)).background(Field)) {
        foto()
        Box(
            Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).clip(CircleShape).background(Navy.copy(alpha = 0.8f))
                .clickable(onClick = alQuitar),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Outlined.Close, "Quitar foto", tint = Color.White, modifier = Modifier.size(16.dp)) }
    }
}

/** Cuadro para agregar una foto con la cámara o la galería. */
@Composable
private fun BotonFoto(icono: androidx.compose.ui.graphics.vector.ImageVector, texto: String, habilitado: Boolean, alPulsar: () -> Unit) {
    Column(
        modifier = Modifier.size(80.dp).clip(RoundedCornerShape(14.dp)).background(Field)
            .clickable(enabled = habilitado, onClick = alPulsar),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icono, null, tint = if (habilitado) Blue else Muted)
        Text(texto, color = if (habilitado) Blue else Muted, fontSize = 12.sp)
    }
}

@Preview(showBackground = true)
@Composable
fun OpProductoScreenPreview() {
    VistaPrevia(Rol.OPERADOR) { OpProductoScreen(id = null, onClickBack = {}, onGuardado = {}) }
}
