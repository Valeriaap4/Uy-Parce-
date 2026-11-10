package me.valeriaacosta.uyparceshopper.data.firebase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.LruCache
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.Transaction
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import kotlin.math.roundToInt

/** Foto elegida en un formulario: el archivo ya comprimido en el celular y el id que tendrá en Firestore. */
data class FotoNueva(val id: String, val ruta: String)

/** Foto lista para guardar: los bytes de la foto completa y de su miniatura. */
class FotoLista(val id: String, val bytes: ByteArray, val miniatura: ByteArray)

/**
 * Fotos de productos y comprobantes. Como el plan gratuito de Firebase no incluye Storage, cada foto
 * se comprime en el celular y se guarda en Firestore: fotos/{id} (máx. ~600 KB, Firestore admite
 * 1 MB por documento) y fotos/{id}_min (miniatura de 320 px para las listas).
 */
object FotosRepositorio {

    private const val LADO_MAX = 1280
    private const val LADO_MINI = 320
    private const val MAX_BYTES = 600_000
    private const val MAX_BYTES_MINI = 60_000

    /** Fotos ya descargadas, en memoria (hasta ~24 MB). */
    private val cache = object : LruCache<String, ByteArray>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ByteArray) = value.size
    }

    /** Carpeta del caché donde quedan las fotos de la cámara y las ya comprimidas. */
    fun carpeta(context: Context): File = File(context.cacheDir, "fotos").apply { mkdirs() }

    /** Comprime una foto (de la cámara o la galería) y la deja como archivo JPEG. Devuelve la ruta. */
    suspend fun preparar(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val bytes = comprimir(decodificar(context, uri), MAX_BYTES)
        val archivo = File(carpeta(context), "foto_${UUID.randomUUID()}.jpg")
        archivo.writeBytes(bytes)
        archivo.absolutePath
    }

    /** Lee los archivos ya comprimidos y arma sus miniaturas (en segundo plano). */
    suspend fun leerArchivos(fotos: List<FotoNueva>): List<FotoLista> = withContext(Dispatchers.IO) {
        fotos.map { f ->
            val bytes = File(f.ruta).readBytes()
            FotoLista(f.id, bytes, miniatura(bytes))
        }
    }

    private fun decodificar(context: Context, uri: Uri): Bitmap {
        val fuente = ImageDecoder.createSource(context.contentResolver, uri)
        return ImageDecoder.decodeBitmap(fuente) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val ancho = info.size.width
            val alto = info.size.height
            val escala = minOf(1f, LADO_MAX.toFloat() / maxOf(ancho, alto))
            if (escala < 1f) {
                decoder.setTargetSize((ancho * escala).roundToInt().coerceAtLeast(1), (alto * escala).roundToInt().coerceAtLeast(1))
            }
        }
    }

    /** JPEG con la mejor calidad que quepa en maxBytes. Las imágenes con transparencia se ponen sobre blanco. */
    private fun comprimir(original: Bitmap, maxBytes: Int): ByteArray {
        var bitmap = if (original.hasAlpha()) sobreBlanco(original) else original
        var calidad = 85
        while (true) {
            val bytes = ByteArrayOutputStream().use { salida ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, calidad, salida)
                salida.toByteArray()
            }
            when {
                bytes.size <= maxBytes -> return bytes
                calidad > 50 -> calidad -= 10
                bitmap.width > 200 && bitmap.height > 200 -> bitmap = Bitmap.createScaledBitmap(
                    bitmap, (bitmap.width * 0.8f).roundToInt(), (bitmap.height * 0.8f).roundToInt(), true
                )
                else -> return bytes
            }
        }
    }

    private fun sobreBlanco(original: Bitmap): Bitmap {
        val resultado = Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
        Canvas(resultado).apply {
            drawColor(Color.WHITE)
            drawBitmap(original, 0f, 0f, null)
        }
        return resultado
    }

    private fun miniatura(bytes: ByteArray): ByteArray {
        val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, medidas)
        var muestra = 1
        while (maxOf(medidas.outWidth, medidas.outHeight) / (muestra * 2) >= LADO_MINI) muestra *= 2
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = muestra })
            ?: return bytes
        val escala = minOf(1f, LADO_MINI.toFloat() / maxOf(bitmap.width, bitmap.height))
        val chica = if (escala < 1f) Bitmap.createScaledBitmap(
            bitmap, (bitmap.width * escala).roundToInt().coerceAtLeast(1), (bitmap.height * escala).roundToInt().coerceAtLeast(1), true
        ) else bitmap
        return comprimir(chica, MAX_BYTES_MINI)
    }

    private fun documento(bytes: ByteArray, tipo: String, uid: String) =
        mapOf("datos" to Blob.fromBytes(bytes), "tipo" to tipo, "creadoPor" to uid, "fecha" to Timestamp.now())

    /** Agrega las fotos al lote (foto completa + miniatura). tipo: "producto" o "comprobante". */
    fun agregar(lote: WriteBatch, fotos: List<FotoLista>, tipo: String, uid: String) {
        fotos.forEach { f ->
            lote.set(db.collection(Col.FOTOS).document(f.id), documento(f.bytes, tipo, uid))
            lote.set(db.collection(Col.FOTOS).document("${f.id}_min"), documento(f.miniatura, tipo, uid))
            recordar(f)
        }
    }

    fun agregar(tx: Transaction, fotos: List<FotoLista>, tipo: String, uid: String) {
        fotos.forEach { f ->
            tx.set(db.collection(Col.FOTOS).document(f.id), documento(f.bytes, tipo, uid))
            tx.set(db.collection(Col.FOTOS).document("${f.id}_min"), documento(f.miniatura, tipo, uid))
            recordar(f)
        }
    }

    /** Las fotos recién guardadas se muestran sin volver a descargarlas. */
    private fun recordar(f: FotoLista) {
        cache.put(f.id, f.bytes)
        cache.put("${f.id}_min", f.miniatura)
    }

    fun borrar(lote: WriteBatch, ids: List<String>) {
        ids.forEach { id ->
            lote.delete(db.collection(Col.FOTOS).document(id))
            lote.delete(db.collection(Col.FOTOS).document("${id}_min"))
        }
    }

    /**
     * Bytes de una foto: primero de la memoria, después del caché del celular y por último del
     * servidor. Si no hay miniatura se usa la foto completa. Devuelve null si no se pudo leer.
     */
    suspend fun leer(id: String, miniatura: Boolean): ByteArray? {
        val clave = if (miniatura) "${id}_min" else id
        cache.get(clave)?.let { return it }
        val bytes = leerDocumento(clave) ?: if (miniatura) leerDocumento(id) else null
        bytes?.let { cache.put(clave, it) }
        return bytes
    }

    private suspend fun leerDocumento(clave: String): ByteArray? = try {
        val ref = db.collection(Col.FOTOS).document(clave)
        val local = try { ref.get(Source.CACHE).await() } catch (e: Exception) { null }
        val snap = if (local != null && local.exists()) local else ref.get().await()
        snap.getBlob("datos")?.toBytes()
    } catch (e: Exception) {
        null
    }
}
