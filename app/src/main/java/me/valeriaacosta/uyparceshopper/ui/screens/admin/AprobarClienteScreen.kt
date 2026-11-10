package me.valeriaacosta.uyparceshopper.ui.screens.admin

import me.valeriaacosta.uyparceshopper.data.*

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HowToReg
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import me.valeriaacosta.uyparceshopper.data.DatosEjemplo
import me.valeriaacosta.uyparceshopper.data.models.TipoCliente
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.Muted
import me.valeriaacosta.uyparceshopper.ui.theme.Red
import me.valeriaacosta.uyparceshopper.ui.theme.UyParceTheme

// ─── AprobarClienteScreen ────────────────────────────────────────────────────

/** Pantalla 42. El admin revisa el registro, asigna el tipo de cliente y aprueba o rechaza. */
@Composable
fun AprobarClienteScreen(
    id: String,
    onClickBack: () -> Unit,
    onListo: () -> Unit
) {
    val c = DatosEjemplo.porAprobar.firstOrNull { it.id == id } ?: return NoEncontrado("Aprobar cliente", onClickBack)
    val mensaje = LocalMensaje.current
    var tipo by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var rechazar by remember { mutableStateOf(false) }

    UpScreen("Aprobar cliente", onClickBack, barraInferior = {
        BotonSecundario("Rechazar", { rechazar = true }, Modifier.weight(1f), color = Red)
        BotonPrincipal("Aprobar", {
            if (tipo == null) error = "Asigna el tipo de cliente"
            else {
                mensaje("${c.nombre} aprobado como $tipo")
                onListo()
            }
        }, Modifier.weight(1.4f), Icons.Outlined.HowToReg)
    }) {
        ContenidoScroll {
            Tarjeta {
                FilaDato("Nombre", c.nombre, true)
                FilaDato("Cédula", c.cedula)
                FilaDato("Teléfono", c.telefono)
                FilaDato("Correo", c.correo)
                FilaDato("Ciudad", c.ciudad)
                FilaDato("Dirección", c.direccion)
            }
            Selector("Tipo de cliente", TipoCliente.entries.map { it.etiqueta }, tipo, { tipo = it; error = null }, error = error)
            Text("Mayorista Avanzado 20 % · Parcero 25 % (USD). Detal sin comisión (COP).", color = Muted, fontSize = 13.sp)
        }
    }
    if (rechazar) DialogoConfirmar(
        "Rechazar registro", "${c.nombre} no podrá entrar a la app.", "Rechazar",
        { mensaje("Registro rechazado"); onListo() }, { rechazar = false }, peligro = true
    )
}

@Preview(showBackground = true)
@Composable
fun AprobarClienteScreenPreview() {
    UyParceTheme { AprobarClienteScreen(id = "c5", onClickBack = {}, onListo = {}) }
}
