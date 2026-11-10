package me.valeriaacosta.uyparceshopper.ui.screens.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.mensaje
import me.valeriaacosta.uyparceshopper.data.models.Rol
import me.valeriaacosta.uyparceshopper.data.models.TipoCliente
import me.valeriaacosta.uyparceshopper.data.models.Usuario
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── EditarUsuarioScreen ─────────────────────────────────────────────────────

/**
 * Pantalla 44. Sin id: agrega a alguien del equipo (Firebase le envía un correo para crear su
 * contraseña). Con id: edita nombre, rol, tipo de cliente y si la cuenta está activa.
 * Los clientes no se crean aquí: se registran solos desde la app.
 */
@Composable
fun EditarUsuarioScreen(
    id: String?,
    onClickBack: () -> Unit,
    onGuardado: () -> Unit
) {
    if (id == null) FormularioUsuario(null, onClickBack, onGuardado)
    else SegunCarga(rememberUsuario(id), "Editar usuario", onClickBack) { u -> FormularioUsuario(u, onClickBack, onGuardado) }
}

@Composable
private fun FormularioUsuario(u: Usuario?, onClickBack: () -> Unit, onGuardado: () -> Unit) {
    val estado = LocalEstadoApp.current
    val context = LocalContext.current
    val mensaje = LocalMensaje.current
    val scope = rememberCoroutineScope()
    val esNuevo = u == null
    val soyYo = u?.id == estado.usuario?.id
    // Al crear solo se ofrecen roles del equipo (los clientes se registran solos).
    val roles = if (esNuevo) Rol.entries.filter { it != Rol.CLIENTE } else Rol.entries.toList()

    var nombre by rememberSaveable(u?.id) { mutableStateOf(u?.nombre ?: "") }
    var correo by rememberSaveable(u?.id) { mutableStateOf(u?.correo ?: "") }
    var rol by rememberSaveable(u?.id) { mutableStateOf(u?.rol) }
    var tipo by rememberSaveable(u?.id) { mutableStateOf(u?.tipoCliente) }
    var activo by rememberSaveable(u?.id) { mutableStateOf(u?.activo ?: true) }
    var errores by remember { mutableStateOf(mapOf<String, String>()) }
    var guardando by remember { mutableStateOf(false) }

    UpScreen(if (esNuevo) "Agregar usuario" else "Editar usuario", onClickBack, barraInferior = {
        BotonPrincipal("Guardar", {
            val e = mutableMapOf<String, String>()
            if (nombre.trim().split(" ").filter { it.isNotBlank() }.size < 2) e["n"] = "Escribe nombre y apellido"
            if (esNuevo && !correoValido(correo)) e["c"] = "Correo no válido"
            if (rol == null) e["r"] = "Elige un rol"
            if (rol == Rol.CLIENTE && tipo == null) e["t"] = "Elige el tipo de cliente"
            errores = e
            val rolElegido = rol
            if (e.isEmpty() && rolElegido != null) {
                guardando = true
                scope.launch {
                    val resultado = if (u == null) estado.crearUsuarioEquipo(context, nombre, correo, rolElegido)
                    else estado.editarUsuario(u, nombre, rolElegido, tipo, activo)
                    resultado
                        .onSuccess {
                            mensaje(if (esNuevo) "Cuenta creada: le enviamos un correo a ${correo.trim()} para crear su contraseña" else "Usuario guardado")
                            onGuardado()
                        }
                        .onFailure { mensaje(it.mensaje()) }
                    guardando = false
                }
            }
        }, Modifier.fillMaxWidth(), cargando = guardando)
    }) {
        ContenidoScroll {
            CampoTexto("Nombre completo", nombre, { errores = errores - "n"; nombre = it }, error = errores["n"])
            CampoTexto("Correo", correo, { errores = errores - "c"; correo = it }, teclado = KeyboardType.Email,
                error = errores["c"], soloLectura = !esNuevo)
            if (!esNuevo) Text("El correo no se puede cambiar: es con el que la persona inicia sesión.", color = Muted, fontSize = 13.sp)
            Selector("Rol", roles.map { it.etiqueta }, rol?.etiqueta,
                { elegido -> errores = errores - "r"; rol = Rol.entries.first { it.etiqueta == elegido } }, error = errores["r"])
            if (rol == Rol.CLIENTE)
                Selector("Tipo de cliente", TipoCliente.entries.map { it.etiqueta }, tipo?.etiqueta,
                    { elegido -> errores = errores - "t"; tipo = TipoCliente.entries.first { it.etiqueta == elegido } }, error = errores["t"])
            if (esNuevo) Text("Le llegará un correo para crear su contraseña (puede caer en spam).", color = Muted, fontSize = 13.sp)
            if (soyYo) Aviso("Esta es tu cuenta: no puedes quitarte el rol de administrador ni desactivarte.", TonoChip.AZUL)
            if (!esNuevo) Tarjeta {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Usuario activo", fontWeight = FontWeight.Bold)
                        Text("Si lo desactivas no podrá iniciar sesión.", color = Muted, fontSize = 13.sp)
                    }
                    Switch(
                        checked = activo, onCheckedChange = { activo = it }, enabled = !soyYo,
                        colors = SwitchDefaults.colors(checkedTrackColor = Navy, checkedThumbColor = Yellow)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EditarUsuarioScreenPreview() {
    VistaPrevia(Rol.ADMIN) { EditarUsuarioScreen(id = null, onClickBack = {}, onGuardado = {}) }
}
