package me.valeriaacosta.uyparceshopper.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

// ─── PapeleraScreen ──────────────────────────────────────────────────────────

/** Pantalla 53. Elementos eliminados que solo el admin puede restaurar. */
@Composable
fun PapeleraScreen(
    onClickBack: () -> Unit
) {
    val mensaje = LocalMensaje.current
    val elementos = remember { mutableStateListOf(*DatosEjemplo.papelera.toTypedArray()) }

    UpScreen("Papelera", onClickBack, subtitulo = "Solo el administrador puede restaurar") {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(elementos, key = { it.nombre }) { e ->
                Tarjeta {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Chip(e.tipo, TonoChip.GRIS)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(e.nombre, fontWeight = FontWeight.Bold)
                            Text("${e.eliminadoPor} · ${e.fecha}", color = Muted, fontSize = 14.sp)
                        }
                        TextButton(onClick = { elementos.remove(e); mensaje("${e.tipo} restaurado") }) {
                            Icon(Icons.Outlined.RestoreFromTrash, null, tint = Blue)
                            Text(" Restaurar", color = Blue)
                        }
                    }
                }
            }
            if (elementos.isEmpty()) item { EstadoVacio(Icons.Outlined.DeleteOutline, "La papelera está vacía") }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PapeleraScreenPreview() {
    UyParceTheme { PapeleraScreen(onClickBack = {}) }
}
