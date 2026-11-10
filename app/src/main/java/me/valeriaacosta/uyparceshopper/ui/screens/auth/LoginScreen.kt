package me.valeriaacosta.uyparceshopper.ui.screens.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.Sesion
import me.valeriaacosta.uyparceshopper.data.mensaje
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── LoginScreen ─────────────────────────────────────────────────────────────

/**
 * Pantalla 2. Inicio de sesión con Firebase. Al entrar, la navegación lleva sola al inicio del rol
 * (o a "cuenta en revisión" si el admin todavía no la aprueba).
 */
@Composable
fun LoginScreen(
    onClickRegister: () -> Unit,
    onClickForgotPassword: () -> Unit
) {
    val estado = LocalEstadoApp.current
    val scope = rememberCoroutineScope()
    var correo by rememberSaveable { mutableStateOf("") }
    var clave by rememberSaveable { mutableStateOf("") }
    var errCorreo by remember { mutableStateOf<String?>(null) }
    var errClave by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var entrando by remember { mutableStateOf(false) }

    // Si la sesión se cierra (por ejemplo, la cuenta está desactivada), el botón vuelve a funcionar.
    LaunchedEffect(estado.sesion) { if (estado.sesion == Sesion.SIN_SESION) entrando = false }

    FondoMarca(
        titulo = "UyParce Shopper",
        subtitulo = "Encarga ropa de outlets de EE. UU. y recíbela en Colombia"
    ) {
        Text("Inicia sesión para continuar", style = MaterialTheme.typography.titleLarge)
        (error ?: estado.avisoSesion)?.let { Aviso(it, TonoChip.ROJO, Icons.Outlined.ErrorOutline) }
        CampoTexto("Correo", correo, { correo = it; errCorreo = null; error = null }, placeholder = "tucorreo@ejemplo.com",
            iconoInicio = Icons.Outlined.Email, teclado = KeyboardType.Email, error = errCorreo)
        CampoTexto("Contraseña", clave, { clave = it; errClave = null; error = null }, placeholder = "••••••••",
            iconoInicio = Icons.Outlined.Lock, contrasena = true, error = errClave)

        BotonPrincipal("Iniciar sesión", {
            errCorreo = if (!correoValido(correo)) "Escribe un correo válido" else null
            errClave = if (clave.length < 6) "La contraseña tiene mínimo 6 caracteres" else null
            if (errCorreo == null && errClave == null) {
                entrando = true
                error = null
                scope.launch {
                    estado.iniciarSesion(correo, clave).onFailure {
                        error = it.mensaje()
                        entrando = false
                    }
                }
            }
        }, Modifier.fillMaxWidth(), cargando = entrando)
        BotonSecundario("¿Olvidaste tu contraseña?", onClickForgotPassword, Modifier.fillMaxWidth())

        HorizontalDivider(color = Line)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("¿No tienes cuenta? ", color = Muted)
            Text("Regístrate", color = Blue, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onClickRegister() })
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    VistaPrevia { LoginScreen(onClickRegister = {}, onClickForgotPassword = {}) }
}
