package me.valeriaacosta.uyparceshopper.data.models

/** Zona de envío nacional (Cúcuta → ciudad del cliente). */
data class Zona(val nombre: String = "", val precioCop: Int = 0, val ciudades: List<String> = emptyList())

/**
 * Las 3 zonas fijas. Las ciudades viven en el código porque el registro las necesita antes de
 * iniciar sesión; el precio de cada zona lo pone el admin (Ajustes.preciosZona).
 */
object Zonas {
    val lista = listOf(
        Zona("Cúcuta y área metropolitana", 8000, listOf("Cúcuta", "Villa del Rosario", "Los Patios", "El Zulia")),
        Zona("Ciudades principales", 15000, listOf("Bogotá", "Medellín", "Cali", "Barranquilla", "Bucaramanga", "Cartagena")),
        Zona("Resto del país", 22000, listOf("Pamplona", "Ocaña", "Tunja", "Pasto", "Montería", "Otra ciudad"))
    )

    val ciudades: List<String> = lista.flatMap { it.ciudades }

    /** Nombre de la zona de una ciudad (las que no están en la lista van a "Resto del país"). */
    fun nombreDe(ciudad: String): String = (lista.firstOrNull { ciudad in it.ciudades } ?: lista.last()).nombre
}
