package me.valeriaacosta.uyparceshopper.ui.screens.operador

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.valeriaacosta.uyparceshopper.data.Listas
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.models.Caja
import me.valeriaacosta.uyparceshopper.data.models.EstadoCaja
import me.valeriaacosta.uyparceshopper.data.models.Rol
import me.valeriaacosta.uyparceshopper.ui.components.*

// ─── CajasScreen ─────────────────────────────────────────────────────────────

/** Pantalla 22. Cajas abiertas (por empacar, en vivo) y enviadas (incluye las ya recibidas). */
@Composable
fun CajasScreen(
    onClickBack: (() -> Unit)? = null,
    onClickCaja: (codigo: String) -> Unit,
    onClickCrear: () -> Unit
) {
    val estado = LocalEstadoApp.current
    var filtro by rememberSaveable { mutableIntStateOf(0) }
    val activas = estado.cajas
    val recibidas by produceState<List<Caja>?>(null, filtro) {
        if (filtro == 1) value = estado.cajasRecibidas().getOrDefault(emptyList())
    }
    val lista = if (filtro == 0) activas.filter { it.estado == EstadoCaja.ABIERTA }
    else activas.filter { it.estado != EstadoCaja.ABIERTA } + recibidas.orEmpty()

    UpScreen("Cajas", onClickBack, cabecera = {
        ResumenCabecera(
            "${activas.count { it.estado == EstadoCaja.ABIERTA }}" to "Abiertas",
            "${activas.count { it.estado == EstadoCaja.ENVIADA }}" to "En camino",
            "${activas.count { it.estado == EstadoCaja.EN_CUCUTA }}" to "En Cúcuta"
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
            if (lista.isEmpty()) item {
                val cargando = !estado.cargado(Listas.CAJAS) || (filtro == 1 && recibidas == null)
                if (cargando) Cargando() else EstadoVacio(Icons.Outlined.Inventory2, "No hay cajas aquí")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CajasScreenPreview() {
    VistaPrevia(Rol.OPERADOR) { CajasScreen(onClickCaja = {}, onClickCrear = {}) }
}
