package me.valeriaacosta.uyparceshopper.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HowToReg
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.valeriaacosta.uyparceshopper.data.Listas
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.hace
import me.valeriaacosta.uyparceshopper.data.models.Rol
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── PorAprobarScreen ────────────────────────────────────────────────────────

/** Pantalla 41. Clientes que se registraron y esperan aprobación (se actualiza en vivo). */
@Composable
fun PorAprobarScreen(
    onClickCliente: (id: String) -> Unit
) {
    val estado = LocalEstadoApp.current
    val pendientes = estado.clientesPorAprobar.sortedBy { it.creadoEn }

    UpScreen("Por aprobar", subtitulo = "Registros nuevos de clientes") {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(pendientes, key = { it.id }) { c ->
                Tarjeta(alPulsar = { onClickCliente(c.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Avatar(c.iniciales, 42.dp, AmberSoft, Amber)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(c.nombre, fontWeight = FontWeight.Bold)
                            Text("${c.ciudad} · ${c.correo}", color = Muted, fontSize = 14.sp)
                            Text("Se registró ${hace(c.creadoEn)}", color = Muted, fontSize = 13.sp)
                        }
                        Chip("Nuevo", TonoChip.AMARILLO)
                    }
                }
            }
            if (pendientes.isEmpty()) item {
                if (estado.cargado(Listas.USUARIOS)) EstadoVacio(Icons.Outlined.HowToReg, "No hay registros por aprobar")
                else Cargando()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PorAprobarScreenPreview() {
    VistaPrevia(Rol.ADMIN) { PorAprobarScreen(onClickCliente = {}) }
}
