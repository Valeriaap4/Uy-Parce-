package me.valeriaacosta.uyparceshopper.ui.screens.supervisor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.fecha
import me.valeriaacosta.uyparceshopper.data.mensaje
import me.valeriaacosta.uyparceshopper.data.models.Gasto
import me.valeriaacosta.uyparceshopper.data.models.Rol
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── GastosScreen ────────────────────────────────────────────────────────────

/** Pantalla 37. Gastos del negocio (los más recientes primero) con filtro por categoría. */
@Composable
fun GastosScreen(
    onClickBack: () -> Unit,
    onClickNuevo: () -> Unit
) {
    val estado = LocalEstadoApp.current
    val mensaje = LocalMensaje.current
    var categoria by rememberSaveable { mutableStateOf<String?>(null) }
    // Se consultan al abrir la pantalla (y al volver de registrar uno nuevo).
    val gastos by produceState<List<Gasto>?>(null) {
        value = estado.gastosRecientes().onFailure { mensaje(it.mensaje()) }.getOrDefault(emptyList())
    }
    val lista = gastos.orEmpty().filter { categoria == null || categoria == "Todas" || it.categoria == categoria }

    UpScreen("Gastos", onClickBack, barraInferior = {
        BotonPrincipal("Registrar gasto", onClickNuevo, Modifier.fillMaxWidth(), Icons.Outlined.Add)
    }) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Selector("Categoría", listOf("Todas") + Gasto.categorias, categoria, { categoria = it }, placeholder = "Todas")
            }
            items(lista, key = { it.id }) { g ->
                Tarjeta {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(g.descripcion, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Dinero(g.monto, g.moneda, 15.sp)
                    }
                    Text(listOf(g.proveedor, fecha(g.fecha), g.creadoPor).filter { it.isNotBlank() }.joinToString(" · "), color = Muted, fontSize = 14.sp)
                    Chip(g.categoria, TonoChip.GRIS)
                }
            }
            if (lista.isEmpty()) item {
                if (gastos == null) Cargando() else EstadoVacio(Icons.Outlined.RequestQuote, "No hay gastos en esta categoría")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GastosScreenPreview() {
    VistaPrevia(Rol.SUPERVISOR) { GastosScreen(onClickBack = {}, onClickNuevo = {}) }
}
