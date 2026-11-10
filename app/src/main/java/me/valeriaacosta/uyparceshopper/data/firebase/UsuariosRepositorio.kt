package me.valeriaacosta.uyparceshopper.data.firebase

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import me.valeriaacosta.uyparceshopper.data.models.Rol
import me.valeriaacosta.uyparceshopper.data.models.TipoCliente
import me.valeriaacosta.uyparceshopper.data.models.Usuario

/** Cuentas de clientes y del equipo (colección usuarios). */
object UsuariosRepositorio {

    private val col get() = db.collection(Col.USUARIOS)

    fun ref(uid: String): DocumentReference = col.document(uid)
    fun consultaTodos(): Query = col
    fun consultaClientes(): Query = col.whereEqualTo("rol", Rol.CLIENTE.name)

    suspend fun obtener(uid: String): Usuario? = col.document(uid).get().await().aObjeto(Usuario::class.java)

    /** Guarda a alguien del equipo (su cuenta ya se creó con AuthRepositorio.crearCuentaEquipo). */
    suspend fun guardarNuevoEquipo(usuario: Usuario, admin: Usuario) {
        val lote = db.batch()
        lote.set(col.document(usuario.id), usuario)
        AuditoriaRepositorio.registrar(lote, admin, "Creó usuario", "${usuario.nombre} · ${usuario.rol.etiqueta}")
        lote.commit().await()
    }

    suspend fun aprobarCliente(cliente: Usuario, tipo: TipoCliente, admin: Usuario) {
        val lote = db.batch()
        lote.update(col.document(cliente.id), mapOf("aprobado" to true, "activo" to true, "tipoCliente" to tipo.name))
        AuditoriaRepositorio.registrar(lote, admin, "Aprobó cliente", "${cliente.nombre} · ${tipo.etiqueta}")
        lote.commit().await()
    }

    /** Rechazar un registro desactiva la cuenta (sin servidor no se puede borrar la cuenta de Firebase). */
    suspend fun rechazarCliente(cliente: Usuario, admin: Usuario) {
        val lote = db.batch()
        lote.update(col.document(cliente.id), "activo", false)
        AuditoriaRepositorio.registrar(lote, admin, "Rechazó registro", cliente.nombre)
        lote.commit().await()
    }

    /** Teléfono, ciudad y dirección. Los cambia el propio cliente o alguien del equipo. */
    suspend fun actualizarContacto(uid: String, telefono: String, ciudad: String, direccion: String, quien: Usuario) {
        val lote = db.batch()
        lote.update(col.document(uid), mapOf("telefono" to telefono.trim(), "ciudad" to ciudad, "direccion" to direccion.trim()))
        if (quien.id != uid) AuditoriaRepositorio.registrar(lote, quien, "Editó datos de cliente", "$ciudad · ${direccion.trim()}")
        lote.commit().await()
    }

    /** El admin cambia nombre, rol, tipo de cliente y si la cuenta está activa. */
    suspend fun editar(usuario: Usuario, nombre: String, rol: Rol, tipo: TipoCliente?, activo: Boolean, admin: Usuario) {
        val cambios = buildList {
            if (nombre.trim() != usuario.nombre) add("nombre")
            if (rol != usuario.rol) add("rol: ${usuario.rol.etiqueta} → ${rol.etiqueta}")
            if (tipo != usuario.tipoCliente) add("tipo: ${usuario.tipoCliente?.etiqueta ?: "—"} → ${tipo?.etiqueta ?: "—"}")
            if (activo != usuario.activo) add(if (activo) "activado" else "desactivado")
        }
        if (cambios.isEmpty()) return
        val lote = db.batch()
        lote.update(
            col.document(usuario.id),
            mapOf("nombre" to nombre.trim(), "rol" to rol.name, "tipoCliente" to tipo?.name, "activo" to activo)
        )
        AuditoriaRepositorio.registrar(lote, admin, "Editó usuario", "${usuario.nombre} · ${cambios.joinToString(", ")}")
        lote.commit().await()
    }
}
