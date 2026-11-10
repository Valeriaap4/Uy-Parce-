package me.valeriaacosta.uyparceshopper.data.models

enum class EstadoPedido(val etiqueta: String) {
    PENDIENTE_PAGO("Pendiente de pago"),
    PAGO_EN_REVISION("Pago en revisión"),
    PAGADO("Pagado"),
    COMPRADO("Comprado"),
    EN_CAMINO("En camino"),
    EN_CUCUTA("En Cúcuta"),
    ENVIADO("Enviado"),
    ENTREGADO("Entregado"),
    CANCELADO("Cancelado");

    companion object {
        /** Orden de la línea de tiempo que ve el cliente (sin Cancelado). */
        val lineaDeTiempo = entries.filter { it != CANCELADO }
    }
}

data class LineaPedido(
    val producto: Producto,
    val talla: String,
    val cantidad: Int,
    val compradas: Int = 0,
    val recibidas: Int = 0
)

data class Pedido(
    val numero: Int,
    val cliente: Usuario,
    val lineas: List<LineaPedido>,
    val estado: EstadoPedido,
    val fecha: String,
    val zona: Zona,
    val transportadora: String? = null,
    val guia: String? = null,
    val motivoRechazo: String? = null
) {
    val prendas: Int get() = lineas.sumOf { it.cantidad }
    val recibidas: Int get() = lineas.sumOf { it.recibidas }
}
