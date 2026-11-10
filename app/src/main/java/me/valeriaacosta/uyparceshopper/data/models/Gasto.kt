package me.valeriaacosta.uyparceshopper.data.models

data class Gasto(
    val id: String,
    val categoria: String,
    val descripcion: String,
    val monto: Double,
    val moneda: Moneda,
    val proveedor: String,
    val fecha: String
)
