package me.valeriaacosta.uyparceshopper.data.models

import com.google.firebase.Timestamp

/**
 * Aviso para un cliente (se ve en Notificaciones). Si trae número de pedido, al tocarlo se abre
 * ese pedido; si no (por ejemplo "producto agotado"), se abre el carrito.
 */
data class Notificacion(
    val id: String = "",
    /** uid del cliente que lo recibe. */
    val para: String = "",
    val titulo: String = "",
    val texto: String = "",
    val fecha: Timestamp? = null,
    val leida: Boolean = false,
    val numeroPedido: Int? = null
)
