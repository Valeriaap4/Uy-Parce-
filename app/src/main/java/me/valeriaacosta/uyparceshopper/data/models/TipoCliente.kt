package me.valeriaacosta.uyparceshopper.data.models

enum class Moneda { USD, COP }

/**
 * Tipo de cliente. La comisión de cada tipo NO va aquí: la define el admin en Ajustes
 * (ver Precios.comisionDe) para que haya un solo lugar donde cambiarla.
 */
enum class TipoCliente(val etiqueta: String, val moneda: Moneda) {
    MAYORISTA_AVANZADO("Mayorista Avanzado", Moneda.USD),
    MAYORISTA_PARCERO("Mayorista Parcero", Moneda.USD),
    DETAL("Detal", Moneda.COP);

    val esMayorista: Boolean get() = this != DETAL
}
