package me.valeriaacosta.uyparceshopper.ui.screens.supervisor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.valeriaacosta.uyparceshopper.data.DatosEjemplo
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── ClientesScreen ──────────────────────────────────────────────────────────

/** Pantalla 33. Base de datos de clientes con buscador. */
@Composable
fun ClientesScreen(
    onClickBack: () -> Unit,
    onClickCliente: (id: String) -> Unit
) {
    var buscar by remember { mutableStateOf("") }
    val lista = DatosEjemplo.clientes.filter { it.nombre.contains(buscar, true) || it.ciudad.contains(buscar, true) }

    UpScreen("Clientes", onClickBack) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { BarraBuscar(buscar, { buscar = it }, "Buscar por nombre o ciudad") }
            item { Text("${lista.size} clientes", fontWeight = FontWeight.Bold) }
            items(lista, key = { it.id }) { c ->
                Tarjeta(alPulsar = { onClickCliente(c.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Avatar(c.iniciales, 42.dp, BlueSoft, Blue)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(c.nombre, fontWeight = FontWeight.Bold)
                            Text("${c.ciudad} · ${c.telefono}", color = Muted, fontSize = 14.sp)
                        }
                        Chip(
                            c.tipoCliente?.etiqueta?.replace("Mayorista ", "") ?: "",
                            if (c.tipoCliente?.esMayorista == true) TonoChip.AZUL else TonoChip.VERDE
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ClientesScreenPreview() {
    UyParceTheme { ClientesScreen(onClickBack = {}, onClickCliente = {}) }
}
