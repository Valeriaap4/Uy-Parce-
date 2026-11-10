package me.valeriaacosta.uyparceshopper.data.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Transaction
import kotlinx.coroutines.tasks.await
import me.valeriaacosta.uyparceshopper.data.models.*
import java.util.UUID

/** Cajas: crear, empacar (operador), sellar y recibir en Cúcuta (supervisor). */
object CajasRepositorio {

    private val col get() = db.collection(Col.CAJAS)

    fun ref(codigo: String): DocumentReference = col.document(codigo)

    /** Cajas con trabajo pendiente (abiertas, en camino o recibiéndose). */
    fun consultaActivas(): Query =
        col.whereIn("estado", listOf(EstadoCaja.ABIERTA.name, EstadoCaja.ENVIADA.name, EstadoCaja.EN_CUCUTA.name))

    suspend fun obtener(codigo: String): Caja? = ref(codigo).get().await().aObjeto(Caja::class.java)

    /** Las últimas cajas ya recibidas en Cúcuta (historial). */
    suspend fun recibidasRecientes(): List<Caja> =
        col.whereEqualTo("estado", EstadoCaja.RECIBIDA.name).get().await().aLista(Caja::class.java)
            .sortedByDescending { it.fechaRecepcion }.take(60)

    fun codigo(n: Long): String = "CJ-" + n.toString().padStart(4, '0')

    /** Código sugerido para la próxima caja. */
    suspend fun siguienteCodigo(): String = codigo((refContador("cajas").get().await().getLong("ultimo") ?: 0L) + 1)

    /** Crea una caja abierta. Si el código tiene el formato CJ-0000 y es mayor que el último, el contador avanza. */
    suspend fun crear(codigo: String, tipo: TipoCaja, cliente: Usuario?, quien: Usuario) {
        db.runTransaction { tx ->
            if (tx.get(ref(codigo)).exists()) throw ErrorNegocio("Ya existe una caja con el código $codigo")
            val ultimo = tx.get(refContador("cajas")).getLong("ultimo") ?: 0L
            val n = Regex("""CJ-(\d{4,})""").matchEntire(codigo)?.groupValues?.get(1)?.toLongOrNull()
            tx.set(ref(codigo), Caja(codigo, tipo, cliente?.id, cliente?.nombre, creadaPor = quien.nombre, creadaEn = Timestamp.now()))
            if (n != null && n > ultimo) tx.set(refContador("cajas"), mapOf("ultimo" to n))
            AuditoriaRepositorio.registrar(tx, quien, "Creó caja", "$codigo · ${tipo.etiqueta}" + (cliente?.let { " · ${it.nombre}" } ?: ""))
            Unit
        }.await()
    }

    /**
     * Empacar: la prenda escaneada se asigna al pedido más antiguo que la tiene comprada y sin caja.
     * candidatos: pedidos que pueden ir en esta caja (mayorista: los de ese cliente; detal: los de detal),
     * del más antiguo al más nuevo.
     */
    suspend fun agregarPrenda(codigo: String, productoId: String, talla: String, candidatos: List<Int>): PrendaCaja = db.runTransaction { tx ->
        val caja = tx.get(ref(codigo)).aObjeto(Caja::class.java) ?: throw ErrorNegocio("La caja ya no existe")
        if (caja.estado != EstadoCaja.ABIERTA) throw ErrorNegocio("Esta caja ya se selló")
        for (numero in candidatos) {
            val p = tx.get(refPedido(numero)).aObjeto(Pedido::class.java) ?: continue
            if (p.estado != EstadoPedido.PAGADO && p.estado != EstadoPedido.COMPRADO) continue
            if (caja.tipo == TipoCaja.MAYORISTA && p.clienteId != caja.clienteId) continue
            if (caja.tipo == TipoCaja.DETAL && p.moneda != Moneda.COP) continue
            val i = p.lineas.indexOfFirst { it.producto.id == productoId && it.talla == talla && it.porEmpacar > 0 }
            if (i < 0) continue
            val linea = p.lineas[i]
            val lineas = p.lineas.toMutableList().also { it[i] = linea.copy(empacadas = linea.empacadas + 1) }
            val prenda = PrendaCaja(UUID.randomUUID().toString(), numero, productoId, linea.producto.nombre, talla)
            tx.update(refPedido(numero), mapOf("lineas" to lineas, "cajas" to (p.cajas + codigo).distinct()))
            tx.update(ref(codigo), mapOf("contenido" to caja.contenido + prenda, "numerosPedido" to (caja.numerosPedido + numero).distinct()))
            return@runTransaction prenda
        }
        throw ErrorNegocio(
            if (caja.tipo == TipoCaja.MAYORISTA) "No hay unidades compradas de esta prenda esperando caja para ${caja.clienteNombre ?: "este cliente"}"
            else "No hay unidades compradas de esta prenda esperando caja"
        )
    }.await()

    /** Saca una prenda de la caja abierta (y la devuelve a "por empacar" en su pedido). */
    suspend fun quitarPrenda(codigo: String, prendaId: String) {
        db.runTransaction { tx ->
            val caja = tx.get(ref(codigo)).aObjeto(Caja::class.java) ?: throw ErrorNegocio("La caja ya no existe")
            if (caja.estado != EstadoCaja.ABIERTA) throw ErrorNegocio("Esta caja ya se selló")
            val prenda = caja.contenido.firstOrNull { it.id == prendaId } ?: return@runTransaction Unit
            val p = tx.get(refPedido(prenda.numeroPedido)).aObjeto(Pedido::class.java)
            val contenido = caja.contenido.filter { it.id != prendaId }
            val sigueEnCaja = contenido.any { it.numeroPedido == prenda.numeroPedido }
            tx.update(
                ref(codigo),
                mapOf("contenido" to contenido, "numerosPedido" to if (sigueEnCaja) caja.numerosPedido else caja.numerosPedido - prenda.numeroPedido)
            )
            if (p != null) {
                val i = p.lineas.indexOfFirst { it.producto.id == prenda.productoId && it.talla == prenda.talla && it.empacadas > 0 }
                if (i >= 0) {
                    val lineas = p.lineas.toMutableList().also { it[i] = it[i].copy(empacadas = it[i].empacadas - 1) }
                    tx.update(refPedido(p.numero), mapOf("lineas" to lineas, "cajas" to if (sigueEnCaja) p.cajas else p.cajas - codigo))
                }
            }
            Unit
        }.await()
    }

    /** Lee los pedidos de la caja y las otras cajas de esos pedidos (para saber si pueden avanzar). */
    private fun leerPedidosYCajas(tx: Transaction, caja: Caja): Pair<List<Pedido>, List<Caja>> {
        val pedidos = caja.numerosPedido.mapNotNull { tx.get(refPedido(it)).aObjeto(Pedido::class.java) }
        val otras = pedidos.flatMap { it.cajas }.distinct().filter { it != caja.codigo }
            .mapNotNull { tx.get(ref(it)).aObjeto(Caja::class.java) }
        return pedidos to otras
    }

    /**
     * Sella la caja y la envía a Cúcuta. Mayorista: se guarda el peso y lo que cobra el courier.
     * Los pedidos que ya tienen todo empacado en cajas selladas pasan a "En camino".
     */
    suspend fun sellar(codigo: String, pesoLb: Double?, tipoEnvio: TipoEnvio?, ajustes: Ajustes, quien: Usuario) {
        db.runTransaction { tx ->
            val caja = tx.get(ref(codigo)).aObjeto(Caja::class.java) ?: throw ErrorNegocio("La caja ya no existe")
            if (caja.estado != EstadoCaja.ABIERTA) throw ErrorNegocio("Esta caja ya se selló")
            if (caja.contenido.isEmpty()) throw ErrorNegocio("La caja está vacía")
            val (pedidos, otras) = leerPedidosYCajas(tx, caja)
            val ahora = Timestamp.now()
            val costo = if (pesoLb != null && tipoEnvio != null)
                Precios.redondear(pesoLb * ajustes.tarifaDe(tipoEnvio) + ajustes.seguroUsd, Moneda.USD) else null
            val sellada = caja.copy(estado = EstadoCaja.ENVIADA, pesoLb = pesoLb, tipoEnvio = tipoEnvio, costoEnvio = costo, fechaEnvio = ahora)
            tx.update(
                ref(codigo),
                mapOf("estado" to EstadoCaja.ENVIADA.name, "pesoLb" to pesoLb, "tipoEnvio" to tipoEnvio?.name, "costoEnvio" to costo, "fechaEnvio" to ahora)
            )
            for (p in pedidos) {
                val nuevo = p.avance(otras + sellada)
                if (nuevo == p.estado) continue
                tx.update(refPedido(p.numero), "estado", nuevo.name)
                if (nuevo == EstadoPedido.EN_CAMINO) {
                    NotificacionesRepositorio.avisar(
                        tx, p.clienteId, "Tu pedido va hacia Cúcuta",
                        "Ya compramos y empacamos tu pedido #${p.numero}. Te avisamos cuando llegue a Cúcuta.", p.numero
                    )
                }
            }
            AuditoriaRepositorio.registrar(
                tx, quien, "Selló caja",
                "$codigo · ${caja.prendas} prendas" + (pesoLb?.let { " · $it lb · ${tipoEnvio?.etiqueta}" } ?: "")
            )
            Unit
        }.await()
    }

    /**
     * Recepción en Cúcuta: marca recibida la prenda escaneada (la del pedido más antiguo de la caja que
     * la espera). Devuelve la prenda, que dice a qué pedido va. La caja pasa a "En Cúcuta".
     */
    suspend fun recibirPrenda(codigo: String, productoId: String, talla: String): PrendaCaja = db.runTransaction { tx ->
        val caja = tx.get(ref(codigo)).aObjeto(Caja::class.java) ?: throw ErrorNegocio("La caja ya no existe")
        if (caja.estado != EstadoCaja.ENVIADA && caja.estado != EstadoCaja.EN_CUCUTA) {
            throw ErrorNegocio("Esta caja no está en camino (${caja.estado.etiqueta.lowercase()})")
        }
        val iguales = caja.contenido.filter { it.productoId == productoId && it.talla == talla }
        if (iguales.isEmpty()) throw ErrorNegocio("Esta prenda no viene en esta caja")
        val prenda = iguales.filter { !it.recibida }.minByOrNull { it.numeroPedido }
            ?: throw ErrorNegocio("Ya recibiste todas las unidades de esta prenda en esta caja")
        val p = tx.get(refPedido(prenda.numeroPedido)).aObjeto(Pedido::class.java)
        val contenido = caja.contenido.map { if (it.id == prenda.id) it.copy(recibida = true) else it }
        tx.update(ref(codigo), mapOf("contenido" to contenido, "estado" to EstadoCaja.EN_CUCUTA.name))
        if (p != null) {
            val i = p.lineas.indexOfFirst { it.producto.id == productoId && it.talla == talla && it.recibidas < it.empacadas }
            if (i >= 0) {
                val lineas = p.lineas.toMutableList().also { it[i] = it[i].copy(recibidas = it[i].recibidas + 1) }
                tx.update(refPedido(p.numero), "lineas", lineas)
            }
        }
        prenda.copy(recibida = true)
    }.await()

    /**
     * Cierra la recepción. Los pedidos cuyas cajas ya llegaron todas pasan a "En Cúcuta"; lo empacado
     * que no llegó queda como faltante y se avisa a cada cliente. Devuelve cuántos pedidos tienen faltantes.
     */
    suspend fun cerrarRecepcion(codigo: String, quien: Usuario): Int = db.runTransaction { tx ->
        val caja = tx.get(ref(codigo)).aObjeto(Caja::class.java) ?: throw ErrorNegocio("La caja ya no existe")
        if (caja.estado == EstadoCaja.RECIBIDA) return@runTransaction 0
        if (caja.estado != EstadoCaja.ENVIADA && caja.estado != EstadoCaja.EN_CUCUTA) throw ErrorNegocio("Esta caja todavía no se ha enviado")
        val (pedidos, otras) = leerPedidosYCajas(tx, caja)
        val ahora = Timestamp.now()
        val cerrada = caja.copy(estado = EstadoCaja.RECIBIDA, fechaRecepcion = ahora)
        tx.update(ref(codigo), mapOf("estado" to EstadoCaja.RECIBIDA.name, "fechaRecepcion" to ahora))
        var conFaltantes = 0
        for (p in pedidos) {
            val nuevo = p.avance(otras + cerrada)
            if (nuevo == EstadoPedido.EN_CUCUTA && p.estado != EstadoPedido.EN_CUCUTA) {
                // Ya llegaron todas sus cajas: lo que se empacó y no se recibió es faltante.
                val nuevosFaltantes = p.lineas.filter { it.empacadas > it.recibidas }.map { l ->
                    val n = l.empacadas - l.recibidas
                    Faltante(
                        UUID.randomUUID().toString(), n, Precios.valorFaltante(p, l, n), p.moneda,
                        "No llegó a Cúcuta", "${l.producto.nombre} · talla ${l.talla} × $n", ahora
                    )
                }
                val cambios = mutableMapOf<String, Any?>("estado" to EstadoPedido.EN_CUCUTA.name)
                if (nuevosFaltantes.isNotEmpty()) {
                    cambios["faltantes"] = p.faltantes + nuevosFaltantes
                    cambios["conSaldo"] = true
                    conFaltantes++
                }
                tx.update(refPedido(p.numero), cambios)
                val faltan = nuevosFaltantes.sumOf { it.unidades }
                NotificacionesRepositorio.avisar(
                    tx, p.clienteId, "Tu pedido llegó a Cúcuta",
                    if (faltan == 0) "El pedido #${p.numero} ya está en Cúcuta. Pronto lo enviamos a tu ciudad."
                    else "El pedido #${p.numero} ya está en Cúcuta, pero " +
                        (if (faltan == 1) "faltó 1 prenda" else "faltaron $faltan prendas") + ". Elige qué hacer con ese dinero.",
                    p.numero
                )
            } else if (nuevo != p.estado) {
                tx.update(refPedido(p.numero), "estado", nuevo.name)
            }
        }
        AuditoriaRepositorio.registrar(tx, quien, "Cerró recepción", "$codigo · ${caja.recibidas} de ${caja.prendas} prendas")
        conFaltantes
    }.await()

    /** Una caja vacía se puede eliminar (va a la papelera). */
    suspend fun eliminarVacia(codigo: String, quien: Usuario) {
        val snap = ref(codigo).get().await()
        val caja = snap.aObjeto(Caja::class.java) ?: throw ErrorNegocio("La caja ya no existe")
        if (caja.contenido.isNotEmpty()) throw ErrorNegocio("Solo se pueden eliminar cajas vacías")
        val lote = db.batch()
        PapeleraRepositorio.agregar(lote, "Caja", "$codigo (vacía)", Col.CAJAS, codigo, snap.data ?: emptyMap(), quien)
        lote.delete(ref(codigo))
        AuditoriaRepositorio.registrar(lote, quien, "Eliminó caja", codigo)
        lote.commit().await()
    }
}
