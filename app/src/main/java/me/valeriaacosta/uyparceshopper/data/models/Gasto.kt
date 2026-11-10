package me.valeriaacosta.uyparceshopper.data.models

import com.google.firebase.Timestamp

/** Gasto del negocio (fletes, empaque, aranceles…). Se guarda en gastos/{id}. */
data class Gasto(
    val id: String = "",
    val categoria: String = "",
    val descripcion: String = "",
    val monto: Double = 0.0,
    val moneda: Moneda = Moneda.COP,
    val proveedor: String = "",
    /** Día del gasto (a mediodía, hora de Colombia). */
    val fecha: Timestamp? = null,
    val creadoPor: String = "",
    val creadoEn: Timestamp? = null
) {
    companion object {
        val categorias = listOf(
            "Compra de mercancía", "Flete y envío", "Aranceles e impuestos de importación", "Empaque",
            "Transporte local", "Comisiones de plataforma", "Publicidad", "Software y herramientas",
            "Gastos bancarios", "Otros"
        )
    }
}
