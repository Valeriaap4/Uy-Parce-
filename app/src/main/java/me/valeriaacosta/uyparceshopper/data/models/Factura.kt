package me.valeriaacosta.uyparceshopper.data.models

import com.google.firebase.Timestamp

/**
 * Factura que se genera al aprobar el pago. Guarda una copia congelada del pedido de ese
 * momento, así no cambia aunque después cambien precios o estados. Se guarda en facturas/{numero}.
 */
data class Factura(
    /** "FAC-0001" */
    val numero: String = "",
    val numeroPedido: Int = 0,
    val clienteId: String = "",
    val pedido: Pedido = Pedido(),
    val fecha: Timestamp? = null
)
