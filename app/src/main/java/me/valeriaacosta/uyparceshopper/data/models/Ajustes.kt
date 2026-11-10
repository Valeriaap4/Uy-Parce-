package me.valeriaacosta.uyparceshopper.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

/** Ajustes del negocio que define el admin. Se guardan en config/ajustes. */
data class Ajustes(
    val tasaDolar: Double = 4000.0,
    val margenDetal: Double = 0.30,
    val comisionAvanzado: Double = 0.20,
    val comisionParcero: Double = 0.25,
    val seguroUsd: Double = 10.0,
    val tarifaRegular: Double = 3.3,
    val tarifaPrime300: Double = 3.8,
    val tarifaPrime800: Double = 5.0,
    /** Plazo para pagar un pedido después de confirmarlo (y después de un rechazo). */
    val horasParaPagar: Int = 24,
    /** Precio del envío nacional (COP) de cada zona, en el mismo orden de Zonas.lista. */
    val preciosZona: List<Int> = Zonas.lista.map { it.precioCop },
    val categorias: List<Categoria> = Categoria.porDefecto,
    val mediosPago: List<MedioPago> = emptyList(),
    val actualizadoPor: String? = null,
    val actualizadoEn: Timestamp? = null
) {
    /** Las 3 zonas con el precio que puso el admin. */
    @get:Exclude
    val zonas: List<Zona>
        get() = Zonas.lista.mapIndexed { i, z -> z.copy(precioCop = preciosZona.getOrElse(i) { z.precioCop }) }

    fun zonaDe(ciudad: String): Zona = zonas.firstOrNull { ciudad in it.ciudades } ?: zonas.last()

    /** USD por libra según el tipo de envío de la caja. */
    fun tarifaDe(tipo: TipoEnvio): Double = when (tipo) {
        TipoEnvio.REGULAR -> tarifaRegular
        TipoEnvio.PRIME_300 -> tarifaPrime300
        TipoEnvio.PRIME_800 -> tarifaPrime800
    }
}
