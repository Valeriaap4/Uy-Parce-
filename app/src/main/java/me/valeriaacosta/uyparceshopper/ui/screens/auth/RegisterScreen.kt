package me.valeriaacosta.uyparceshopper.ui.screens.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.mensaje
import me.valeriaacosta.uyparceshopper.data.models.Usuario
import me.valeriaacosta.uyparceshopper.data.models.Zonas
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.Muted

// ─── RegisterScreen ──────────────────────────────────────────────────────────

/**
 * Pantalla 3. Registro de cliente: crea la cuenta en Firebase y sus datos. Queda pendiente hasta que
 * el admin la apruebe (la navegación lleva sola a "cuenta en revisión").
 */
@Composable
fun RegisterScreen(
    onClickBack: () -> Unit
) {
    val estado = LocalEstadoApp.current
    val scope = rememberCoroutineScope()
    var nombre by rememberSaveable { mutableStateOf("") }
    var cedula by rememberSaveable { mutableStateOf("") }
    var telefono by rememberSaveable { mutableStateOf("") }
    var correo by rememberSaveable { mutableStateOf("") }
    var clave by rememberSaveable { mutableStateOf("") }
    var clave2 by rememberSaveable { mutableStateOf("") }
    var ciudad by rememberSaveable { mutableStateOf<String?>(null) }
    var direccion by rememberSaveable { mutableStateOf("") }
    var errores by remember { mutableStateOf(mapOf<String, String>()) }
    var error by remember { mutableStateOf<String?>(null) }
    var creando by remember { mutableStateOf(false) }

    FondoMarca("Crea tu cuenta", "El equipo de Uy Parce revisa y aprueba cada cuenta") {
        error?.let { Aviso(it, TonoChip.ROJO, Icons.Outlined.ErrorOutline) }
        CampoTexto("Nombre completo", nombre, { errores = errores - "nombre"; nombre = it }, placeholder = "Como aparece en tu cédula", error = errores["nombre"])
        CampoTexto("Cédula", cedula, { errores = errores - "cedula"; cedula = it.filter(Char::isDigit) }, teclado = KeyboardType.Number, error = errores["cedula"])
        CampoTexto("Teléfono / WhatsApp", telefono, { errores = errores - "telefono"; telefono = it.filter { c -> c.isDigit() || c == ' ' } },
            teclado = KeyboardType.Phone, placeholder = "300 000 0000", error = errores["telefono"])
        CampoTexto("Correo", correo, { errores = errores - "correo"; error = null; correo = it }, teclado = KeyboardType.Email, error = errores["correo"])
        CampoTexto("Contraseña", clave, { errores = errores - "clave"; clave = it }, contrasena = true, error = errores["clave"])
        CampoTexto("Repite la contraseña", clave2, { errores = errores - "clave2"; clave2 = it }, contrasena = true, error = errores["clave2"])
        Selector("Ciudad de entrega", Zonas.ciudades, ciudad, { errores = errores - "ciudad"; ciudad = it }, error = errores["ciudad"])
        ciudad?.let { Text("Zona de envío: ${Zonas.nombreDe(it)}", color = Muted, fontSize = 13.sp) }
        CampoTexto("Dirección de entrega", direccion, { errores = errores - "direccion"; direccion = it }, placeholder = "Calle, número, barrio, apto",
            lineas = 2, error = errores["direccion"])

        BotonPrincipal("Crear cuenta", {
            val e = mutableMapOf<String, String>()
            if (nombre.trim().split(" ").filter { it.isNotBlank() }.size < 2) e["nombre"] = "Escribe nombre y apellido"
            if (cedula.length !in 6..10) e["cedula"] = "La cédula tiene entre 6 y 10 dígitos"
            if (telefono.filter(Char::isDigit).length != 10) e["telefono"] = "El celular tiene 10 dígitos"
            if (!correoValido(correo)) e["correo"] = "Escribe un correo válido"
            if (clave.length < 6) e["clave"] = "Mínimo 6 caracteres"
            if (clave2 != clave) e["clave2"] = "Las contraseñas no coinciden"
            if (ciudad == null) e["ciudad"] = "Elige tu ciudad"
            if (direccion.trim().length < 8) e["direccion"] = "Escribe la dirección completa"
            errores = e
            val ciudadElegida = ciudad
            if (e.isEmpty() && ciudadElegida != null) {
                creando = true
                error = null
                val datos = Usuario(
                    nombre = nombre.trim().replace(Regex("\\s+"), " "), correo = correo.trim(), cedula = cedula,
                    telefono = telefono.trim(), ciudad = ciudadElegida, direccion = direccion.trim()
                )
                scope.launch {
                    // Si sale bien, la sesión queda "en revisión" y la navegación cambia de pantalla sola.
                    estado.registrar(datos, clave).onFailure {
                        error = it.mensaje()
                        creando = false
                    }
                }
            }
        }, Modifier.fillMaxWidth(), cargando = creando)
        BotonSecundario("Ya tengo cuenta", onClickBack, Modifier.fillMaxWidth())
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    VistaPrevia { RegisterScreen(onClickBack = {}) }
}
