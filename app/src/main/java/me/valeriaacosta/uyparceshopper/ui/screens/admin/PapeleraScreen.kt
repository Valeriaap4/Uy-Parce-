package me.valeriaacosta.uyparceshopper.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
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
import kotlinx.coroutines.launch
import me.valeriaacosta.uyparceshopper.data.Listas
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.fecha
import me.valeriaacosta.uyparceshopper.data.mensaje
import me.valeriaacosta.uyparceshopper.data.models.ElementoPapelera
import me.valeriaacosta.uyparceshopper.data.models.Rol
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── PapeleraScreen ──────────────────────────────────────────────────────────

/** Pantalla 53. Lo eliminado (productos y cajas vacías). Solo el admin restaura o borra para siempre. */
@Composable
fun PapeleraScreen(
    onClickBack: () -> Unit
) {
    val estado = LocalEstadoApp.current
    val mensaje = LocalMensaje.current
    val scope = rememberCoroutineScope()
    var borrar by remember { mutableStateOf<ElementoPapelera?>(null) }
    var trabajando by remember { mutableStateOf<String?>(null) }

    fun accion(e: ElementoPapelera, bloque: suspend () -> Result<Unit>, ok: String) {
        trabajando = e.id
        scope.launch {
            bloque().onSuccess { mensaje(ok) }.onFailure { mensaje(it.mensaje()) }
            trabajando = null
        }
    }

    UpScreen("Papelera", onClickBack, subtitulo = "Solo el administrador puede restaurar") {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(estado.papelera, key = { it.id }) { e ->
                Tarjeta {
                    Chip(e.tipo, TonoChip.GRIS)
                    Text(e.nombre, fontWeight = FontWeight.Bold)
                    Text("Eliminado por ${e.eliminadoPor} · ${fecha(e.fecha)}", color = Muted, fontSize = 14.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { accion(e, { estado.restaurar(e) }, "${e.tipo} restaurado") }, enabled = trabajando == null) {
                            Icon(Icons.Outlined.RestoreFromTrash, null, tint = Blue)
                            Text(" Restaurar", color = Blue)
                        }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { borrar = e }, enabled = trabajando == null) {
                            Icon(Icons.Outlined.DeleteForever, null, tint = Red)
                            Text(" Borrar", color = Red)
                        }
                    }
                }
            }
            if (estado.papelera.isEmpty()) item {
                if (estado.cargado(Listas.PAPELERA)) EstadoVacio(Icons.Outlined.DeleteOutline, "La papelera está vacía") else Cargando()
            }
        }
    }

    borrar?.let { e ->
        DialogoConfirmar(
            "Borrar para siempre", "${e.tipo} \"${e.nombre}\" se borra definitivamente (también sus fotos). No se puede deshacer.",
            "Borrar", { accion(e, { estado.eliminarDefinitivo(e) }, "Borrado definitivamente") }, { borrar = null }, peligro = true
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PapeleraScreenPreview() {
    VistaPrevia(Rol.ADMIN) { PapeleraScreen(onClickBack = {}) }
}
