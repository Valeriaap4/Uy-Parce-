package me.valeriaacosta.uyparceshopper.ui.screens.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.mensaje
import me.valeriaacosta.uyparceshopper.ui.components.*

// ─── ForgotPasswordScreen ────────────────────────────────────────────────────

/**
 * Pantalla 5. Recuperar contraseña: Firebase envía un correo (en español) con el enlace.
 * El mensaje no dice si el correo existe, para no revelar quién tiene cuenta.
 */
@Composable
fun ForgotPasswordScreen(
    onClickBack: () -> Unit
) {
    val estado = LocalEstadoApp.current
    val scope = rememberCoroutineScope()
    var correo by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var enviado by rememberSaveable { mutableStateOf(false) }
    var enviando by remember { mutableStateOf(false) }

    FondoMarca("Recupera tu contraseña", "Te enviamos un enlace para crear una nueva") {
        if (enviado) {
            Aviso("Si ${correo.trim()} tiene una cuenta, te llegará un correo con el enlace. Revisa también la carpeta de spam.",
                TonoChip.VERDE, Icons.Outlined.MarkEmailRead)
            BotonPrincipal("Volver a iniciar sesión", onClickBack, Modifier.fillMaxWidth())
        } else {
            CampoTexto("Correo", correo, { correo = it; error = null }, teclado = KeyboardType.Email,
                iconoInicio = Icons.Outlined.Email, error = error)
            BotonPrincipal("Enviar enlace", {
                if (!correoValido(correo)) error = "Escribe un correo válido"
                else {
                    enviando = true
                    scope.launch {
                        estado.recuperarClave(correo)
                            .onSuccess { enviado = true }
                            .onFailure { error = it.mensaje() }
                        enviando = false
                    }
                }
            }, Modifier.fillMaxWidth(), cargando = enviando)
            BotonSecundario("Cancelar", onClickBack, Modifier.fillMaxWidth())
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ForgotPasswordScreenPreview() {
    VistaPrevia { ForgotPasswordScreen(onClickBack = {}) }
}
