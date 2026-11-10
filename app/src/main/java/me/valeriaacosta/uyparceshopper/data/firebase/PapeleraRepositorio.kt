package me.valeriaacosta.uyparceshopper.data.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.tasks.await
import me.valeriaacosta.uyparceshopper.data.models.ElementoPapelera
import me.valeriaacosta.uyparceshopper.data.models.Usuario

/** Lo eliminado queda aquí con una copia completa, para que el admin lo pueda restaurar. */
object PapeleraRepositorio {

    private val col get() = db.collection(Col.PAPELERA)

    fun consultaTodos(): Query = col

    fun agregar(
        lote: WriteBatch, tipo: String, nombre: String, coleccion: String, docId: String,
        datos: Map<String, Any?>, quien: Usuario
    ) {
        val ref = col.document()
        lote.set(ref, ElementoPapelera(ref.id, tipo, nombre, coleccion, docId, datos, quien.nombre, Timestamp.now()))
    }

    /** Lo devuelve a su colección, con el mismo id que tenía. */
    suspend fun restaurar(elemento: ElementoPapelera, admin: Usuario) {
        val lote = db.batch()
        lote.set(db.collection(elemento.coleccion).document(elemento.docId), elemento.datos)
        lote.delete(col.document(elemento.id))
        AuditoriaRepositorio.registrar(lote, admin, "Restauró de la papelera", "${elemento.tipo} · ${elemento.nombre}")
        lote.commit().await()
    }

    /** Lo borra para siempre (si es un producto, también sus fotos). */
    suspend fun eliminarDefinitivo(elemento: ElementoPapelera, admin: Usuario) {
        val lote = db.batch()
        lote.delete(col.document(elemento.id))
        if (elemento.coleccion == Col.PRODUCTOS) {
            val fotos = (elemento.datos["fotos"] as? List<*>)?.filterIsInstance<String>().orEmpty()
            FotosRepositorio.borrar(lote, fotos)
        }
        AuditoriaRepositorio.registrar(lote, admin, "Eliminó definitivamente", "${elemento.tipo} · ${elemento.nombre}")
        lote.commit().await()
    }
}
