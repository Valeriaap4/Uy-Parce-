package me.valeriaacosta.uyparceshopper.ui.screens.admin

import me.valeriaacosta.uyparceshopper.data.*

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonAdd
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

// ─── UsuariosScreen ──────────────────────────────────────────────────────────

/** Pantalla 43. Equipo y clientes, activos e inactivos. */
@Composable
fun UsuariosScreen(
    onClickUsuario: (id: String?) -> Unit
) {
    var filtro by remember { mutableIntStateOf(0) }
    val lista = if (filtro == 0) DatosEjemplo.equipo else DatosEjemplo.clientes

    UpScreen("Usuarios", barraInferior = {
        BotonPrincipal("Agregar usuario", { onClickUsuario(null) }, Modifier.fillMaxWidth(), Icons.Outlined.PersonAdd)
    }) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { PestanasPildora(listOf("Equipo", "Clientes"), filtro, { filtro = it }) }
            items(lista, key = { it.id }) { u ->
                Tarjeta(alPulsar = { onClickUsuario(u.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Avatar(u.iniciales, 42.dp, if (u.activo) Yellow else GreySoft)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(u.nombre, fontWeight = FontWeight.Bold)
                            Text(u.correo, color = Muted, fontSize = 14.sp)
                        }
                        Chip(
                            u.tipoCliente?.etiqueta?.replace("Mayorista ", "") ?: u.rol.etiqueta,
                            if (u.activo) TonoChip.AZUL else TonoChip.GRIS
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun UsuariosScreenPreview() {
    UyParceTheme { UsuariosScreen(onClickUsuario = {}) }
}
