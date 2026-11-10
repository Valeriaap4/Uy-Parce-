package me.valeriaacosta.uyparceshopper.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.valeriaacosta.uyparceshopper.R
import me.valeriaacosta.uyparceshopper.data.models.EstadoPedido
import me.valeriaacosta.uyparceshopper.data.models.Moneda
import me.valeriaacosta.uyparceshopper.data.models.Producto
import me.valeriaacosta.uyparceshopper.data.models.formatoDinero
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── Escáner ───

/**
 * Escáner de códigos de barras.
 * Por ahora simula la lectura; en la siguiente fase aquí se llama al Google Code Scanner (ML Kit),
 * que abre la cámara y devuelve el código leído.
 */
@Composable
fun DialogoEscaner(alLeer: (String) -> Unit, alCerrar: () -> Unit, codigoSimulado: String = "0888407612345") {
    var manual by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = alCerrar,
        containerColor = Color.White,
        title = { Text("Escanear código", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(16.dp)).background(Navy),
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.fillMaxWidth(0.75f).height(90.dp).border(3.dp, Yellow, RoundedCornerShape(12.dp)))
                    Text("Apunta al código de barras", color = NavyText, fontSize = 13.sp,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(10.dp))
                }
                CampoTexto("O escríbelo", manual, { manual = it.filter(Char::isLetterOrDigit) },
                    placeholder = "Código de barras", teclado = KeyboardType.Number)
            }
        },
        confirmButton = {
            TextButton(onClick = { alLeer(manual.ifBlank { codigoSimulado }) }) {
                Text(if (manual.isBlank()) "Simular lectura" else "Usar código", fontWeight = FontWeight.Bold, color = Blue)
            }
        },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar", color = Muted) } }
    )
}

/** Diálogo de confirmación para acciones que no se pueden deshacer. */
@Composable
fun DialogoConfirmar(titulo: String, texto: String, confirmar: String, alConfirmar: () -> Unit, alCerrar: () -> Unit, peligro: Boolean = false) {
    AlertDialog(
        onDismissRequest = alCerrar,
        containerColor = Color.White,
        title = { Text(titulo, fontWeight = FontWeight.Bold) },
        text = { Text(texto) },
        confirmButton = {
            TextButton(onClick = { alConfirmar(); alCerrar() }) {
                Text(confirmar, fontWeight = FontWeight.Bold, color = if (peligro) Red else Blue)
            }
        },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar", color = Muted) } }
    )
}

/** Muestra un mensaje corto abajo ("Guardado", "Pago aprobado"…). Lo provee el NavHost. */
val LocalMensaje = staticCompositionLocalOf<(String) -> Unit> { {} }
