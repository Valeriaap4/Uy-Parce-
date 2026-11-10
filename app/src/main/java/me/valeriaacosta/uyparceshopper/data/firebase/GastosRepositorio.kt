package me.valeriaacosta.uyparceshopper.data.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import me.valeriaacosta.uyparceshopper.data.models.Gasto
import me.valeriaacosta.uyparceshopper.data.models.Usuario
import me.valeriaacosta.uyparceshopper.data.models.formatoDinero

/** Gastos del negocio. Los registran el supervisor y el admin. */
object GastosRepositorio {

    private val col get() = db.collection(Col.GASTOS)

    /** El id del gasto se genera al abrir el formulario: si se reintenta, no queda repetido. */
    suspend fun crear(gasto: Gasto, quien: Usuario) {
        val lote = db.batch()
        lote.set(col.document(gasto.id), gasto.copy(creadoPor = quien.nombre, creadoEn = Timestamp.now()))
        AuditoriaRepositorio.registrar(lote, quien, "Registró gasto", "${gasto.categoria} · ${formatoDinero(gasto.monto, gasto.moneda)} ${gasto.moneda}")
        lote.commit().await()
    }

    suspend fun recientes(limite: Long = 300): List<Gasto> =
        col.orderBy("fecha", Query.Direction.DESCENDING).limit(limite).get().await().aLista(Gasto::class.java)

    suspend fun desde(desde: Timestamp): List<Gasto> =
        col.whereGreaterThanOrEqualTo("fecha", desde).get().await().aLista(Gasto::class.java)
}
