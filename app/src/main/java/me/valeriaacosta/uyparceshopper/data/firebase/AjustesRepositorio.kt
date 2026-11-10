package me.valeriaacosta.uyparceshopper.data.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import kotlinx.coroutines.tasks.await
import me.valeriaacosta.uyparceshopper.data.DatosEjemplo
import me.valeriaacosta.uyparceshopper.data.models.Ajustes
import me.valeriaacosta.uyparceshopper.data.models.Usuario

/** Ajustes del negocio (config/ajustes) y la preparación inicial de la base de datos. */
object AjustesRepositorio {

    val ref: DocumentReference get() = db.collection(Col.CONFIG).document("ajustes")

    /** Cambia solo los campos indicados y deja el "antes → después" en la auditoría. */
    suspend fun actualizar(campos: Map<String, Any?>, quien: Usuario, accion: String, detalle: String) {
        val lote = db.batch()
        lote.update(ref, campos + mapOf("actualizadoPor" to quien.nombre, "actualizadoEn" to Timestamp.now()))
        AuditoriaRepositorio.registrar(lote, quien, accion, detalle)
        lote.commit().await()
    }

    /**
     * Crea lo que la tienda necesita para arrancar: los ajustes, los contadores (el primer pedido será
     * el #1001, la primera factura FAC-0001 y la primera caja CJ-0001) y, si se pide, los 8 productos
     * de ejemplo. Lo que ya existe no se toca.
     */
    suspend fun prepararBaseDeDatos(admin: Usuario, conProductosDeEjemplo: Boolean) {
        db.runTransaction { tx ->
            val ajustes = tx.get(ref)
            val pedidos = tx.get(refContador("pedidos"))
            val facturas = tx.get(refContador("facturas"))
            val cajas = tx.get(refContador("cajas"))
            if (!ajustes.exists()) {
                tx.set(ref, Ajustes(mediosPago = DatosEjemplo.mediosPago, actualizadoPor = admin.nombre, actualizadoEn = Timestamp.now()))
            }
            if (!pedidos.exists()) tx.set(refContador("pedidos"), mapOf("ultimo" to 1000))
            if (!facturas.exists()) tx.set(refContador("facturas"), mapOf("ultimo" to 0))
            if (!cajas.exists()) tx.set(refContador("cajas"), mapOf("ultimo" to 0))
            if (conProductosDeEjemplo) {
                DatosEjemplo.productos.forEach { p ->
                    val id = "demo-${p.id}"
                    tx.set(db.collection(Col.PRODUCTOS).document(id), p.copy(id = id, creadoPor = admin.nombre, creadoEn = Timestamp.now()))
                }
            }
            AuditoriaRepositorio.registrar(
                tx, admin, "Preparó la base de datos",
                if (conProductosDeEjemplo) "Ajustes, contadores y 8 productos de ejemplo" else "Ajustes y contadores"
            )
            Unit
        }.await()
    }
}
