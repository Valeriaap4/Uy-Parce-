package me.valeriaacosta.uyparceshopper.ui.screens.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.UyParceTheme

// ─── ForgotPasswordScreen ────────────────────────────────────────────────────

/** Pantalla 5. Recuperar contraseña (Firebase envía el enlace). */
@Composable
fun ForgotPasswordScreen(
    onClickBack: () -> Unit
) {
    var correo by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var enviado by remember { mutableStateOf(false) }

    FondoMarca("Recupera tu contraseña", "Te enviamos un enlace para crear una nueva") {
        if (enviado) {
            Aviso("Si $correo está registrado, te llegará un correo con el enlace.", TonoChip.VERDE, Icons.Outlined.MarkEmailRead)
            BotonPrincipal("Volver a iniciar sesión", onClickBack, Modifier.fillMaxWidth())
        } else {
            CampoTexto("Correo", correo, { correo = it; error = null }, teclado = KeyboardType.Email,
                iconoInicio = Icons.Outlined.Email, error = error)
            BotonPrincipal("Enviar enlace", {
                if (correoValido(correo)) enviado = true else error = "Escribe un correo válido"
            }, Modifier.fillMaxWidth())
            BotonSecundario("Cancelar", onClickBack, Modifier.fillMaxWidth())
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ForgotPasswordScreenPreview() {
    UyParceTheme { ForgotPasswordScreen(onClickBack = {}) }
}
