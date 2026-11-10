package me.valeriaacosta.uyparceshopper.data.models

/**
 * Aviso al cliente. Si trae número de pedido, al tocarlo se abre ese pedido;
 * si no (por ejemplo "producto agotado"), se abre el carrito.
 */
data class Notificacion(
    val titulo: String,
    val texto: String,
    val hace: String,
    val leida: Boolean = false,
    val numeroPedido: Int? = null
)
