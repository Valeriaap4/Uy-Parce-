package me.valeriaacosta.uyparceshopper.ui.screens.operador

import me.valeriaacosta.uyparceshopper.data.*

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.valeriaacosta.uyparceshopper.data.DatosEjemplo
import me.valeriaacosta.uyparceshopper.data.models.EstadoCaja
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.UyParceTheme

// ─── CajasScreen ─────────────────────────────────────────────────────────────

/** Pantalla 22. Cajas abiertas (por empacar) y enviadas. */
@Composable
fun CajasScreen(
    onClickBack: (() -> Unit)? = null,
    onClickCaja: (codigo: String) -> Unit,
    onClickCrear: () -> Unit
) {
    var filtro by remember { mutableIntStateOf(0) }
    val lista = DatosEjemplo.cajas.filter {
        if (filtro == 0) it.estado == EstadoCaja.ABIERTA else it.estado != EstadoCaja.ABIERTA
    }

    UpScreen("Cajas", onClickBack, cabecera = {
        ResumenCabecera(
            "${DatosEjemplo.cajas.count { it.estado == EstadoCaja.ABIERTA }}" to "Abiertas",
            "${DatosEjemplo.cajas.count { it.estado == EstadoCaja.ENVIADA }}" to "En camino",
            "${DatosEjemplo.cajas.count { it.estado == EstadoCaja.EN_CUCUTA }}" to "En Cúcuta"
        )
    }, barraInferior = {
        BotonPrincipal("Crear caja", onClickCrear, Modifier.fillMaxWidth(), Icons.Outlined.Add)
    }) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { PestanasPildora(listOf("Abiertas", "Enviadas"), filtro, { filtro = it }) }
            items(lista, key = { it.codigo }) { c ->
                TarjetaCaja(c) { if (c.estado == EstadoCaja.ABIERTA) onClickCaja(c.codigo) }
            }
            if (lista.isEmpty()) item { EstadoVacio(Icons.Outlined.Inventory2, "No hay cajas aquí") }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CajasScreenPreview() {
    UyParceTheme { CajasScreen(onClickCaja = {}, onClickCrear = {}) }
}
