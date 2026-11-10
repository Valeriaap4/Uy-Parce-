package me.valeriaacosta.uyparceshopper.data.firebase

import com.google.firebase.firestore.DocumentReference
import kotlinx.coroutines.tasks.await
import me.valeriaacosta.uyparceshopper.data.models.Carrito
import me.valeriaacosta.uyparceshopper.data.models.ItemCarrito

/** Carrito del cliente en carritos/{uid}: se conserva al cerrar la app o cambiar de celular. */
object CarritoRepositorio {

    fun ref(uid: String): DocumentReference = db.collection(Col.CARRITOS).document(uid)

    suspend fun guardar(uid: String, items: List<ItemCarrito>) {
        ref(uid).set(Carrito(items, items.map { it.productoId }.distinct())).await()
    }
}
