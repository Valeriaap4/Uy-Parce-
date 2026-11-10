package me.valeriaacosta.uyparceshopper.data.models

import com.google.firebase.Timestamp

enum class EstadoPago(val etiqueta: String) { EN_REVISION("En revisión"), APROBADO("Aprobado"), RECHAZADO("Rechazado") }

/** Pago que reporta el cliente con la foto del comprobante. Se guarda en pagos/{id}. */
data class Pago(
    val id: String = "",
    val numeroPedido: Int = 0,
    val clienteId: String = "",
    val clienteNombre: String = "",
    /** Lo que debía pagar: el total del pedido menos el saldo a favor usado. */
    val monto: Double = 0.0,
    val moneda: Moneda = Moneda.COP,
    val medio: String = "",
    val referencia: String = "",
    /** Fecha en que el cliente dice que pagó, tal como la escribió (dd/mm/aaaa). */
    val fechaPago: String = "",
    /** Id de la foto del comprobante en la colección "fotos" (null si el saldo cubrió todo). */
    val fotoId: String? = null,
    val estado: EstadoPago = EstadoPago.EN_REVISION,
    val motivoRechazo: String? = null,
    val revisadoPor: String? = null,
    val revisadoEn: Timestamp? = null,
    /** Cuándo se reportó. */
    val fecha: Timestamp? = null
)
