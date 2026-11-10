package me.valeriaacosta.uyparceshopper.data.models

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.roundToInt

/** Desglose de lo que paga un cliente por un pedido, en la moneda de su tipo. */
data class Desglose(
    val moneda: Moneda,
    val productos: Double,
    val comision: Double,
    val porcentajeComision: Double,
    val envioInternacional: Double,
    val envioNacional: Double
) {
    val total: Double get() = productos + comision + envioInternacional + envioNacional
}

object Precios {

    /** Redondea pesos al mil más cercano. */
    private fun redondearMil(cop: Double): Double = (cop / 1000.0).roundToInt() * 1000.0

    /**
     * Precio por unidad que ve el cliente en el catálogo.
     * Mayorista: precio del outlet en USD (la comisión se suma aparte).
     * Detal: (precio USD + envío estimado por peso) × tasa × (1 + margen), en COP.
     */
    fun precioUnitario(producto: Producto, tipo: TipoCliente, ajustes: Ajustes): Double =
        if (tipo.esMayorista) producto.precioUsd
        else redondearMil(
            (producto.precioUsd + producto.pesoLb * ajustes.tarifaRegular) *
                ajustes.tasaDolar * (1 + ajustes.margenDetal)
        )

    fun comisionDe(tipo: TipoCliente, ajustes: Ajustes): Double = when (tipo) {
        TipoCliente.MAYORISTA_AVANZADO -> ajustes.comisionAvanzado
        TipoCliente.MAYORISTA_PARCERO -> ajustes.comisionParcero
        TipoCliente.DETAL -> 0.0
    }

    fun desglose(lineas: List<LineaPedido>, tipo: TipoCliente, zona: Zona, ajustes: Ajustes): Desglose {
        val productos = lineas.sumOf { precioUnitario(it.producto, tipo, ajustes) * it.cantidad }
        return if (tipo.esMayorista) {
            val pct = comisionDe(tipo, ajustes)
            val peso = lineas.sumOf { it.producto.pesoLb * it.cantidad }
            Desglose(
                moneda = Moneda.USD,
                productos = productos,
                comision = productos * pct,
                porcentajeComision = pct,
                envioInternacional = if (lineas.isEmpty()) 0.0 else peso * ajustes.tarifaRegular + ajustes.seguroUsd,
                envioNacional = zona.precioCop / ajustes.tasaDolar
            )
        } else {
            Desglose(Moneda.COP, productos, 0.0, 0.0, 0.0, zona.precioCop.toDouble())
        }
    }

    fun desglose(pedido: Pedido, ajustes: Ajustes): Desglose =
        desglose(pedido.lineas, pedido.cliente.tipoCliente ?: TipoCliente.DETAL, pedido.zona, ajustes)
}

private val simbolosUsd = DecimalFormatSymbols(Locale.US)
private val simbolosCop = DecimalFormatSymbols(Locale.forLanguageTag("es-CO")).apply { groupingSeparator = '.' }

/** "$1,214.40" o "$3.865.000" según la moneda. */
fun formatoDinero(valor: Double, moneda: Moneda): String = when (moneda) {
    Moneda.USD -> "$" + DecimalFormat("#,##0.00", simbolosUsd).format(valor)
    Moneda.COP -> "$" + DecimalFormat("#,##0", simbolosCop).format(valor)
}
