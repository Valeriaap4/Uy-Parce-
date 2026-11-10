package me.valeriaacosta.uyparceshopper.ui.screens.supervisor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoveToInbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.valeriaacosta.uyparceshopper.data.Listas
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.models.EstadoCaja
import me.valeriaacosta.uyparceshopper.data.models.Rol
import me.valeriaacosta.uyparceshopper.ui.components.*

// ─── CajasEnCaminoScreen ─────────────────────────────────────────────────────

/** Pantalla 27. Cajas que vienen hacia Cúcuta y las que ya llegaron y se están separando (en vivo). */
@Composable
fun CajasEnCaminoScreen(
    onClickBack: (() -> Unit)? = null,
    onClickCaja: (codigo: String) -> Unit
) {
    val estado = LocalEstadoApp.current
    val enCamino = estado.cajas.filter { it.estado == EstadoCaja.ENVIADA }.sortedBy { it.fechaEnvio }
    val recibiendo = estado.cajas.filter { it.estado == EstadoCaja.EN_CUCUTA }

    UpScreen("Recepción", onClickBack, subtitulo = "Cajas que llegan a Cúcuta") {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { TituloSeccion("En camino (${enCamino.size})") }
            items(enCamino, key = { it.codigo }) { c -> TarjetaCaja(c) { onClickCaja(c.codigo) } }
            item { TituloSeccion("Llegaron, por separar (${recibiendo.size})") }
            items(recibiendo, key = { it.codigo }) { c -> TarjetaCaja(c) { onClickCaja(c.codigo) } }
            if (enCamino.isEmpty() && recibiendo.isEmpty()) item {
                if (estado.cargado(Listas.CAJAS)) EstadoVacio(Icons.Outlined.MoveToInbox, "No hay cajas en camino") else Cargando()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CajasEnCaminoScreenPreview() {
    VistaPrevia(Rol.SUPERVISOR) { CajasEnCaminoScreen(onClickCaja = {}) }
}
