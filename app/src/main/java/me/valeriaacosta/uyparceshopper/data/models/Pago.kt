package me.valeriaacosta.uyparceshopper.data.models

enum class EstadoPago(val etiqueta: String) { EN_REVISION("En revisión"), APROBADO("Aprobado"), RECHAZADO("Rechazado") }

data class Pago(
    val id: String,
    val pedido: Pedido,
    val medio: String,
    val referencia: String,
    val fecha: String,
    val estado: EstadoPago
)
