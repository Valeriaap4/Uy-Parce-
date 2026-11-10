package me.valeriaacosta.uyparceshopper.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.valeriaacosta.uyparceshopper.data.DatosEjemplo
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.models.Rol
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── PerfilScreen ────────────────────────────────────────────────────────────

/** Pantalla 6. Perfil del usuario (todos los roles). */
@Composable
fun PerfilScreen(
    onClickBack: (() -> Unit)? = null,
    onClickNotificaciones: () -> Unit,
    onLogout: () -> Unit
) {
    val estado = LocalEstadoApp.current
    val u = estado.usuario ?: DatosEjemplo.laura
    val mensaje = LocalMensaje.current
    var telefono by remember { mutableStateOf(u.telefono) }
    var direccion by remember { mutableStateOf(u.direccion) }
    var salir by remember { mutableStateOf(false) }

    UpScreen("Perfil", onClickBack, cabecera = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Avatar(u.iniciales, 56.dp)
            Column {
                Text(u.nombre, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                Text(u.correo, color = NavyText)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    u.tipoCliente?.etiqueta ?: u.rol.etiqueta, color = Navy, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(Yellow).padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    }) {
        ContenidoScroll {
            if (u.rol == Rol.CLIENTE) {
                if (u.saldoAFavor > 0) Aviso("Tienes saldo a favor para tu próximo pedido.", TonoChip.VERDE, Icons.Outlined.Savings)
                Tarjeta {
                    FilaDato("Cédula", u.cedula)
                    FilaDato("Ciudad", u.ciudad)
                    FilaDato("Zona de envío", DatosEjemplo.zonaDe(u.ciudad).nombre)
                }
                CampoTexto("Teléfono", telefono, { telefono = it }, teclado = KeyboardType.Phone)
                CampoTexto("Dirección de entrega", direccion, { direccion = it }, lineas = 2,
                    error = if (direccion.length < 8) "Escribe la dirección completa" else null)
                BotonPrincipal("Guardar cambios", { if (direccion.length >= 8) mensaje("Datos guardados") }, Modifier.fillMaxWidth())
                FilaMenu(Icons.Outlined.Notifications, "Notificaciones", "Avisos de tus pedidos", onClickNotificaciones)
            } else {
                Tarjeta {
                    FilaDato("Rol", u.rol.etiqueta)
                    FilaDato("Estado", if (u.activo) "Activo" else "Inactivo")
                }
            }
            BotonSecundario("Cerrar sesión", { salir = true }, Modifier.fillMaxWidth(), Icons.AutoMirrored.Outlined.Logout, color = Red)
            Text("UyParce Shopper · versión 0.1 (Beta)", color = Muted, fontSize = 13.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
    if (salir) DialogoConfirmar("Cerrar sesión", "¿Seguro que quieres salir?", "Salir", {
        estado.cerrarSesion(); onLogout()
    }, { salir = false }, peligro = true)
}

@Preview(showBackground = true)
@Composable
fun PerfilScreenPreview() {
    UyParceTheme { PerfilScreen(onClickNotificaciones = {}, onLogout = {}) }
}
