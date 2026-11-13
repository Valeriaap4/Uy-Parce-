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
import me.valeriaacosta.uyparceshopper.data.models.Zonas
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── EditarUsuarioScreen ─────────────────────────────────────────────────────

/**
 * Pantalla 44. Sin id: crea una cuenta del equipo o de un cliente (el cliente queda aprobado de una vez)
 * y Firebase le envía a la persona un correo para crear su contraseña. Con id: edita nombre, rol, tipo de
 * cliente y si la cuenta está activa.
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

    var nombre by rememberSaveable(u?.id) { mutableStateOf(u?.nombre ?: "") }
    var correo by rememberSaveable(u?.id) { mutableStateOf(u?.correo ?: "") }
    var rol by rememberSaveable(u?.id) { mutableStateOf(u?.rol) }
    var tipo by rememberSaveable(u?.id) { mutableStateOf(u?.tipoCliente) }
    var activo by rememberSaveable(u?.id) { mutableStateOf(u?.activo ?: true) }
    // Datos de entrega: solo al crear un cliente (después los cambian el mismo cliente o el supervisor).
    var cedula by rememberSaveable { mutableStateOf("") }
    var telefono by rememberSaveable { mutableStateOf("") }
    var ciudad by rememberSaveable { mutableStateOf<String?>(null) }
    var direccion by rememberSaveable { mutableStateOf("") }
    var errores by remember { mutableStateOf(mapOf<String, String>()) }
    var guardando by remember { mutableStateOf(false) }
    val clienteNuevo = esNuevo && rol == Rol.CLIENTE

    UpScreen(if (esNuevo) "Agregar usuario" else "Editar usuario", onClickBack, barraInferior = {
        BotonPrincipal("Guardar", {
            val e = mutableMapOf<String, String>()
            if (nombre.trim().split(" ").filter { it.isNotBlank() }.size < 2) e["n"] = "Escribe nombre y apellido"
            if (esNuevo && !correoValido(correo)) e["c"] = "Correo no válido"
            if (rol == null) e["r"] = "Elige un rol"
            if (rol == Rol.CLIENTE && tipo == null) e["t"] = "Elige el tipo de cliente"
            if (clienteNuevo) {
                if (cedula.length !in 6..10) e["cedula"] = "La cédula tiene entre 6 y 10 dígitos"
                if (telefono.filter(Char::isDigit).length != 10) e["telefono"] = "El celular tiene 10 dígitos"
                if (ciudad == null) e["ciudad"] = "Elige la ciudad"
                if (direccion.trim().length < 8) e["direccion"] = "Escribe la dirección completa"
            }
            errores = e
            val rolElegido = rol
            if (e.isEmpty() && rolElegido != null) {
                guardando = true
                scope.launch {
                    val resultado = if (u == null) estado.crearUsuario(context, datosNuevos(
                        nombre, correo, rolElegido, tipo, cedula, telefono, ciudad.orEmpty(), direccion
                    ))
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
            Selector("Rol", Rol.entries.map { it.etiqueta }, rol?.etiqueta,
                { elegido -> errores = errores - "r"; rol = Rol.entries.first { it.etiqueta == elegido } }, error = errores["r"])
            if (rol == Rol.CLIENTE)
                Selector("Tipo de cliente", TipoCliente.entries.map { it.etiqueta }, tipo?.etiqueta,
                    { elegido -> errores = errores - "t"; tipo = TipoCliente.entries.first { it.etiqueta == elegido } }, error = errores["t"])
            if (clienteNuevo) {
                CampoTexto("Cédula", cedula, { errores = errores - "cedula"; cedula = it.filter(Char::isDigit) },
                    teclado = KeyboardType.Number, error = errores["cedula"])
                CampoTexto("Teléfono / WhatsApp", telefono, { errores = errores - "telefono"; telefono = it.filter { c -> c.isDigit() || c == ' ' } },
                    teclado = KeyboardType.Phone, placeholder = "300 000 0000", error = errores["telefono"])
                Selector("Ciudad de entrega", Zonas.ciudades, ciudad, { errores = errores - "ciudad"; ciudad = it }, error = errores["ciudad"])
                ciudad?.let { Text("Zona de envío: ${Zonas.nombreDe(it)}", color = Muted, fontSize = 13.sp) }
                CampoTexto("Dirección de entrega", direccion, { errores = errores - "direccion"; direccion = it },
                    placeholder = "Calle, número, barrio, apto", lineas = 2, error = errores["direccion"])
                Text("El cliente queda aprobado de una vez, con el tipo que elijas.", color = Muted, fontSize = 13.sp)
            }
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

/** Datos de la cuenta nueva: los de entrega solo se guardan si es un cliente. */
private fun datosNuevos(
    nombre: String, correo: String, rol: Rol, tipo: TipoCliente?,
    cedula: String, telefono: String, ciudad: String, direccion: String
): Usuario {
    val esCliente = rol == Rol.CLIENTE
    return Usuario(
        nombre = nombre.trim().replace(Regex("\\s+"), " "),
        correo = correo.trim(),
        rol = rol,
        tipoCliente = if (esCliente) tipo else null,
        cedula = if (esCliente) cedula else "",
        telefono = if (esCliente) telefono.trim() else "",
        ciudad = if (esCliente) ciudad else "",
        direccion = if (esCliente) direccion.trim() else ""
    )
}

@Preview(showBackground = true)
@Composable
fun EditarUsuarioScreenPreview() {
    VistaPrevia(Rol.ADMIN) { EditarUsuarioScreen(id = null, onClickBack = {}, onGuardado = {}) }
}
