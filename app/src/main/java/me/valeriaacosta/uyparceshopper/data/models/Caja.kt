package me.valeriaacosta.uyparceshopper.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

enum class EstadoCaja(val etiqueta: String) {
    ABIERTA("Abierta"),
    ENVIADA("Enviada"),
    EN_CUCUTA("En Cúcuta"),
    /** La recepción en Cúcuta ya se cerró. */
    RECIBIDA("Recibida")
}

enum class TipoCaja(val etiqueta: String) { MAYORISTA("Mayorista"), DETAL("Detal") }

/** Tipo de envío del courier. La tarifa por libra de cada uno la define el admin en Ajustes. */
enum class TipoEnvio(val etiqueta: String) {
    REGULAR("Regular"), PRIME_300("Prime 300"), PRIME_800("Prime 800")
}

/** Una prenda escaneada dentro de una caja y el pedido al que se asignó. */
data class PrendaCaja(
    val id: String = "",
    val numeroPedido: Int = 0,
    val productoId: String = "",
    val nombre: String = "",
    val talla: String = "",
    val recibida: Boolean = false
)

/**
 * Caja que viaja de New Jersey a Cúcuta. Se guarda en cajas/{codigo}.
 * Mayorista: una caja por cliente. Detal: prendas de varios pedidos que se separan en Cúcuta.
 */
data class Caja(
    val codigo: String = "",
    val tipo: TipoCaja = TipoCaja.DETAL,
    val clienteId: String? = null,
    val clienteNombre: String? = null,
    val numerosPedido: List<Int> = emptyList(),
    val contenido: List<PrendaCaja> = emptyList(),
    val estado: EstadoCaja = EstadoCaja.ABIERTA,
    val pesoLb: Double? = null,
    val tipoEnvio: TipoEnvio? = null,
    /** Lo que cobra el courier por la caja (USD): peso × tarifa + seguro. */
    val costoEnvio: Double? = null,
    val creadaPor: String = "",
    val creadaEn: Timestamp? = null,
    val fechaEnvio: Timestamp? = null,
    val fechaRecepcion: Timestamp? = null
) {
    @get:Exclude
    val prendas: Int get() = contenido.size

    @get:Exclude
    val recibidas: Int get() = contenido.count { it.recibida }
}
