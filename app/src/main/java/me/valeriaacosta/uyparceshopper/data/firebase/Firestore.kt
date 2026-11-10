package me.valeriaacosta.uyparceshopper.data.firebase

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

/*
 * Lo que comparten todos los repositorios: nombres de colecciones, cómo escuchar datos en vivo
 * y cómo ejecutar una acción sin que un error tumbe la app.
 */

/** Nombres de las colecciones de Firestore, en un solo lugar para no escribirlos mal. */
object Col {
    const val USUARIOS = "usuarios"
    const val PRODUCTOS = "productos"
    const val FOTOS = "fotos"
    const val CONFIG = "config"
    const val CONTADORES = "contadores"
    const val CARRITOS = "carritos"
    const val PEDIDOS = "pedidos"
    const val PAGOS = "pagos"
    const val FACTURAS = "facturas"
    const val CAJAS = "cajas"
    const val GASTOS = "gastos"
    const val NOTIFICACIONES = "notificaciones"
    const val AUDITORIA = "auditoria"
    const val PAPELERA = "papelera"
}

/** La base de datos. Es un getter: solo se toca Firebase cuando de verdad se usa (no en los @Preview). */
internal val db: FirebaseFirestore get() = Firebase.firestore

internal fun refPedido(numero: Int): DocumentReference = db.collection(Col.PEDIDOS).document(numero.toString())
internal fun refContador(tipo: String): DocumentReference = db.collection(Col.CONTADORES).document(tipo)

/** Id nuevo para un documento (se genera antes de guardar: si se reintenta, no se duplica). */
fun nuevoId(coleccion: String): String = db.collection(coleccion).document().id

/** Error de una regla del negocio ("Esta prenda ya se registró"). Se le muestra tal cual a la persona. */
class ErrorNegocio(mensaje: String) : RuntimeException(mensaje)

/**
 * Ejecuta una acción contra Firebase con tiempo límite y devuelve el resultado o el error,
 * sin lanzar excepciones hacia la pantalla.
 */
suspend fun <T> intentar(segundos: Long = 20, bloque: suspend () -> T): Result<T> = try {
    Result.success(withTimeout(segundos * 1000) { bloque() })
} catch (e: TimeoutCancellationException) {
    // Va antes que CancellationException porque es un caso particular de ella.
    Result.failure(ErrorNegocio("La conexión está muy lenta. Revisa tu internet e intenta de nuevo."))
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Log.w("UyParce", "Falló una acción", e)
    Result.failure(e)
}

/** Convierte los documentos a objetos; si uno está dañado se ignora (y se anota en Logcat) en vez de tumbar la lista. */
fun <T : Any> QuerySnapshot.aLista(clase: Class<T>): List<T> = documents.mapNotNull { it.aObjeto(clase) }

fun <T : Any> DocumentSnapshot.aObjeto(clase: Class<T>): T? = try {
    toObject(clase)
} catch (e: Exception) {
    Log.w("UyParce", "No se pudo leer ${reference.path}", e)
    null
}

/**
 * Escucha una consulta en vivo. delServidor = false cuando los datos vienen del caché del celular
 * (por ejemplo, sin internet).
 */
fun <T : Any> Query.escuchar(
    clase: Class<T>,
    alRecibir: (lista: List<T>, delServidor: Boolean) -> Unit,
    alFallar: (FirebaseFirestoreException) -> Unit
): ListenerRegistration = addSnapshotListener(MetadataChanges.INCLUDE) { snap, error ->
    when {
        error != null -> alFallar(error)
        snap != null -> alRecibir(snap.aLista(clase), !snap.metadata.isFromCache)
    }
}

/** Escucha un documento en vivo. El objeto es null si el documento no existe. */
fun <T : Any> DocumentReference.escuchar(
    clase: Class<T>,
    alRecibir: (objeto: T?, existe: Boolean, delServidor: Boolean) -> Unit,
    alFallar: (FirebaseFirestoreException) -> Unit
): ListenerRegistration = addSnapshotListener(MetadataChanges.INCLUDE) { snap, error ->
    when {
        error != null -> alFallar(error)
        snap != null -> alRecibir(if (snap.exists()) snap.aObjeto(clase) else null, snap.exists(), !snap.metadata.isFromCache)
    }
}
