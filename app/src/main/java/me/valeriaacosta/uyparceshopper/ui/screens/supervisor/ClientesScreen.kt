package me.valeriaacosta.uyparceshopper.ui.screens.supervisor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.valeriaacosta.uyparceshopper.data.Listas
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.models.Rol
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── ClientesScreen ──────────────────────────────────────────────────────────

/** Pantalla 33. Clientes aprobados (en vivo) con buscador. */
@Composable
fun ClientesScreen(
    onClickBack: () -> Unit,
    onClickCliente: (id: String) -> Unit
) {
    val estado = LocalEstadoApp.current
    var buscar by rememberSaveable { mutableStateOf("") }
    val lista = estado.usuarios
        .filter { it.rol == Rol.CLIENTE && it.aprobado }
        .filter { buscar.isBlank() || it.nombre.contains(buscar, true) || it.ciudad.contains(buscar, true) || it.cedula.contains(buscar) }

    UpScreen("Clientes", onClickBack) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { BarraBuscar(buscar, { buscar = it }, "Buscar por nombre, ciudad o cédula") }
            item { Text(cantidad(lista.size, "cliente"), fontWeight = FontWeight.Bold) }
            items(lista, key = { it.id }) { c ->
                Tarjeta(alPulsar = { onClickCliente(c.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Avatar(c.iniciales, 42.dp, if (c.activo) BlueSoft else GreySoft, Blue)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(c.nombre, fontWeight = FontWeight.Bold)
                            Text("${c.ciudad} · ${c.telefono}", color = Muted, fontSize = 14.sp)
                        }
                        Chip(
                            if (!c.activo) "Inactivo" else c.tipoCliente?.etiqueta?.removePrefix("Mayorista ") ?: "Sin tipo",
                            if (!c.activo) TonoChip.GRIS else if (c.tipoCliente?.esMayorista == true) TonoChip.AZUL else TonoChip.VERDE
                        )
                    }
                }
            }
            if (lista.isEmpty()) item {
                if (estado.cargado(Listas.USUARIOS)) EstadoVacio(Icons.Outlined.Group, "No hay clientes aquí") else Cargando()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ClientesScreenPreview() {
    VistaPrevia(Rol.SUPERVISOR) { ClientesScreen(onClickBack = {}, onClickCliente = {}) }
}
