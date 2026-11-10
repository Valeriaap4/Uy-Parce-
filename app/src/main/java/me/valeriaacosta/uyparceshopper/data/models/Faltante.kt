package me.valeriaacosta.uyparceshopper.data.models

import com.google.firebase.Timestamp

/**
 * Prendas que el cliente pagó y no va a recibir (no se consiguieron en el outlet o no llegaron
 * a Cúcuta). Lo registra el equipo con su valor; el cliente decide qué hacer con ese dinero.
 */
data class Faltante(
    val id: String = "",
    val unidades: Int = 0,
    val valor: Double = 0.0,
    val moneda: Moneda = Moneda.COP,
    /** "No se consiguió en el outlet", "No llegó a Cúcuta"… */
    val motivo: String = "",
    /** "Jean Levi's 511 · talla 32 × 1" */
    val detalle: String = "",
    val fecha: Timestamp? = null,
    /** La devolución del dinero ya se hizo (se marca en Cartera). */
    val devuelto: Boolean = false,
    val devueltoPor: String? = null,
    val devueltoEn: Timestamp? = null
)

enum class TipoResolucion(val etiqueta: String) {
    SALDO_A_FAVOR("Saldo a favor"),
    DEVOLUCION("Devolución")
}

/** Lo que el cliente eligió para un faltante. Solo lo escribe el cliente y no se puede cambiar. */
data class Resolucion(
    val tipo: TipoResolucion = TipoResolucion.SALDO_A_FAVOR,
    /** Número de Nequi o cuenta para la devolución. */
    val cuenta: String = "",
    val fecha: Timestamp? = null
)
