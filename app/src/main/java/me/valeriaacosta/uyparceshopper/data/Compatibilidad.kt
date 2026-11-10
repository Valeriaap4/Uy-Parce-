package me.valeriaacosta.uyparceshopper.data

import me.valeriaacosta.uyparceshopper.data.models.*

/*
 * TEMPORAL: mantiene compilando las pantallas que todavía usan la forma vieja de los modelos
 * mientras se conectan a Firebase, fase por fase. Cuando ninguna pantalla lo use, se borra.
 */

val Pago.pedido: Pedido get() = DatosEjemplo.pedido(numeroPedido)

val Caja.pedidos: List<Pedido> get() = numerosPedido.mapNotNull { DatosEjemplo.buscarPedido(it) }

val Caja.cliente: Usuario? get() = clienteId?.let { id -> DatosEjemplo.usuarios.firstOrNull { it.id == id } }

val Usuario.saldoAFavor: Double
    get() = Precios.saldoAFavor(DatosEjemplo.pedidos.filter { it.clienteId == id }, moneda)

val Notificacion.hace: String get() = hace(fecha)

fun Precios.desglose(pedido: Pedido, @Suppress("UNUSED_PARAMETER") ajustes: Ajustes): Desglose = pedido.desglose
