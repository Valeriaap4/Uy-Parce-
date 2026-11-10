package me.valeriaacosta.uyparceshopper.data.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Transaction
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.tasks.await
import me.valeriaacosta.uyparceshopper.data.models.RegistroAuditoria
import me.valeriaacosta.uyparceshopper.data.models.Usuario

/** Registro de quién hizo qué. Se agrega en el mismo lote que el cambio que describe. */
object AuditoriaRepositorio {

    private fun registro(quien: Usuario, accion: String, detalle: String): Pair<com.google.firebase.firestore.DocumentReference, RegistroAuditoria> {
        val ref = db.collection(Col.AUDITORIA).document()
        return ref to RegistroAuditoria(ref.id, quien.id, quien.nombre, accion, detalle, Timestamp.now())
    }

    fun registrar(lote: WriteBatch, quien: Usuario, accion: String, detalle: String) {
        val (ref, r) = registro(quien, accion, detalle)
        lote.set(ref, r)
    }

    fun registrar(tx: Transaction, quien: Usuario, accion: String, detalle: String) {
        val (ref, r) = registro(quien, accion, detalle)
        tx.set(ref, r)
    }

    /** Los últimos registros (solo el admin puede leerlos). */
    fun consultaRecientes(limite: Long = 200): Query =
        db.collection(Col.AUDITORIA).orderBy("fecha", Query.Direction.DESCENDING).limit(limite)

    suspend fun recientes(limite: Long = 200): List<RegistroAuditoria> =
        consultaRecientes(limite).get().await().aLista(RegistroAuditoria::class.java)
}
