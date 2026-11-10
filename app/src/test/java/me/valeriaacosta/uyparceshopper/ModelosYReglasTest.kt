package me.valeriaacosta.uyparceshopper

import com.google.firebase.firestore.util.CustomClassMapper
import me.valeriaacosta.uyparceshopper.data.DatosEjemplo
import me.valeriaacosta.uyparceshopper.data.buscarPorCodigo
import me.valeriaacosta.uyparceshopper.data.firebase.ErrorNegocio
import me.valeriaacosta.uyparceshopper.data.mensaje
import me.valeriaacosta.uyparceshopper.data.mensajeDeConfiguracion
import me.valeriaacosta.uyparceshopper.data.models.*
import me.valeriaacosta.uyparceshopper.data.normalizarCodigo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

/**
 * Pruebas que corren en el computador, sin Firebase ni emulador.
 *
 * 1. Cada modelo se convierte a lo que guarda Firestore y de vuelta, con el mismo traductor que usa
 *    la librería de Firebase, y debe quedar igual. Si un modelo tuviera algo que Firestore no sabe
 *    guardar (un Set, un Pair…) o un campo calculado sin @get:Exclude, la prueba falla.
 * 2. Reglas del negocio: avance del pedido, valor de un faltante, saldo a favor y códigos de barras.
 */
class ModelosYReglasTest {

    // ─── 1. Modelos ↔ Firestore ───

    /** Firestore devuelve los enteros como Long y los decimales como Double. */
    private fun comoServidor(valor: Any?): Any? = when (valor) {
        is Int -> valor.toLong()
        is Float -> valor.toDouble()
        is List<*> -> valor.map { comoServidor(it) }
        is Map<*, *> -> valor.entries.associate { (k, v) -> k to comoServidor(v) }
        else -> valor
    }

    private fun <T : Any> idaYVuelta(objeto: T): T {
        @Suppress("UNCHECKED_CAST")
        val guardado = CustomClassMapper.convertToPlainJavaTypes(objeto) as Map<String, Any?>
        // Se guardan justo los campos del modelo: nada calculado.
        val campos = objeto.javaClass.declaredFields
            .filter { !Modifier.isStatic(it.modifiers) && !it.isSynthetic }
            .map { it.name }.toSet()
        assertEquals("Campos guardados de ${objeto.javaClass.simpleName}", campos, guardado.keys)
        // Si al leer sobrara un campo, Firestore escribe un aviso en el Log, y en estas pruebas
        // cualquier uso de android.util.Log hace fallar la prueba: así se revisan también los anidados.
        return CustomClassMapper.convertToCustomClass(comoServidor(guardado), objeto.javaClass, null)
    }

    private fun <T : Any> probar(lista: List<T>) {
        assertTrue(lista.isNotEmpty())
        lista.forEach { assertEquals(it, idaYVuelta(it)) }
    }

    @Test fun usuarios() = probar(DatosEjemplo.usuarios)

    @Test fun productos() = probar(DatosEjemplo.productos + DatosEjemplo.productos.first().copy(
        fotos = listOf("f1", "f2"), codigos = mapOf("M" to "12345678905", "L" to "12345678912")
    ))

    @Test fun pedidos() {
        // Que los ejemplos traigan faltantes y resoluciones, para probar también esos campos.
        assertTrue(DatosEjemplo.pedidos.any { it.faltantes.isNotEmpty() && it.resoluciones.isNotEmpty() })
        probar(DatosEjemplo.pedidos)
    }

    @Test fun pagos() = probar(DatosEjemplo.pagos)
    @Test fun facturas() = probar(DatosEjemplo.facturas)
    @Test fun cajas() = probar(DatosEjemplo.cajas)
    @Test fun gastos() = probar(DatosEjemplo.gastos)
    @Test fun notificaciones() = probar(DatosEjemplo.notificaciones)
    @Test fun auditoria() = probar(DatosEjemplo.auditoria)
    @Test fun papelera() = probar(DatosEjemplo.papelera)

    @Test fun ajustes() = probar(listOf(
        DatosEjemplo.ajustes,
        DatosEjemplo.ajustes.copy(mediosPago = listOf(MedioPago("m9", "Zelle", "Ana", "ana@correo.test", Moneda.USD)))
    ))

    @Test fun carrito() {
        val items = DatosEjemplo.carritoLaura
        probar(listOf(Carrito(items, items.map { it.productoId }.distinct())))
    }

    /** Comprueba que la revisión de campos sobrantes de idaYVuelta de verdad hace fallar la prueba. */
    @Test(expected = RuntimeException::class)
    fun unCampoSobranteSeDetecta() {
        val pedidoConExtra = mapOf("numero" to 1L, "cliente" to mapOf("nombre" to "Ana", "iniciales" to "A"))
        CustomClassMapper.convertToCustomClass(pedidoConExtra, Pedido::class.java, null)
    }

    // ─── 2. Reglas del negocio ───

    private val producto = Producto(id = "p1", nombre = "Jean", pesoLb = 1.5, precioUsd = 50.0, tallas = listOf("30", "32"))

    private fun pedido(estado: EstadoPedido, vararg lineas: LineaPedido, cajas: List<String> = emptyList()) =
        Pedido(numero = 1001, lineas = lineas.toList(), estado = estado, cajas = cajas)

    @Test fun avanceDeCompra() {
        val linea = LineaPedido(producto, "32", cantidad = 2, precioUnitario = 50.0)
        // Falta comprar una unidad: sigue pagado.
        assertEquals(EstadoPedido.PAGADO, pedido(EstadoPedido.PAGADO, linea.copy(compradas = 1)).avance(emptyList()))
        // Una comprada y otra que no se consiguió: comprado.
        assertEquals(EstadoPedido.COMPRADO,
            pedido(EstadoPedido.PAGADO, linea.copy(compradas = 1, noConseguidas = 1)).avance(emptyList()))
        // No se consiguió nada: el pedido se cancela.
        assertEquals(EstadoPedido.CANCELADO, pedido(EstadoPedido.PAGADO, linea.copy(noConseguidas = 2)).avance(emptyList()))
    }

    @Test fun avanceDeCajas() {
        val empacada = LineaPedido(producto, "32", cantidad = 2, precioUnitario = 50.0, compradas = 2, empacadas = 2)
        val p = pedido(EstadoPedido.COMPRADO, empacada, cajas = listOf("CJ-0001"))
        val caja = Caja(codigo = "CJ-0001")
        // Caja abierta: sigue comprado. Sellada: en camino. Recibida en Cúcuta: en Cúcuta.
        assertEquals(EstadoPedido.COMPRADO, p.avance(listOf(caja)))
        assertEquals(EstadoPedido.EN_CAMINO, p.avance(listOf(caja.copy(estado = EstadoCaja.ENVIADA))))
        assertEquals(EstadoPedido.EN_CUCUTA, p.avance(listOf(caja.copy(estado = EstadoCaja.RECIBIDA))))
        // Si no llegó la información de una de sus cajas, no avanza.
        val dosCajas = p.copy(cajas = listOf("CJ-0001", "CJ-0002"))
        assertEquals(EstadoPedido.COMPRADO, dosCajas.avance(listOf(caja.copy(estado = EstadoCaja.ENVIADA))))
        // Con una prenda sin empacar no sale.
        val sinEmpacar = pedido(EstadoPedido.COMPRADO, empacada.copy(empacadas = 1), cajas = listOf("CJ-0001"))
        assertEquals(EstadoPedido.COMPRADO, sinEmpacar.avance(listOf(caja.copy(estado = EstadoCaja.ENVIADA))))
    }

    @Test fun valorDeUnFaltante() {
        val linea = LineaPedido(producto, "32", cantidad = 2, precioUnitario = 50.0)
        // Mayorista: precio + 20 % de comisión + 1,5 lb × 3,3 USD = 60 + 4,95.
        val mayorista = pedido(EstadoPedido.PAGADO, linea).copy(
            desglose = Desglose(moneda = Moneda.USD, porcentajeComision = 0.2, tarifaLb = 3.3)
        )
        assertEquals(64.95, Precios.valorFaltante(mayorista, linea, 1), 0.001)
        // Detal: lo que pagó por las unidades.
        val detal = pedido(EstadoPedido.PAGADO, linea.copy(precioUnitario = 380000.0))
            .copy(desglose = Desglose(moneda = Moneda.COP))
        assertEquals(760000.0, Precios.valorFaltante(detal, linea.copy(precioUnitario = 380000.0), 2), 0.001)
    }

    @Test fun saldoAFavor() {
        val f = Faltante("f1", 1, 64.95, Moneda.USD)
        val conFaltante = pedido(EstadoPedido.EN_CUCUTA).copy(
            desglose = Desglose(moneda = Moneda.USD), faltantes = listOf(f),
            resoluciones = mapOf("f1" to Resolucion(TipoResolucion.SALDO_A_FAVOR))
        )
        val usoSaldo = pedido(EstadoPedido.PAGADO).copy(desglose = Desglose(moneda = Moneda.USD), saldoAplicado = 20.0)
        // Cancelado antes de pagar: el saldo que usó vuelve.
        val cancelado = pedido(EstadoPedido.CANCELADO).copy(desglose = Desglose(moneda = Moneda.USD), saldoAplicado = 10.0)
        assertEquals(44.95, Precios.saldoAFavor(listOf(conFaltante, usoSaldo, cancelado), Moneda.USD), 0.001)
        // Si eligió devolución, no hay saldo.
        val devolucion = conFaltante.copy(resoluciones = mapOf("f1" to Resolucion(TipoResolucion.DEVOLUCION)))
        assertEquals(0.0, Precios.saldoAFavor(listOf(devolucion), Moneda.USD), 0.001)
        // El saldo en dólares no se mezcla con el de pesos.
        assertEquals(0.0, Precios.saldoAFavor(listOf(conFaltante), Moneda.COP), 0.001)
    }

    @Test fun totalDelDesglose() {
        val ajustes = Ajustes()
        val lineas = Precios.congelar(listOf(LineaPedido(producto, "32", cantidad = 2)), TipoCliente.MAYORISTA_AVANZADO, ajustes)
        val d = Precios.desglose(lineas, TipoCliente.MAYORISTA_AVANZADO, ajustes.zonas.first(), ajustes)
        assertEquals(Moneda.USD, d.moneda)
        assertEquals(100.0, d.productos, 0.001)
        assertEquals(20.0, d.comision, 0.001)
        assertEquals(d.productos + d.comision + d.envioInternacional + d.envioNacional, d.total, 0.001)
    }

    @Test fun codigosDeBarras() {
        // El UPC y el EAN de la misma prenda quedan iguales.
        assertEquals(normalizarCodigo("012345678905"), normalizarCodigo("0012345678905"))
        assertEquals("ABC123", normalizarCodigo(" abc-123 "))
        assertEquals("0", normalizarCodigo("000"))
        val conCodigo = producto.copy(codigos = mapOf("30" to "12345678905", "32" to "12345678912"))
        val encontrado = buscarPorCodigo(listOf(conCodigo), "0012345678912")
        assertEquals("32", encontrado?.talla)
        assertNull(buscarPorCodigo(listOf(conCodigo), "999"))
        assertNull(buscarPorCodigo(listOf(conCodigo), "  "))
    }

    /*
     * Las excepciones de Firebase no se pueden crear en estas pruebas (sus constructores usan clases de
     * Android que solo existen en el celular). Por eso se prueba el texto que trae el error.
     */
    @Test fun mensajesDeError() {
        assertEquals("Esta prenda ya se registró", ErrorNegocio("Esta prenda ya se registró").mensaje())
        // Errores de configuración de la consola de Firebase: deben decir qué falta.
        assertEquals("Falta activar Authentication (correo y contraseña) en la consola de Firebase",
            mensajeDeConfiguracion("An internal error has occurred. [ CONFIGURATION_NOT_FOUND ]"))
        assertEquals("La configuración de Firebase no es válida: revisa el archivo google-services.json",
            mensajeDeConfiguracion("An internal error has occurred. [ API key not valid. Please pass a valid API key. ]"))
        assertNull(mensajeDeConfiguracion("An internal error has occurred."))
        assertNull(mensajeDeConfiguracion(null))
    }

    @Test fun porcentajes() {
        assertEquals("29 %", porcentaje(0.29))
        assertEquals("27,5 %", porcentaje(0.275))
        assertEquals("20 %", porcentaje(0.2))
    }
}
