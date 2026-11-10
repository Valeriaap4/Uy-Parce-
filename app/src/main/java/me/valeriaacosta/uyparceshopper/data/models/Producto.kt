package me.valeriaacosta.uyparceshopper.data.models

import com.google.firebase.Timestamp

/** Categoría de producto con su peso estimado (sirve para calcular el envío internacional). */
data class Categoria(val nombre: String = "", val pesoLb: Double = 0.0) {
    companion object {
        /** Categorías con las que arranca la tienda; el admin ajusta los pesos en Ajustes. */
        val porDefecto = listOf(
            Categoria("Camiseta", 0.5), Categoria("Camisa", 0.7), Categoria("Jean", 1.5),
            Categoria("Chaqueta", 3.0), Categoria("Zapatos", 3.0), Categoria("Sandalias", 1.2),
            Categoria("Bolso", 1.8), Categoria("Accesorio", 0.3)
        )
    }
}

/** Producto del catálogo. Se guarda en productos/{id}. */
data class Producto(
    val id: String = "",
    val nombre: String = "",
    val marca: String = "",
    val descripcion: String = "",
    val categoria: Categoria = Categoria(),
    val pesoLb: Double = 0.0,
    val precioUsd: Double = 0.0,
    val tienda: String = "",
    val tallas: List<String> = emptyList(),
    val agotado: Boolean = false,
    /** Ids de las fotos guardadas en la colección "fotos" (máximo 4). */
    val fotos: List<String> = emptyList(),
    /** Código de barras de fábrica de cada talla: talla → código (ya normalizado). */
    val codigos: Map<String, String> = emptyMap(),
    /** Color de respaldo mientras el producto no tiene fotos. */
    val colorFoto: Long = 0xFFDDE3F0,
    val creadoPor: String = "",
    val creadoEn: Timestamp? = null
) {
    /** Talla a la que pertenece un código de barras, o null si no es de este producto. */
    fun tallaDelCodigo(codigo: String): String? = codigos.entries.firstOrNull { it.value == codigo }?.key
}
