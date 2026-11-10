package me.valeriaacosta.uyparceshopper.data

import me.valeriaacosta.uyparceshopper.data.models.Producto

/**
 * Normaliza un código de barras: sin espacios ni guiones y, si es numérico, sin ceros a la
 * izquierda. Así el UPC "012345678905" y el EAN "0012345678905" de la misma prenda quedan iguales.
 */
fun normalizarCodigo(texto: String): String {
    val limpio = texto.trim().filter { it.isLetterOrDigit() }.uppercase()
    return if (limpio.isNotEmpty() && limpio.all { it.isDigit() }) limpio.trimStart('0').ifEmpty { "0" } else limpio
}

/** Producto y talla de un código escaneado (o null si ningún producto lo tiene guardado). */
data class ProductoTalla(val producto: Producto, val talla: String)

fun buscarPorCodigo(productos: List<Producto>, codigo: String): ProductoTalla? {
    val normal = normalizarCodigo(codigo)
    if (normal.isEmpty()) return null
    for (p in productos) {
        val talla = p.tallaDelCodigo(normal)
        if (talla != null) return ProductoTalla(p, talla)
    }
    return null
}
