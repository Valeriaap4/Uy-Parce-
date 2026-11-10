package me.valeriaacosta.uyparceshopper.data.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import me.valeriaacosta.uyparceshopper.data.masHoras
import me.valeriaacosta.uyparceshopper.data.models.EstadoPago
import me.valeriaacosta.uyparceshopper.data.models.EstadoPedido
import me.valeriaacosta.uyparceshopper.data.models.Factura
import me.valeriaacosta.uyparceshopper.data.models.Pago
import me.valeriaacosta.uyparceshopper.data.models.Pedido
import me.valeriaacosta.uyparceshopper.data.models.Usuario

/** Pagos que reportan los clientes y su revisión (supervisor o admin). */
object PagosRepositorio {

    private val col get() = db.collection(Col.PAGOS)

    fun consultaDeCliente(uid: String): Query = col.whereEqualTo("clienteId", uid)
    fun consultaPorRevisar(): Query = col.whereEqualTo("estado", EstadoPago.EN_REVISION.name)

    suspend fun recientes(limite: Long = 150): List<Pago> =
        col.orderBy("fecha", Query.Direction.DESCENDING).limit(limite).get().await().aLista(Pago::class.java)

    suspend fun obtener(id: String): Pago? = col.document(id).get().await().aObjeto(Pago::class.java)

    /**
     * Aprueba el pago en una sola transacción: el pago queda aprobado, el pedido "Pagado", se crea la
     * factura con el siguiente número (FAC-0001…), se avisa al cliente y queda en la auditoría.
     * Devuelve el número de la factura.
     */
    suspend fun aprobar(pagoId: String, revisor: Usuario): String = db.runTransaction { tx ->
        val pago = tx.get(col.document(pagoId)).aObjeto(Pago::class.java) ?: throw ErrorNegocio("El pago ya no existe")
        if (pago.estado != EstadoPago.EN_REVISION) throw ErrorNegocio("Este pago ya fue revisado")
        val pedido = tx.get(refPedido(pago.numeroPedido)).aObjeto(Pedido::class.java)
            ?: throw ErrorNegocio("El pedido #${pago.numeroPedido} ya no existe")
        if (pedido.estado != EstadoPedido.PAGO_EN_REVISION) {
            throw ErrorNegocio("El pedido #${pedido.numero} no está esperando revisión (${pedido.estado.etiqueta.lowercase()})")
        }
        val n = (tx.get(refContador("facturas")).getLong("ultimo") ?: 0L) + 1
        val numeroFactura = "FAC-" + n.toString().padStart(4, '0')
        val ahora = Timestamp.now()
        val pagado = pedido.copy(estado = EstadoPedido.PAGADO, pagadoEn = ahora, motivoRechazo = null)

        tx.update(col.document(pagoId), mapOf("estado" to EstadoPago.APROBADO.name, "revisadoPor" to revisor.nombre, "revisadoEn" to ahora))
        tx.update(refPedido(pedido.numero), mapOf("estado" to EstadoPedido.PAGADO.name, "pagadoEn" to ahora, "motivoRechazo" to null))
        tx.set(db.collection(Col.FACTURAS).document(numeroFactura), Factura(numeroFactura, pedido.numero, pedido.clienteId, pagado, ahora))
        tx.set(refContador("facturas"), mapOf("ultimo" to n))
        NotificacionesRepositorio.avisar(
            tx, pedido.clienteId, "Pago aprobado",
            "Aprobamos el pago del pedido #${pedido.numero}. Tu factura $numeroFactura ya está lista y vamos a comprar tus prendas.",
            pedido.numero
        )
        AuditoriaRepositorio.registrar(tx, revisor, "Aprobó pago", "Pedido #${pedido.numero} · ${pago.medio} ${pago.referencia} · $numeroFactura")
        numeroFactura
    }.await()

    /**
     * Rechaza el comprobante: el pedido vuelve a "Pendiente de pago" con un plazo nuevo para pagar
     * y se le avisa al cliente el motivo.
     */
    suspend fun rechazar(pagoId: String, motivo: String, revisor: Usuario, horasParaPagar: Int) {
        db.runTransaction { tx ->
            val pago = tx.get(col.document(pagoId)).aObjeto(Pago::class.java) ?: throw ErrorNegocio("El pago ya no existe")
            if (pago.estado != EstadoPago.EN_REVISION) throw ErrorNegocio("Este pago ya fue revisado")
            val pedido = tx.get(refPedido(pago.numeroPedido)).aObjeto(Pedido::class.java)
                ?: throw ErrorNegocio("El pedido #${pago.numeroPedido} ya no existe")
            val ahora = Timestamp.now()
            tx.update(
                col.document(pagoId),
                mapOf("estado" to EstadoPago.RECHAZADO.name, "motivoRechazo" to motivo, "revisadoPor" to revisor.nombre, "revisadoEn" to ahora)
            )
            if (pedido.estado == EstadoPedido.PAGO_EN_REVISION) {
                tx.update(
                    refPedido(pedido.numero),
                    mapOf("estado" to EstadoPedido.PENDIENTE_PAGO.name, "motivoRechazo" to motivo, "venceEn" to ahora.masHoras(horasParaPagar))
                )
            }
            NotificacionesRepositorio.avisar(
                tx, pedido.clienteId, "Comprobante rechazado",
                "El pago del pedido #${pedido.numero} fue rechazado: ${motivo.replaceFirstChar { it.lowercase() }}. " +
                    "Tienes $horasParaPagar horas para enviarlo otra vez.",
                pedido.numero
            )
            AuditoriaRepositorio.registrar(tx, revisor, "Rechazó pago", "Pedido #${pedido.numero} · $motivo")
            Unit
        }.await()
    }
}
