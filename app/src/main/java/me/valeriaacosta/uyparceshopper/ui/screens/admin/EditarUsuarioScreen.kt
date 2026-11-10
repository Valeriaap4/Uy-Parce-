package me.valeriaacosta.uyparceshopper.ui.screens.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import me.valeriaacosta.uyparceshopper.data.DatosEjemplo
import me.valeriaacosta.uyparceshopper.data.models.Rol
import me.valeriaacosta.uyparceshopper.data.models.TipoCliente
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── EditarUsuarioScreen ─────────────────────────────────────────────────────

/** Pantalla 44. Crear o editar un usuario: rol, tipo de cliente y si está activo. */
@Composable
fun EditarUsuarioScreen(
    id: String?,
    onClickBack: () -> Unit,
    onGuardado: () -> Unit
) {
    val u = (DatosEjemplo.equipo + DatosEjemplo.clientes).firstOrNull { it.id == id }
    val mensaje = LocalMensaje.current
    var nombre by remember { mutableStateOf(u?.nombre ?: "") }
    var correo by remember { mutableStateOf(u?.correo ?: "") }
    var rol by remember { mutableStateOf(u?.rol?.etiqueta) }
    var tipo by remember { mutableStateOf(u?.tipoCliente?.etiqueta) }
    var activo by remember { mutableStateOf(u?.activo ?: true) }
    var errores by remember { mutableStateOf(mapOf<String, String>()) }

    UpScreen(if (u == null) "Agregar usuario" else "Editar usuario", onClickBack, barraInferior = {
        BotonPrincipal("Guardar", {
            val e = mutableMapOf<String, String>()
            if (nombre.trim().split(" ").size < 2) e["n"] = "Escribe nombre y apellido"
            if (!correoValido(correo)) e["c"] = "Correo no válido"
            if (rol == null) e["r"] = "Elige un rol"
            if (rol == Rol.CLIENTE.etiqueta && tipo == null) e["t"] = "Elige el tipo de cliente"
            errores = e
            if (e.isEmpty()) {
                mensaje("Usuario guardado")
                onGuardado()
            }
        }, Modifier.fillMaxWidth())
    }) {
        ContenidoScroll {
            CampoTexto("Nombre completo", nombre, { errores = errores - "n"; nombre = it }, error = errores["n"])
            CampoTexto("Correo", correo, { errores = errores - "c"; correo = it }, teclado = KeyboardType.Email, error = errores["c"])
            Selector("Rol", Rol.entries.map { it.etiqueta }, rol, { errores = errores - "r"; rol = it }, error = errores["r"])
            if (rol == Rol.CLIENTE.etiqueta)
                Selector("Tipo de cliente", TipoCliente.entries.map { it.etiqueta }, tipo, { errores = errores - "t"; tipo = it }, error = errores["t"])
            if (u == null) Text("Le llegará un correo para crear su contraseña.", color = Muted, fontSize = 13.sp)
            Tarjeta {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Usuario activo", fontWeight = FontWeight.Bold)
                        Text("Si lo desactivas no podrá iniciar sesión.", color = Muted, fontSize = 13.sp)
                    }
                    Switch(
                        checked = activo, onCheckedChange = { activo = it },
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
    UyParceTheme { EditarUsuarioScreen(id = null, onClickBack = {}, onGuardado = {}) }
}
