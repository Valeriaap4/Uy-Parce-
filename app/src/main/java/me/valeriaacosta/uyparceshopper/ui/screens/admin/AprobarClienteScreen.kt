package me.valeriaacosta.uyparceshopper.ui.screens.admin

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HowToReg
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.fechaHora
import me.valeriaacosta.uyparceshopper.data.mensaje
import me.valeriaacosta.uyparceshopper.data.models.*
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.Muted
import me.valeriaacosta.uyparceshopper.ui.theme.Red

// ─── AprobarClienteScreen ────────────────────────────────────────────────────

/** Pantalla 42. El admin revisa el registro, asigna el tipo de cliente y aprueba o rechaza. */
@Composable
fun AprobarClienteScreen(
    id: String,
    onClickBack: () -> Unit,
    onListo: () -> Unit
) {
    SegunCarga(rememberUsuario(id), "Aprobar cliente", onClickBack) { c ->
        AprobarClienteContenido(c, onClickBack, onListo)
    }
}

@Composable
private fun AprobarClienteContenido(c: Usuario, onClickBack: () -> Unit, onListo: () -> Unit) {
    val estado = LocalEstadoApp.current
    val mensaje = LocalMensaje.current
    val scope = rememberCoroutineScope()
    val a = estado.ajustes
    var tipo by remember { mutableStateOf<TipoCliente?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var rechazar by remember { mutableStateOf(false) }
    var guardando by remember { mutableStateOf(false) }
    val pendiente = !c.aprobado && c.activo

    UpScreen("Aprobar cliente", onClickBack, barraInferior = if (!pendiente) null else ({
        BotonSecundario("Rechazar", { rechazar = true }, Modifier.weight(1f), color = Red, habilitado = !guardando)
        BotonPrincipal("Aprobar", {
            val elegido = tipo
            if (elegido == null) error = "Asigna el tipo de cliente"
            else {
                guardando = true
                scope.launch {
                    estado.aprobarCliente(c, elegido)
                        .onSuccess { mensaje("${c.nombre} aprobado como ${elegido.etiqueta}"); onListo() }
                        .onFailure { mensaje(it.mensaje()) }
                    guardando = false
                }
            }
        }, Modifier.weight(1.4f), Icons.Outlined.HowToReg, cargando = guardando)
    })) {
        ContenidoScroll {
            if (!pendiente) Aviso(
                if (c.aprobado) "Esta cuenta ya está aprobada (${c.tipoCliente?.etiqueta ?: c.rol.etiqueta})." else "Este registro fue rechazado.",
                TonoChip.AZUL, Icons.Outlined.Info
            )
            Tarjeta {
                FilaDato("Nombre", c.nombre, true)
                FilaDato("Cédula", c.cedula)
                FilaDato("Teléfono", c.telefono)
                FilaDato("Correo", c.correo)
                FilaDato("Ciudad", c.ciudad)
                FilaDato("Dirección", c.direccion)
                FilaDato("Se registró", fechaHora(c.creadoEn))
            }
            if (pendiente) {
                Selector("Tipo de cliente", TipoCliente.entries.map { it.etiqueta }, tipo?.etiqueta,
                    { elegido -> tipo = TipoCliente.entries.first { it.etiqueta == elegido }; error = null }, error = error)
                Text(
                    "Mayorista Avanzado ${porcentaje(a.comisionAvanzado)} · Parcero ${porcentaje(a.comisionParcero)} (USD). " +
                        "Detal sin comisión (COP).",
                    color = Muted, fontSize = 13.sp
                )
            }
        }
    }
    if (rechazar) DialogoConfirmar(
        "Rechazar registro", "${c.nombre} no podrá entrar a la app.", "Rechazar",
        {
            guardando = true
            scope.launch {
                estado.rechazarCliente(c)
                    .onSuccess { mensaje("Registro rechazado"); onListo() }
                    .onFailure { mensaje(it.mensaje()) }
                guardando = false
            }
        }, { rechazar = false }, peligro = true
    )
}

@Preview(showBackground = true)
@Composable
fun AprobarClienteScreenPreview() {
    VistaPrevia(Rol.ADMIN) { AprobarClienteScreen(id = "c5", onClickBack = {}, onListo = {}) }
}
