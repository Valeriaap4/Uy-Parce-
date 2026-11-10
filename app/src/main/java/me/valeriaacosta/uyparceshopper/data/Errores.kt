package me.valeriaacosta.uyparceshopper.data

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreException.Code
import me.valeriaacosta.uyparceshopper.data.firebase.ErrorNegocio

/** Mensaje en español, entendible para cualquiera, para un error de Firebase o de la app. */
fun Throwable.mensaje(): String = when (this) {
    is ErrorNegocio -> message ?: "No se pudo completar"
    is FirebaseAuthWeakPasswordException -> "La contraseña es muy débil: usa al menos 6 caracteres"
    is FirebaseAuthInvalidCredentialsException ->
        if (errorCode == "ERROR_INVALID_EMAIL") "El correo no es válido" else "Correo o contraseña incorrectos"
    is FirebaseAuthInvalidUserException ->
        if (errorCode == "ERROR_USER_DISABLED") "Esta cuenta está bloqueada" else "Correo o contraseña incorrectos"
    is FirebaseAuthUserCollisionException -> "Ese correo ya tiene una cuenta"
    is FirebaseAuthException -> "No se pudo iniciar sesión (${errorCode.lowercase().removePrefix("error_")})"
    is FirebaseTooManyRequestsException -> "Demasiados intentos. Espera unos minutos y vuelve a intentar"
    is FirebaseNetworkException -> "Sin conexión a internet"
    is FirebaseFirestoreException -> when (code) {
        Code.PERMISSION_DENIED -> "No tienes permiso para hacer esto"
        Code.UNAVAILABLE -> "Sin conexión a internet. Intenta de nuevo cuando tengas señal"
        Code.NOT_FOUND -> "Ya no existe: puede que alguien lo haya eliminado"
        Code.ALREADY_EXISTS -> "Eso ya existe"
        Code.ABORTED, Code.FAILED_PRECONDITION -> "Alguien más lo cambió al mismo tiempo. Intenta de nuevo"
        Code.DEADLINE_EXCEEDED -> "La conexión está muy lenta. Intenta de nuevo"
        Code.RESOURCE_EXHAUSTED -> "Se acabó la cuota gratuita de Firebase por hoy. Intenta mañana"
        Code.INVALID_ARGUMENT -> "Algún dato no es válido (por ejemplo, una foto demasiado grande)"
        else -> "No se pudo guardar (${code.name.lowercase()})"
    }
    else -> "Algo salió mal. Intenta de nuevo"
}
