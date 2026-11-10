package me.valeriaacosta.uyparceshopper.data.models

data class Categoria(val nombre: String, val pesoLb: Double)

data class Producto(
    val id: String,
    val nombre: String,
    val marca: String,
    val descripcion: String,
    val categoria: Categoria,
    val pesoLb: Double,
    val precioUsd: Double,
    val tienda: String,
    val tallas: List<String>,
    val agotado: Boolean = false,
    /** Color de la foto de ejemplo mientras no hay fotos reales. */
    val colorFoto: Long = 0xFFDDE3F0
)
