package me.valeriaacosta.uyparceshopper.data.models

/** Una línea del carrito guardado: qué producto, en qué talla y cuántas unidades. */
data class ItemCarrito(
    val productoId: String = "",
    val talla: String = "",
    val cantidad: Int = 1
)

/**
 * Carrito del cliente, guardado en carritos/{uid}. No guarda precios: se calculan con el
 * catálogo del momento (así se ve en vivo si algo se agota o cambia de precio).
 * productoIds sirve para encontrar los carritos que tienen un producto que se agotó.
 */
data class Carrito(
    val items: List<ItemCarrito> = emptyList(),
    val productoIds: List<String> = emptyList()
)
