package me.valeriaacosta.uyparceshopper.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

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

        /** Pedidos en los que todavía hay trabajo: el equipo los escucha en vivo. */
        val activos = listOf(PENDIENTE_PAGO, PAGO_EN_REVISION, PAGADO, COMPRADO, EN_CAMINO, EN_CUCUTA, ENVIADO)
    }
}

/**
 * Una línea del pedido: un producto en una talla. Guarda una copia del producto y el precio por
 * unidad congelados al confirmar, y cuántas unidades van en cada paso del proceso.
 */
data class LineaPedido(
    val producto: Producto = Producto(),
    val talla: String = "",
    val cantidad: Int = 0,
    /** Precio por unidad que pagó el cliente (USD si es mayorista, COP si es detal). */
    val precioUnitario: Double = 0.0,
    val compradas: Int = 0,
    val noConseguidas: Int = 0,
    val empacadas: Int = 0,
    val recibidas: Int = 0
) {
    /** Unidades que el operador todavía debe buscar en el outlet. */
    @get:Exclude
    val porComprar: Int get() = (cantidad - compradas - noConseguidas).coerceAtLeast(0)

    /** Unidades compradas que todavía no van en una caja. */
    @get:Exclude
    val porEmpacar: Int get() = (compradas - empacadas).coerceAtLeast(0)

    /** Producto + talla. El separador evita confundir "p1" + "10" con "p11" + "0". */
    @get:Exclude
    val clave: String get() = "${producto.id}|$talla"
}

/** Pedido de un cliente. Se guarda en pedidos/{numero}. */
data class Pedido(
    val numero: Int = 0,
    val clienteId: String = "",
    /** Copia de los datos del cliente al confirmar (la dirección de entrega queda congelada). */
    val cliente: Usuario = Usuario(),
    /** Copia de la zona de envío nacional con el precio de ese momento. */
    val zona: Zona = Zona(),
    val lineas: List<LineaPedido> = emptyList(),
    /** Lo que cuesta el pedido, congelado al confirmar. */
    val desglose: Desglose = Desglose(),
    /** Saldo a favor que el cliente usó para pagar este pedido. */
    val saldoAplicado: Double = 0.0,
    val estado: EstadoPedido = EstadoPedido.PENDIENTE_PAGO,
    val fecha: Timestamp? = null,
    /** Hasta cuándo puede pagar (se renueva si le rechazan el comprobante). */
    val venceEn: Timestamp? = null,
    val pagadoEn: Timestamp? = null,
    val despachadoEn: Timestamp? = null,
    val entregadoEn: Timestamp? = null,
    val canceladoPor: String? = null,
    val motivoCancelacion: String? = null,
    val motivoRechazo: String? = null,
    val transportadora: String? = null,
    val guia: String? = null,
    /** Códigos de las cajas donde van prendas de este pedido. */
    val cajas: List<String> = emptyList(),
    val faltantes: List<Faltante> = emptyList(),
    /** Lo que el cliente eligió para cada faltante: id del faltante → resolución. */
    val resoluciones: Map<String, Resolucion> = emptyMap(),
    /** true si usó saldo a favor o tiene faltantes: así Cartera lo encuentra con una consulta. */
    val conSaldo: Boolean = false
) {
    @get:Exclude
    val prendas: Int get() = lineas.sumOf { it.cantidad }

    @get:Exclude
    val recibidas: Int get() = lineas.sumOf { it.recibidas }

    @get:Exclude
    val moneda: Moneda get() = desglose.moneda

    /** Lo que falta por pagar después de usar el saldo a favor. */
    @get:Exclude
    val totalAPagar: Double
        get() = Precios.redondear((desglose.total - saldoAplicado).coerceAtLeast(0.0), desglose.moneda)

    @get:Exclude
    val faltantesSinResolver: List<Faltante> get() = faltantes.filter { it.id !in resoluciones }

    /** true si sigue sin pagar y ya pasó el plazo. */
    fun vencido(ahora: Timestamp = Timestamp.now()): Boolean =
        estado == EstadoPedido.PENDIENTE_PAGO && venceEn != null && venceEn < ahora

    /**
     * Siguiente estado según cómo van las prendas. La usan todas las acciones que tocan el pedido
     * (compra, empaque, sellado y recepción) para que ninguno quede atascado.
     * cajasDelPedido: las cajas donde hay prendas de este pedido, ya actualizadas.
     */
    fun avance(cajasDelPedido: List<Caja>): EstadoPedido {
        var actual = estado
        repeat(4) {
            val siguiente = siguienteEstado(actual, cajasDelPedido)
            if (siguiente == actual) return actual
            actual = siguiente
        }
        return actual
    }

    private fun siguienteEstado(estado: EstadoPedido, cajasDelPedido: List<Caja>): EstadoPedido {
        // Si falta información de alguna caja del pedido, no se avanza.
        val cajasCompletas = cajas.all { codigo -> cajasDelPedido.any { it.codigo == codigo } }
        val misCajas = cajasDelPedido.filter { it.codigo in cajas }
        return when (estado) {
            EstadoPedido.PAGADO -> when {
                lineas.any { it.porComprar > 0 } -> EstadoPedido.PAGADO
                lineas.sumOf { it.compradas } == 0 -> EstadoPedido.CANCELADO // no se consiguió nada
                else -> EstadoPedido.COMPRADO
            }
            EstadoPedido.COMPRADO ->
                if (cajasCompletas && misCajas.isNotEmpty() && lineas.all { it.porEmpacar == 0 } &&
                    misCajas.all { it.estado != EstadoCaja.ABIERTA }
                ) EstadoPedido.EN_CAMINO else EstadoPedido.COMPRADO
            EstadoPedido.EN_CAMINO ->
                if (cajasCompletas && misCajas.isNotEmpty() && misCajas.all { it.estado == EstadoCaja.RECIBIDA })
                    EstadoPedido.EN_CUCUTA else EstadoPedido.EN_CAMINO
            else -> estado
        }
    }

    companion object {
        val transportadoras = listOf("Servientrega", "Interrapidísimo", "Coordinadora", "Envía")
    }
}
