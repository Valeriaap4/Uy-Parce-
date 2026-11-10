package me.valeriaacosta.uyparceshopper.data.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import me.valeriaacosta.uyparceshopper.data.masHoras
import me.valeriaacosta.uyparceshopper.data.models.*
import java.util.UUID

/** Pedidos: confirmar, pagar, comprar en el outlet, faltantes, despacho y entrega. */
object PedidosRepositorio {

    private val col get() = db.collection(Col.PEDIDOS)

    fun consultaDeCliente(uid: String): Query = col.whereEqualTo("clienteId", uid)

    /** Pedidos con trabajo pendiente (el equipo los escucha en vivo). */
    fun consultaActivos(): Query = col.whereIn("estado", EstadoPedido.activos.map { it.name })

    suspend fun obtener(numero: Int): Pedido? = refPedido(numero).get().await().aObjeto(Pedido::class.java)

    suspend fun deCliente(uid: String): List<Pedido> = consultaDeCliente(uid).get().await().aLista(Pedido::class.java)

    suspend fun pagadosDesde(desde: Timestamp): List<Pedido> =
        col.whereGreaterThanOrEqualTo("pagadoEn", desde).get().await().aLista(Pedido::class.java)

    /** Pedidos con saldo usado o faltantes (para Cartera). */
    suspend fun conSaldo(): List<Pedido> = col.whereEqualTo("conSaldo", true).get().await().aLista(Pedido::class.java)

    // ─── Cliente ────────────────────────────────────────────────────────────

    /**
     * Convierte el carrito guardado en un pedido, en una sola transacción: revisa que nada se haya
     * agotado, toma el siguiente número, congela precios y desglose, aplica el saldo a favor, vacía
     * el carrito y deja el aviso "Recuerda pagar". Si se reintenta, el carrito ya está vacío y no se
     * crea un pedido repetido.
     */
    suspend fun crear(cliente: Usuario, ajustes: Ajustes, saldoDisponible: Double): Pedido = db.runTransaction { tx ->
        val carrito = tx.get(CarritoRepositorio.ref(cliente.id)).aObjeto(Carrito::class.java)?.items.orEmpty()
        if (carrito.isEmpty()) throw ErrorNegocio("Tu carrito está vacío. Si ya confirmaste, revisa Mis pedidos.")
        val contador = tx.get(refContador("pedidos"))
        if (!contador.exists()) throw ErrorNegocio("La tienda todavía no está lista para recibir pedidos. Intenta más tarde.")
        val productos = carrito.map { it.productoId }.distinct().associateWith { id ->
            tx.get(db.collection(Col.PRODUCTOS).document(id)).aObjeto(Producto::class.java)
        }
        val lineas = carrito.map { item ->
            val p = productos[item.productoId] ?: throw ErrorNegocio("Un producto de tu carrito ya no existe. Quítalo para continuar.")
            if (p.agotado) throw ErrorNegocio("${p.nombre} se agotó. Quítalo del carrito para continuar.")
            if (item.talla !in p.tallas) throw ErrorNegocio("La talla ${item.talla} de ${p.nombre} ya no está disponible.")
            LineaPedido(p, item.talla, item.cantidad)
        }
        val numero = ((contador.getLong("ultimo") ?: 1000L) + 1).toInt()
        val tipo = cliente.tipoCliente ?: TipoCliente.DETAL
        val zona = ajustes.zonaDe(cliente.ciudad)
        val congeladas = Precios.congelar(lineas, tipo, ajustes)
        val desglose = Precios.desglose(congeladas, tipo, zona, ajustes)
        val saldo = Precios.redondear(minOf(saldoDisponible.coerceAtLeast(0.0), desglose.total), desglose.moneda)
        val ahora = Timestamp.now()
        val pedido = Pedido(
            numero, cliente.id, cliente, zona, congeladas, desglose, saldo, EstadoPedido.PENDIENTE_PAGO, ahora,
            venceEn = ahora.masHoras(ajustes.horasParaPagar), conSaldo = saldo > 0
        )
        tx.set(refPedido(numero), pedido)
        tx.update(refContador("pedidos"), "ultimo", numero)
        tx.set(CarritoRepositorio.ref(cliente.id), Carrito())
        NotificacionesRepositorio.avisar(
            tx, cliente.id, "Recuerda pagar",
            "Tienes ${ajustes.horasParaPagar} horas para pagar el pedido #$numero o se cancela.", numero
        )
        pedido
    }.await()

    /** Cancela un pedido que todavía no se ha pagado. */
    suspend fun cancelar(numero: Int, por: String, motivo: String) {
        db.runTransaction { tx ->
            val p = tx.get(refPedido(numero)).aObjeto(Pedido::class.java) ?: throw ErrorNegocio("El pedido ya no existe")
            if (p.estado == EstadoPedido.CANCELADO) return@runTransaction Unit
            if (p.estado != EstadoPedido.PENDIENTE_PAGO) {
                throw ErrorNegocio("Este pedido ya no se puede cancelar (${p.estado.etiqueta.lowercase()})")
            }
            tx.update(refPedido(numero), mapOf("estado" to EstadoPedido.CANCELADO.name, "canceladoPor" to por, "motivoCancelacion" to motivo))
            Unit
        }.await()
    }

    /** Cancela, uno por uno, los pedidos que no se pagaron a tiempo (revisando que sigan sin pagar). */
    suspend fun cancelarVencidos(pedidos: List<Pedido>): Int {
        var cancelados = 0
        for (p in pedidos.filter { it.vencido() }) {
            val ok = runCatching {
                db.runTransaction { tx ->
                    val actual = tx.get(refPedido(p.numero)).aObjeto(Pedido::class.java)
                    if (actual != null && actual.vencido()) {
                        tx.update(
                            refPedido(p.numero),
                            mapOf("estado" to EstadoPedido.CANCELADO.name, "canceladoPor" to "Sistema", "motivoCancelacion" to "No se pagó a tiempo")
                        )
                        true
                    } else false
                }.await()
            }.getOrDefault(false)
            if (ok) cancelados++
        }
        return cancelados
    }

    /**
     * El cliente reporta su pago: guarda la foto del comprobante, el pago y pone el pedido en revisión,
     * todo junto. idPago se genera al abrir el formulario: si se reintenta, no se duplica.
     */
    suspend fun reportarPago(
        numero: Int, cliente: Usuario, idPago: String, medio: String, referencia: String, fechaPago: String, foto: FotoNueva?
    ) {
        val fotoLista = foto?.let { FotosRepositorio.leerArchivos(listOf(it)).first() }
        db.runTransaction { tx ->
            val refPago = db.collection(Col.PAGOS).document(idPago)
            if (tx.get(refPago).exists()) return@runTransaction Unit // ya se guardó en un intento anterior
            val p = tx.get(refPedido(numero)).aObjeto(Pedido::class.java) ?: throw ErrorNegocio("El pedido ya no existe")
            if (p.clienteId != cliente.id) throw ErrorNegocio("Este pedido no es tuyo")
            if (p.estado == EstadoPedido.PAGO_EN_REVISION) throw ErrorNegocio("Ya recibimos un pago de este pedido: está en revisión")
            if (p.estado != EstadoPedido.PENDIENTE_PAGO) throw ErrorNegocio("Este pedido ya no está pendiente de pago")
            if (p.vencido()) throw ErrorNegocio("El plazo para pagar este pedido ya venció")
            if (fotoLista == null && p.totalAPagar > 0) throw ErrorNegocio("Adjunta la foto del comprobante")

            fotoLista?.let { FotosRepositorio.agregar(tx, listOf(it), "comprobante", cliente.id) }
            tx.set(
                refPago,
                Pago(idPago, numero, cliente.id, cliente.nombre, p.totalAPagar, p.moneda, medio, referencia.trim(), fechaPago,
                    fotoLista?.id, EstadoPago.EN_REVISION, fecha = Timestamp.now())
            )
            tx.update(refPedido(numero), mapOf("estado" to EstadoPedido.PAGO_EN_REVISION.name, "motivoRechazo" to null))
            NotificacionesRepositorio.avisar(
                tx, cliente.id, "Recibimos tu comprobante",
                "El pago del pedido #$numero está en revisión. Te avisamos cuando lo aprueben.", numero
            )
            Unit
        }.await()
    }

    /** "Recibí mi pedido" (el cliente) o "Marcar entregado" (el equipo). */
    suspend fun confirmarEntrega(numero: Int, quien: Usuario) {
        db.runTransaction { tx ->
            val p = tx.get(refPedido(numero)).aObjeto(Pedido::class.java) ?: throw ErrorNegocio("El pedido ya no existe")
            if (p.estado == EstadoPedido.ENTREGADO) return@runTransaction Unit
            if (p.estado != EstadoPedido.ENVIADO) throw ErrorNegocio("El pedido todavía no se ha enviado")
            tx.update(refPedido(numero), mapOf("estado" to EstadoPedido.ENTREGADO.name, "entregadoEn" to Timestamp.now()))
            if (quien.esEquipo) AuditoriaRepositorio.registrar(tx, quien, "Marcó entregado", "Pedido #$numero")
            Unit
        }.await()
    }

    /** El cliente elige qué hacer con un faltante. No se puede cambiar después. */
    suspend fun resolverFaltante(numero: Int, faltanteId: String, tipo: TipoResolucion, cuenta: String) {
        db.runTransaction { tx ->
            val p = tx.get(refPedido(numero)).aObjeto(Pedido::class.java) ?: throw ErrorNegocio("El pedido ya no existe")
            if (p.faltantes.none { it.id == faltanteId }) throw ErrorNegocio("Ese faltante ya no existe")
            if (faltanteId in p.resoluciones) throw ErrorNegocio("Ya elegiste qué hacer con este faltante")
            tx.update(refPedido(numero), FieldPath.of("resoluciones", faltanteId), Resolucion(tipo, cuenta.trim(), Timestamp.now()))
            Unit
        }.await()
    }

    // ─── Operador: compras en el outlet ─────────────────────────────────────

    /**
     * El operador compró una unidad: se asigna al pedido más antiguo que la necesita.
     * candidatos: pedidos pagados con ese producto y talla pendiente, del más antiguo al más nuevo.
     * Devuelve el número del pedido al que se asignó.
     */
    suspend fun registrarCompra(productoId: String, talla: String, candidatos: List<Int>): Int = db.runTransaction { tx ->
        for (numero in candidatos) {
            val p = tx.get(refPedido(numero)).aObjeto(Pedido::class.java) ?: continue
            if (p.estado != EstadoPedido.PAGADO) continue
            val i = p.lineas.indexOfFirst { it.producto.id == productoId && it.talla == talla && it.porComprar > 0 }
            if (i < 0) continue
            val lineas = p.lineas.toMutableList().also { it[i] = it[i].copy(compradas = it[i].compradas + 1) }
            // Al comprar, la unidad nueva todavía no va en ninguna caja: no hacen falta las cajas.
            val nuevoEstado = p.copy(lineas = lineas).avance(emptyList())
            tx.update(refPedido(numero), mapOf("lineas" to lineas, "estado" to nuevoEstado.name))
            return@runTransaction numero
        }
        throw ErrorNegocio("Ya se compraron todas las unidades de este producto y talla")
    }.await()

    /**
     * El operador no consiguió un producto en una talla: las unidades pendientes de esos pedidos se
     * vuelven faltantes y se avisa a cada cliente para que elija saldo a favor o devolución.
     * Si de un pedido no se consiguió nada, se cancela y el faltante cubre todo lo pagado.
     * Devuelve cuántos pedidos se afectaron.
     */
    suspend fun marcarNoConseguido(productoId: String, talla: String, candidatos: List<Int>, quien: Usuario): Int = db.runTransaction { tx ->
        // Primero se lee todo (pedidos y sus cajas); después se escribe.
        val pedidos = candidatos.mapNotNull { tx.get(refPedido(it)).aObjeto(Pedido::class.java) }
            .filter { it.estado == EstadoPedido.PAGADO }
        val cajas = pedidos.flatMap { it.cajas }.distinct()
            .mapNotNull { tx.get(db.collection(Col.CAJAS).document(it)).aObjeto(Caja::class.java) }
        val ahora = Timestamp.now()
        var afectados = 0
        var nombre = ""
        for (p in pedidos) {
            val i = p.lineas.indexOfFirst { it.producto.id == productoId && it.talla == talla && it.porComprar > 0 }
            if (i < 0) continue
            val linea = p.lineas[i]
            nombre = linea.producto.nombre
            val unidades = linea.porComprar
            val lineas = p.lineas.toMutableList().also { it[i] = linea.copy(noConseguidas = linea.noConseguidas + unidades) }
            var faltantes = p.faltantes + Faltante(
                UUID.randomUUID().toString(), unidades, Precios.valorFaltante(p, linea, unidades), p.moneda,
                "No se consiguió en el outlet", "${linea.producto.nombre} · talla $talla × $unidades", ahora
            )
            val nuevoEstado = p.copy(lineas = lineas, faltantes = faltantes).avance(cajas)
            val cambios = mutableMapOf<String, Any?>("lineas" to lineas, "estado" to nuevoEstado.name, "conSaldo" to true)
            if (nuevoEstado == EstadoPedido.CANCELADO) {
                // No se consiguió nada: se devuelve todo lo pagado (incluye envíos y comisión).
                val resto = Precios.redondear(p.desglose.total - faltantes.sumOf { it.valor }, p.moneda)
                if (resto > 0) faltantes = faltantes + Faltante(
                    UUID.randomUUID().toString(), 0, resto, p.moneda, "Pedido cancelado: no se consiguió nada",
                    "Envíos y demás cobros del pedido", ahora
                )
                cambios["canceladoPor"] = quien.nombre
                cambios["motivoCancelacion"] = "No se consiguió ningún producto"
            }
            cambios["faltantes"] = faltantes
            tx.update(refPedido(p.numero), cambios)
            NotificacionesRepositorio.avisar(
                tx, p.clienteId, "No conseguimos una prenda",
                "No conseguimos ${linea.producto.nombre} (talla $talla) para tu pedido #${p.numero}. " +
                    "Elige si quieres ese dinero como saldo a favor o como devolución.",
                p.numero
            )
            afectados++
        }
        if (afectados == 0) throw ErrorNegocio("No hay unidades pendientes de ese producto y talla")
        AuditoriaRepositorio.registrar(tx, quien, "Marcó no conseguido", "$nombre · talla $talla · $afectados pedido(s)")
        afectados
    }.await()

    // ─── Supervisor ─────────────────────────────────────────────────────────

    /** Registra la transportadora y la guía: el pedido sale de Cúcuta hacia el cliente. */
    suspend fun despachar(numero: Int, transportadora: String, guia: String, quien: Usuario) {
        db.runTransaction { tx ->
            val p = tx.get(refPedido(numero)).aObjeto(Pedido::class.java) ?: throw ErrorNegocio("El pedido ya no existe")
            if (p.estado != EstadoPedido.EN_CUCUTA) throw ErrorNegocio("El pedido no está listo para despachar (${p.estado.etiqueta.lowercase()})")
            tx.update(
                refPedido(numero),
                mapOf("estado" to EstadoPedido.ENVIADO.name, "transportadora" to transportadora, "guia" to guia.trim(), "despachadoEn" to Timestamp.now())
            )
            NotificacionesRepositorio.avisar(
                tx, p.clienteId, "Tu pedido va en camino",
                "El pedido #$numero salió de Cúcuta con $transportadora. Número de guía: ${guia.trim()}.", numero
            )
            AuditoriaRepositorio.registrar(tx, quien, "Despachó pedido", "Pedido #$numero · $transportadora ${guia.trim()}")
            Unit
        }.await()
    }

    /** Ya se le devolvió al cliente el dinero de un faltante (desde Cartera). */
    suspend fun marcarDevuelto(numero: Int, faltanteId: String, quien: Usuario) {
        db.runTransaction { tx ->
            val p = tx.get(refPedido(numero)).aObjeto(Pedido::class.java) ?: throw ErrorNegocio("El pedido ya no existe")
            val f = p.faltantes.firstOrNull { it.id == faltanteId } ?: throw ErrorNegocio("Ese faltante ya no existe")
            if (f.devuelto) return@runTransaction Unit
            val faltantes = p.faltantes.map {
                if (it.id == faltanteId) it.copy(devuelto = true, devueltoPor = quien.nombre, devueltoEn = Timestamp.now()) else it
            }
            tx.update(refPedido(numero), "faltantes", faltantes)
            AuditoriaRepositorio.registrar(tx, quien, "Hizo devolución", "Pedido #$numero · ${formatoDinero(f.valor, f.moneda)} ${f.moneda}")
            Unit
        }.await()
    }
}
