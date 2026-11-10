package me.valeriaacosta.uyparceshopper.data.models

enum class Moneda { USD, COP }

enum class TipoCliente(val etiqueta: String, val comision: Double, val moneda: Moneda) {
    MAYORISTA_AVANZADO("Mayorista Avanzado", 0.20, Moneda.USD),
    MAYORISTA_PARCERO("Mayorista Parcero", 0.25, Moneda.USD),
    DETAL("Detal", 0.0, Moneda.COP);

    val esMayorista: Boolean get() = this != DETAL
}
