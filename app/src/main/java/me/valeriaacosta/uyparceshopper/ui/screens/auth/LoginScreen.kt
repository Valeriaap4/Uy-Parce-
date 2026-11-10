package me.valeriaacosta.uyparceshopper.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.valeriaacosta.uyparceshopper.data.DatosEjemplo
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.models.Rol
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── LoginScreen ─────────────────────────────────────────────────────────────

/** Pantalla 2. Inicio de sesión con validación de correo y contraseña. */
@Composable
fun LoginScreen(
    onLogin: (rol: Rol) -> Unit,
    onClickRegister: () -> Unit,
    onClickForgotPassword: () -> Unit
) {
    val estado = LocalEstadoApp.current
    var correo by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var errCorreo by remember { mutableStateOf<String?>(null) }
    var errClave by remember { mutableStateOf<String?>(null) }

    fun entrar(rol: Rol) {
        estado.entrarComo(rol)
        onLogin(rol)
    }

    FondoMarca(
        titulo = "UyParce Shopper",
        subtitulo = "Encarga ropa de outlets de EE. UU. y recíbela en Colombia",
        pie = "Modo demo: los datos son de ejemplo hasta conectar Firebase"
    ) {
        Text("Inicia sesión para continuar", style = MaterialTheme.typography.titleLarge)
        CampoTexto("Correo", correo, { correo = it; errCorreo = null }, placeholder = "tucorreo@ejemplo.com",
            iconoInicio = Icons.Outlined.Email, teclado = KeyboardType.Email, error = errCorreo)
        CampoTexto("Contraseña", clave, { clave = it; errClave = null }, placeholder = "••••••••",
            iconoInicio = Icons.Outlined.Lock, contrasena = true, error = errClave)

        BotonPrincipal("Iniciar sesión", {
            errCorreo = if (!correoValido(correo)) "Escribe un correo válido" else null
            errClave = if (clave.length < 6) "La contraseña tiene mínimo 6 caracteres" else null
            if (errCorreo == null && errClave == null) {
                // Demo: el rol sale del correo de ejemplo; si no coincide, entra como cliente.
                val rol = listOf(DatosEjemplo.admin, DatosEjemplo.operador, DatosEjemplo.supervisor)
                    .firstOrNull { it.correo.equals(correo.trim(), ignoreCase = true) }?.rol ?: Rol.CLIENTE
                entrar(rol)
            }
        }, Modifier.fillMaxWidth())
        BotonSecundario("¿Olvidaste tu contraseña?", onClickForgotPassword, Modifier.fillMaxWidth())

        HorizontalDivider(color = Line)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("¿No tienes cuenta? ", color = Muted)
            Text("Regístrate", color = Blue, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onClickRegister() })
        }

        // ── Accesos rápidos para probar cada rol ──
        HorizontalDivider(color = Line)
        EtiquetaCampo("Entrar rápido (demo)")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Rol.entries.forEach { rol ->
                Text(
                    text = if (rol == Rol.ADMIN) "Admin" else rol.etiqueta,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy, textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Field)
                        .clickable { entrar(rol) }
                        .padding(vertical = 10.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    UyParceTheme { LoginScreen(onLogin = {}, onClickRegister = {}, onClickForgotPassword = {}) }
}
