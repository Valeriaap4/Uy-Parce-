package me.valeriaacosta.uyparceshopper.data.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Transaction
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.tasks.await
import me.valeriaacosta.uyparceshopper.data.models.Notificacion

/** Avisos dentro de la app (lista de Notificaciones del cliente). */
object NotificacionesRepositorio {

    private val col get() = db.collection(Col.NOTIFICACIONES)

    private fun nueva(para: String, titulo: String, texto: String, numeroPedido: Int?) =
        col.document().let { ref -> ref to Notificacion(ref.id, para, titulo, texto, Timestamp.now(), false, numeroPedido) }

    fun avisar(lote: WriteBatch, para: String, titulo: String, texto: String, numeroPedido: Int? = null) {
        val (ref, n) = nueva(para, titulo, texto, numeroPedido)
        lote.set(ref, n)
    }

    fun avisar(tx: Transaction, para: String, titulo: String, texto: String, numeroPedido: Int? = null) {
        val (ref, n) = nueva(para, titulo, texto, numeroPedido)
        tx.set(ref, n)
    }

    /** Los avisos de una persona (se ordenan en la app para no necesitar índices). */
    fun consultaDe(uid: String): Query = col.whereEqualTo("para", uid)

    suspend fun marcarLeida(id: String) {
        col.document(id).update("leida", true).await()
    }

    suspend fun marcarTodasLeidas(ids: List<String>) {
        if (ids.isEmpty()) return
        val lote = db.batch()
        ids.take(400).forEach { lote.update(col.document(it), "leida", true) }
        lote.commit().await()
    }
}
