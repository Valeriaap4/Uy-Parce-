package me.valeriaacosta.uyparceshopper.data.models

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Lo que paga un cliente por un pedido, en la moneda de su tipo. Se calcula al confirmar y se
 * guarda congelado dentro del pedido, junto con la tasa, la tarifa por libra y el seguro de ese
 * momento: si el admin cambia los ajustes, los pedidos y facturas ya hechos no cambian.
 */
data class Desglose(
    val moneda: Moneda = Moneda.COP,
    val productos: Double = 0.0,
    val comision: Double = 0.0,
    val porcentajeComision: Double = 0.0,
    val envioInternacional: Double = 0.0,
    val envioNacional: Double = 0.0,
    val total: Double = Precios.redondear(productos + comision + envioInternacional + envioNacional, moneda),
    val tasaDolar: Double = 0.0,
    val tarifaLb: Double = 0.0,
    val seguroUsd: Double = 0.0,
    /** Lo que gana el negocio: la comisión (mayorista) o el margen (detal). Para los reportes. */
    val ganancia: Double = 0.0
)

object Precios {

    /** Redondea pesos al mil más cercano. */
    private fun redondearMil(cop: Double): Double = (cop / 1000.0).roundToInt() * 1000.0

    /** Dólares a centavos y pesos a pesos enteros (evita totales como 120.30000000001). */
    fun redondear(valor: Double, moneda: Moneda): Double = when (moneda) {
        Moneda.USD -> (valor * 100).roundToLong() / 100.0
        Moneda.COP -> valor.roundToLong().toDouble()
    }

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

    /** Copia las líneas del carrito con el precio por unidad de hoy: así queda congelado en el pedido. */
    fun congelar(lineas: List<LineaPedido>, tipo: TipoCliente, ajustes: Ajustes): List<LineaPedido> =
        lineas.map { it.copy(precioUnitario = precioUnitario(it.producto, tipo, ajustes)) }

    /** Desglose de unas líneas que ya tienen el precio por unidad congelado. */
    fun desglose(lineas: List<LineaPedido>, tipo: TipoCliente, zona: Zona, ajustes: Ajustes): Desglose {
        val productos = lineas.sumOf { it.precioUnitario * it.cantidad }
        return if (tipo.esMayorista) {
            val porcentaje = comisionDe(tipo, ajustes)
            val peso = lineas.sumOf { it.producto.pesoLb * it.cantidad }
            val comision = redondear(productos * porcentaje, Moneda.USD)
            Desglose(
                moneda = Moneda.USD,
                productos = redondear(productos, Moneda.USD),
                comision = comision,
                porcentajeComision = porcentaje,
                envioInternacional = if (lineas.isEmpty()) 0.0
                    else redondear(peso * ajustes.tarifaRegular + ajustes.seguroUsd, Moneda.USD),
                envioNacional = redondear(zona.precioCop / ajustes.tasaDolar, Moneda.USD),
                tasaDolar = ajustes.tasaDolar,
                tarifaLb = ajustes.tarifaRegular,
                seguroUsd = ajustes.seguroUsd,
                ganancia = comision
            )
        } else {
            // Margen del detal = precio cobrado − costo (precio del outlet + envío estimado, en pesos).
            val costo = lineas.sumOf {
                (it.producto.precioUsd + it.producto.pesoLb * ajustes.tarifaRegular) * ajustes.tasaDolar * it.cantidad
            }
            Desglose(
                moneda = Moneda.COP,
                productos = redondear(productos, Moneda.COP),
                envioNacional = zona.precioCop.toDouble(),
                tasaDolar = ajustes.tasaDolar,
                tarifaLb = ajustes.tarifaRegular,
                seguroUsd = ajustes.seguroUsd,
                ganancia = redondear(productos - costo, Moneda.COP)
            )
        }
    }

    /** Desglose de un carrito con los precios de hoy (para mostrar antes de confirmar). */
    fun desgloseCarrito(carrito: List<LineaPedido>, tipo: TipoCliente, zona: Zona, ajustes: Ajustes): Desglose =
        desglose(congelar(carrito, tipo, ajustes), tipo, zona, ajustes)

    /**
     * Lo que el cliente pagó por unas unidades que no va a recibir.
     * Detal: el precio por unidad (ya incluye el envío). Mayorista: precio + comisión + envío
     * internacional estimado de esas prendas. El seguro y el envío nacional no se devuelven.
     */
    fun valorFaltante(pedido: Pedido, linea: LineaPedido, unidades: Int): Double {
        val d = pedido.desglose
        return if (d.moneda == Moneda.USD)
            redondear(unidades * (linea.precioUnitario * (1 + d.porcentajeComision) + linea.producto.pesoLb * d.tarifaLb), Moneda.USD)
        else redondear(unidades * linea.precioUnitario, Moneda.COP)
    }

    /**
     * Saldo a favor de un cliente en una moneda: los faltantes que eligió como saldo, menos el saldo
     * que ya usó en sus pedidos. Si un pedido se cancela antes de pagarse, ese saldo vuelve.
     */
    fun saldoAFavor(pedidosDelCliente: List<Pedido>, moneda: Moneda): Double {
        val ganado = pedidosDelCliente.sumOf { p ->
            p.faltantes
                .filter { it.moneda == moneda && p.resoluciones[it.id]?.tipo == TipoResolucion.SALDO_A_FAVOR }
                .sumOf { it.valor }
        }
        val usado = pedidosDelCliente
            .filter { it.moneda == moneda && !(it.estado == EstadoPedido.CANCELADO && it.pagadoEn == null) }
            .sumOf { it.saldoAplicado }
        return redondear((ganado - usado).coerceAtLeast(0.0), moneda)
    }
}

private val simbolosUsd = DecimalFormatSymbols(Locale.US)
private val simbolosCop = DecimalFormatSymbols(Locale.forLanguageTag("es-CO")).apply { groupingSeparator = '.' }

/** "$1,214.40" o "$3.865.000" según la moneda. */
fun formatoDinero(valor: Double, moneda: Moneda): String = when (moneda) {
    Moneda.USD -> "$" + DecimalFormat("#,##0.00", simbolosUsd).format(valor)
    Moneda.COP -> "$" + DecimalFormat("#,##0", simbolosCop).format(valor)
}

/** 0.2 → "20 %", 0.275 → "27,5 %" (redondea; no corta como toInt()). */
fun porcentaje(fraccion: Double): String {
    val decimas = (fraccion * 1000).roundToLong()
    return if (decimas % 10 == 0L) "${decimas / 10} %" else "${decimas / 10},${decimas % 10} %"
}
