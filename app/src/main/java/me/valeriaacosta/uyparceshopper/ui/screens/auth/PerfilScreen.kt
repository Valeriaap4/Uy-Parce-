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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.mensaje
import me.valeriaacosta.uyparceshopper.data.models.*
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── PerfilScreen ────────────────────────────────────────────────────────────

/**
 * Pantalla 6. Perfil del usuario (todos los roles). El cliente edita aquí su teléfono, ciudad y
 * dirección de entrega (los pedidos nuevos usan estos datos).
 * onGuardado: se usa cuando se abre desde "Cambiar dirección" para volver al pedido después de guardar.
 */
@Composable
fun PerfilScreen(
    onClickBack: (() -> Unit)? = null,
    onClickNotificaciones: () -> Unit,
    onLogout: () -> Unit,
    onGuardado: (() -> Unit)? = null
) {
    val u = LocalEstadoApp.current.usuario
    if (u == null) {
        UpScreen("Perfil", onClickBack) { Cargando() }
        return
    }
    PerfilContenido(u, onClickBack, onClickNotificaciones, onLogout, onGuardado)
}

@Composable
private fun PerfilContenido(
    u: Usuario,
    onClickBack: (() -> Unit)?,
    onClickNotificaciones: () -> Unit,
    onLogout: () -> Unit,
    onGuardado: (() -> Unit)?
) {
    val estado = LocalEstadoApp.current
    val mensaje = LocalMensaje.current
    val scope = rememberCoroutineScope()
    var telefono by rememberSaveable(u.id) { mutableStateOf(u.telefono) }
    var ciudad by rememberSaveable(u.id) { mutableStateOf(u.ciudad) }
    var direccion by rememberSaveable(u.id) { mutableStateOf(u.direccion) }
    var errores by remember { mutableStateOf(mapOf<String, String>()) }
    var guardando by remember { mutableStateOf(false) }
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
                val saldo = estado.miSaldo
                if (saldo > 0) Aviso(
                    "Tienes ${formatoDinero(saldo, u.moneda)} ${u.moneda} de saldo a favor: se descuenta solo en tu próximo pedido.",
                    TonoChip.VERDE, Icons.Outlined.Savings
                )
                Tarjeta { FilaDato("Cédula", u.cedula) }
                CampoTexto("Teléfono / WhatsApp", telefono, { errores = errores - "telefono"; telefono = it.filter { c -> c.isDigit() || c == ' ' } },
                    teclado = KeyboardType.Phone, error = errores["telefono"])
                Selector("Ciudad de entrega", Zonas.ciudades, ciudad.ifBlank { null }, { errores = errores - "ciudad"; ciudad = it }, error = errores["ciudad"])
                if (ciudad.isNotBlank()) {
                    val zona = estado.ajustes.zonaDe(ciudad)
                    Text("Zona de envío: ${zona.nombre} · ${formatoDinero(zona.precioCop.toDouble(), Moneda.COP)} COP", color = Muted, fontSize = 13.sp)
                }
                CampoTexto("Dirección de entrega", direccion, { errores = errores - "direccion"; direccion = it }, lineas = 2,
                    placeholder = "Calle, número, barrio, apto", error = errores["direccion"])
                val hayCambios = telefono.trim() != u.telefono || ciudad != u.ciudad || direccion.trim() != u.direccion
                BotonPrincipal("Guardar cambios", {
                    val e = mutableMapOf<String, String>()
                    if (telefono.filter(Char::isDigit).length != 10) e["telefono"] = "El celular tiene 10 dígitos"
                    if (ciudad.isBlank()) e["ciudad"] = "Elige tu ciudad"
                    if (direccion.trim().length < 8) e["direccion"] = "Escribe la dirección completa"
                    errores = e
                    if (e.isEmpty()) {
                        guardando = true
                        scope.launch {
                            estado.actualizarMiContacto(telefono, ciudad, direccion)
                                .onSuccess { mensaje("Datos guardados"); onGuardado?.invoke() }
                                .onFailure { mensaje(it.mensaje()) }
                            guardando = false
                        }
                    }
                }, Modifier.fillMaxWidth(), habilitado = hayCambios, cargando = guardando)
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
    if (salir) DialogoConfirmar("Cerrar sesión", "¿Seguro que quieres salir?", "Salir", onLogout, { salir = false }, peligro = true)
}

@Preview(showBackground = true)
@Composable
fun PerfilScreenPreview() {
    VistaPrevia { PerfilScreen(onClickNotificaciones = {}, onLogout = {}) }
}
