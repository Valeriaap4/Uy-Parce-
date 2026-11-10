package me.valeriaacosta.uyparceshopper.ui.screens.supervisor

import me.valeriaacosta.uyparceshopper.data.*

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.valeriaacosta.uyparceshopper.data.DatosEjemplo
import me.valeriaacosta.uyparceshopper.data.models.EstadoCaja
import me.valeriaacosta.uyparceshopper.ui.components.TarjetaCaja
import me.valeriaacosta.uyparceshopper.ui.components.TituloSeccion
import me.valeriaacosta.uyparceshopper.ui.components.UpScreen
import me.valeriaacosta.uyparceshopper.ui.theme.UyParceTheme

// ─── CajasEnCaminoScreen ─────────────────────────────────────────────────────

/** Pantalla 27. Cajas que vienen hacia Cúcuta y las que ya llegaron y faltan por separar. */
@Composable
fun CajasEnCaminoScreen(
    onClickBack: (() -> Unit)? = null,
    onClickCaja: (codigo: String) -> Unit
) {
    val enCamino = DatosEjemplo.cajas.filter { it.estado == EstadoCaja.ENVIADA }
    val recibidas = DatosEjemplo.cajas.filter { it.estado == EstadoCaja.EN_CUCUTA }

    UpScreen("Recepción", onClickBack, subtitulo = "Cajas que llegan a Cúcuta") {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { TituloSeccion("En camino (${enCamino.size})") }
            items(enCamino, key = { it.codigo }) { c -> TarjetaCaja(c) { onClickCaja(c.codigo) } }
            item { TituloSeccion("Recibidas, por separar (${recibidas.size})") }
            items(recibidas, key = { it.codigo }) { c -> TarjetaCaja(c) { onClickCaja(c.codigo) } }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CajasEnCaminoScreenPreview() {
    UyParceTheme { CajasEnCaminoScreen(onClickCaja = {}) }
}
