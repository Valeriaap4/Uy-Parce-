package me.valeriaacosta.uyparceshopper.data.firebase

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.app
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.auth
import com.google.firebase.initialize
import kotlinx.coroutines.tasks.await
import me.valeriaacosta.uyparceshopper.data.models.Rol
import me.valeriaacosta.uyparceshopper.data.models.Usuario
import java.util.UUID

/** Cuentas: iniciar sesión, registrarse, recuperar la contraseña y crear cuentas del equipo. */
object AuthRepositorio {

    val auth: FirebaseAuth get() = Firebase.auth

    suspend fun iniciarSesion(correo: String, clave: String) {
        auth.signInWithEmailAndPassword(correo.trim(), clave).await()
    }

    /**
     * Crea la cuenta y su documento de usuario (cliente pendiente de aprobación).
     * Si el correo ya tiene cuenta pero un registro anterior quedó a medias (sin documento), lo completa.
     * Si falla al guardar el documento, borra la cuenta para que se pueda intentar de nuevo.
     */
    suspend fun registrar(datos: Usuario, clave: String): Usuario {
        val correo = datos.correo.trim()
        val cuenta = try {
            auth.createUserWithEmailAndPassword(correo, clave).await().user
                ?: throw ErrorNegocio("No se pudo crear la cuenta")
        } catch (e: FirebaseAuthUserCollisionException) {
            val existente = try {
                auth.signInWithEmailAndPassword(correo, clave).await().user
            } catch (otro: Exception) {
                null
            } ?: throw e
            if (db.collection(Col.USUARIOS).document(existente.uid).get().await().exists()) {
                auth.signOut()
                throw e
            }
            existente
        }
        val usuario = datos.copy(
            id = cuenta.uid, correo = correo, rol = Rol.CLIENTE, aprobado = false, activo = true,
            tipoCliente = null, creadoEn = Timestamp.now()
        )
        try {
            db.collection(Col.USUARIOS).document(usuario.id).set(usuario).await()
        } catch (e: Exception) {
            runCatching { cuenta.delete().await() }
            auth.signOut()
            throw e
        }
        return usuario
    }

    /**
     * Envía el correo para crear una contraseña nueva (en español). Si el correo no tiene cuenta no
     * se avisa: así nadie puede averiguar quién está registrado.
     */
    suspend fun recuperarClave(correo: String) {
        auth.setLanguageCode("es")
        try {
            auth.sendPasswordResetEmail(correo.trim()).await()
        } catch (e: FirebaseAuthInvalidUserException) {
            // El correo no existe: se responde igual que si se hubiera enviado.
        }
    }

    fun cerrarSesion() = auth.signOut()

    /**
     * El admin crea la cuenta de alguien del equipo sin cerrar su propia sesión: se usa una segunda
     * conexión a Firebase solo para crear la cuenta, con una contraseña al azar que nadie conoce.
     * Después Firebase le envía a esa persona un correo para que cree su propia contraseña.
     * Devuelve el uid de la cuenta nueva.
     */
    suspend fun crearCuentaEquipo(context: Context, correo: String): String {
        val nombreApp = "cuentas-equipo"
        val segunda = FirebaseApp.getApps(context).firstOrNull { it.name == nombreApp }
            ?: Firebase.initialize(context, Firebase.app.options, nombreApp)
        val auth2 = Firebase.auth(segunda)
        val claveAlAzar = UUID.randomUUID().toString() + "Aa1!"
        val uid = auth2.createUserWithEmailAndPassword(correo.trim(), claveAlAzar).await().user?.uid
            ?: throw ErrorNegocio("No se pudo crear la cuenta")
        auth2.signOut()
        recuperarClave(correo)
        return uid
    }
}
