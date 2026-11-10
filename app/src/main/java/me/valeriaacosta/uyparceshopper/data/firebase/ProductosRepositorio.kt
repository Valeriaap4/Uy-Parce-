package me.valeriaacosta.uyparceshopper.data.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import me.valeriaacosta.uyparceshopper.data.models.Producto
import me.valeriaacosta.uyparceshopper.data.models.Usuario
import me.valeriaacosta.uyparceshopper.data.normalizarCodigo

/** Catálogo de productos. Lo manejan el operador y el admin. */
object ProductosRepositorio {

    private val col get() = db.collection(Col.PRODUCTOS)

    fun consultaTodos(): Query = col

    /**
     * Crea o edita un producto junto con sus fotos, en un solo lote (o se guarda todo o nada).
     * fotosNuevas: fotos agregadas en el formulario. fotosQuitadas: ids de fotos que se quitaron.
     */
    suspend fun guardar(
        producto: Producto, esNuevo: Boolean, fotosNuevas: List<FotoNueva>, fotosQuitadas: List<String>, quien: Usuario
    ) {
        val listas = FotosRepositorio.leerArchivos(fotosNuevas)
        val lote = db.batch()
        FotosRepositorio.agregar(lote, listas, "producto", quien.id)
        val ref = col.document(producto.id)
        if (esNuevo) {
            lote.set(ref, producto.copy(creadoPor = quien.nombre, creadoEn = Timestamp.now()))
        } else {
            lote.update(
                ref, mapOf(
                    "nombre" to producto.nombre, "marca" to producto.marca, "descripcion" to producto.descripcion,
                    "categoria" to producto.categoria, "pesoLb" to producto.pesoLb, "precioUsd" to producto.precioUsd,
                    "tienda" to producto.tienda, "tallas" to producto.tallas, "fotos" to producto.fotos,
                    "codigos" to producto.codigos
                )
            )
        }
        FotosRepositorio.borrar(lote, fotosQuitadas)
        AuditoriaRepositorio.registrar(lote, quien, if (esNuevo) "Creó producto" else "Editó producto", producto.nombre)
        lote.commit().await()
    }

    /** Marca el producto como agotado y avisa a los clientes que lo tienen en el carrito. Devuelve a cuántos. */
    suspend fun marcarAgotado(producto: Producto, quien: Usuario): Int {
        val carritos = db.collection(Col.CARRITOS).whereArrayContains("productoIds", producto.id).get().await()
        val lote = db.batch()
        lote.update(col.document(producto.id), "agotado", true)
        carritos.documents.forEach { carrito ->
            NotificacionesRepositorio.avisar(
                lote, carrito.id, "Producto agotado",
                "${producto.nombre} ya no está disponible. Quítalo de tu carrito para poder continuar."
            )
        }
        AuditoriaRepositorio.registrar(lote, quien, "Marcó agotado", producto.nombre)
        lote.commit().await()
        return carritos.size()
    }

    suspend fun marcarDisponible(producto: Producto, quien: Usuario) {
        val lote = db.batch()
        lote.update(col.document(producto.id), "agotado", false)
        AuditoriaRepositorio.registrar(lote, quien, "Marcó disponible", producto.nombre)
        lote.commit().await()
    }

    /** Lo manda a la papelera. Sus fotos se borran solo si el admin lo elimina definitivamente. */
    suspend fun eliminar(producto: Producto, quien: Usuario) {
        val datos = col.document(producto.id).get().await().data ?: throw ErrorNegocio("El producto ya no existe")
        val lote = db.batch()
        PapeleraRepositorio.agregar(lote, "Producto", producto.nombre, Col.PRODUCTOS, producto.id, datos, quien)
        lote.delete(col.document(producto.id))
        AuditoriaRepositorio.registrar(lote, quien, "Eliminó producto", producto.nombre)
        lote.commit().await()
    }

    /**
     * Guarda el código de barras de una talla (cuando al registrar una compra el código no se conocía).
     * Revisa que ningún otro producto tenga ya ese código.
     */
    suspend fun guardarCodigo(producto: Producto, talla: String, codigo: String, catalogo: List<Producto>, quien: Usuario) {
        val normal = normalizarCodigo(codigo)
        if (normal.isEmpty()) throw ErrorNegocio("El código está vacío")
        catalogo.firstOrNull { it.id != producto.id && it.tallaDelCodigo(normal) != null }?.let {
            throw ErrorNegocio("Ese código ya es de ${it.nombre}")
        }
        db.runTransaction { tx ->
            val actual = tx.get(col.document(producto.id)).aObjeto(Producto::class.java)
                ?: throw ErrorNegocio("El producto ya no existe")
            tx.update(col.document(producto.id), "codigos", actual.codigos + (talla to normal))
            AuditoriaRepositorio.registrar(tx, quien, "Guardó código de barras", "${producto.nombre} · talla $talla")
            Unit
        }.await()
    }
}
