package me.valeriaacosta.uyparceshopper.data.firebase

import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import me.valeriaacosta.uyparceshopper.data.models.Factura

/** Facturas (se crean al aprobar un pago: ver PagosRepositorio.aprobar). */
object FacturasRepositorio {

    private val col get() = db.collection(Col.FACTURAS)

    fun consultaDeCliente(uid: String): Query = col.whereEqualTo("clienteId", uid)

    suspend fun recientes(limite: Long = 300): List<Factura> =
        col.orderBy("fecha", Query.Direction.DESCENDING).limit(limite).get().await().aLista(Factura::class.java)

    /**
     * Factura de un pedido. Un cliente solo puede consultar las suyas, por eso se filtra también por
     * clienteId cuando quien pregunta es un cliente.
     */
    suspend fun dePedido(numeroPedido: Int, clienteId: String? = null): Factura? {
        var consulta: Query = col.whereEqualTo("numeroPedido", numeroPedido)
        if (clienteId != null) consulta = consulta.whereEqualTo("clienteId", clienteId)
        return consulta.limit(1).get().await().aLista(Factura::class.java).firstOrNull()
    }
}
