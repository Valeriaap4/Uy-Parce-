package me.valeriaacosta.uyparceshopper.data.models

enum class EstadoCaja(val etiqueta: String) { ABIERTA("Abierta"), ENVIADA("Enviada"), EN_CUCUTA("En Cúcuta") }

enum class TipoCaja(val etiqueta: String) { MAYORISTA("Mayorista"), DETAL("Detal") }

enum class TipoEnvio(val etiqueta: String, val usdPorLibra: Double) {
    REGULAR("Regular", 3.3), PRIME_300("Prime 300", 3.8), PRIME_800("Prime 800", 5.0)
}

data class Caja(
    val codigo: String,
    val tipo: TipoCaja,
    val cliente: Usuario?,
    val pedidos: List<Pedido>,
    val prendas: Int,
    val estado: EstadoCaja,
    val pesoLb: Double? = null,
    val tipoEnvio: TipoEnvio? = null,
    val fechaEnvio: String? = null
)
