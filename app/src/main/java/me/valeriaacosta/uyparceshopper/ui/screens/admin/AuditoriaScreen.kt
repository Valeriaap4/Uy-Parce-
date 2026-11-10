package me.valeriaacosta.uyparceshopper.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.fechaHoraCorta
import me.valeriaacosta.uyparceshopper.data.mensaje
import me.valeriaacosta.uyparceshopper.data.models.RegistroAuditoria
import me.valeriaacosta.uyparceshopper.data.models.Rol
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── AuditoriaScreen ─────────────────────────────────────────────────────────

/** Pantalla 54. Registro de quién hizo qué y cuándo (los últimos 200, solo lectura). */
@Composable
fun AuditoriaScreen(
    onClickBack: () -> Unit
) {
    val estado = LocalEstadoApp.current
    val mensaje = LocalMensaje.current
    var buscar by rememberSaveable { mutableStateOf("") }
    val registros by produceState<List<RegistroAuditoria>?>(null) {
        value = estado.auditoria().onFailure { mensaje(it.mensaje()) }.getOrDefault(emptyList())
    }
    val lista = registros.orEmpty().filter {
        buscar.isBlank() || it.accion.contains(buscar, true) || it.detalle.contains(buscar, true) || it.usuario.contains(buscar, true)
    }

    UpScreen("Auditoría", onClickBack, subtitulo = "Registro de solo lectura") {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { BarraBuscar(buscar, { buscar = it }, "Buscar por acción, detalle o persona") }
            items(lista, key = { it.id }) { r ->
                Tarjeta {
                    Row {
                        Text(r.accion, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(fechaHoraCorta(r.fecha), color = Muted, fontSize = 13.sp)
                    }
                    Text(r.detalle, color = Muted)
                    Text(r.usuario, color = Blue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            if (lista.isEmpty()) item {
                if (registros == null) Cargando() else EstadoVacio(Icons.Outlined.History, "No hay registros")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AuditoriaScreenPreview() {
    VistaPrevia(Rol.ADMIN) { AuditoriaScreen(onClickBack = {}) }
}
