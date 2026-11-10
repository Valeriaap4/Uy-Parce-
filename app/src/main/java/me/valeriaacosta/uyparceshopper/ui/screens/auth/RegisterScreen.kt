package me.valeriaacosta.uyparceshopper.ui.screens.auth

import me.valeriaacosta.uyparceshopper.data.*

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import me.valeriaacosta.uyparceshopper.data.DatosEjemplo
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.Muted
import me.valeriaacosta.uyparceshopper.ui.theme.UyParceTheme

// ─── RegisterScreen ──────────────────────────────────────────────────────────

/** Pantalla 3. Registro de cliente. Queda pendiente de aprobación del admin. */
@Composable
fun RegisterScreen(
    onClickBack: () -> Unit,
    onRegistrado: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var cedula by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var clave2 by remember { mutableStateOf("") }
    var ciudad by remember { mutableStateOf<String?>(null) }
    var direccion by remember { mutableStateOf("") }
    var errores by remember { mutableStateOf(mapOf<String, String>()) }

    FondoMarca("Crea tu cuenta", "El equipo de Uy Parce revisa y aprueba cada cuenta") {
        CampoTexto("Nombre completo", nombre, { errores = errores - "nombre"; nombre = it }, placeholder = "Como aparece en tu cédula", error = errores["nombre"])
        CampoTexto("Cédula", cedula, { errores = errores - "cedula"; cedula = it.filter(Char::isDigit) }, teclado = KeyboardType.Number, error = errores["cedula"])
        CampoTexto("Teléfono / WhatsApp", telefono, { errores = errores - "telefono"; telefono = it.filter { c -> c.isDigit() || c == ' ' } },
            teclado = KeyboardType.Phone, placeholder = "300 000 0000", error = errores["telefono"])
        CampoTexto("Correo", correo, { errores = errores - "correo"; correo = it }, teclado = KeyboardType.Email, error = errores["correo"])
        CampoTexto("Contraseña", clave, { errores = errores - "clave"; clave = it }, contrasena = true, error = errores["clave"])
        CampoTexto("Repite la contraseña", clave2, { errores = errores - "clave2"; clave2 = it }, contrasena = true, error = errores["clave2"])
        Selector("Ciudad de entrega", DatosEjemplo.ciudades, ciudad, { errores = errores - "ciudad"; ciudad = it }, error = errores["ciudad"])
        ciudad?.let { Text("Zona de envío: ${DatosEjemplo.zonaDe(it).nombre}", color = Muted, fontSize = 13.sp) }
        CampoTexto("Dirección de entrega", direccion, { errores = errores - "direccion"; direccion = it }, placeholder = "Calle, número, barrio, apto",
            lineas = 2, error = errores["direccion"])

        BotonPrincipal("Crear cuenta", {
            val e = mutableMapOf<String, String>()
            if (nombre.trim().split(" ").size < 2) e["nombre"] = "Escribe nombre y apellido"
            if (cedula.length !in 6..10) e["cedula"] = "La cédula tiene entre 6 y 10 dígitos"
            if (telefono.filter(Char::isDigit).length != 10) e["telefono"] = "El celular tiene 10 dígitos"
            if (!correoValido(correo)) e["correo"] = "Escribe un correo válido"
            if (clave.length < 6) e["clave"] = "Mínimo 6 caracteres"
            if (clave2 != clave) e["clave2"] = "Las contraseñas no coinciden"
            if (ciudad == null) e["ciudad"] = "Elige tu ciudad"
            if (direccion.length < 8) e["direccion"] = "Escribe la dirección completa"
            errores = e
            if (e.isEmpty()) onRegistrado()
        }, Modifier.fillMaxWidth())
        BotonSecundario("Ya tengo cuenta", onClickBack, Modifier.fillMaxWidth())
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    UyParceTheme { RegisterScreen(onClickBack = {}, onRegistrado = {}) }
}
