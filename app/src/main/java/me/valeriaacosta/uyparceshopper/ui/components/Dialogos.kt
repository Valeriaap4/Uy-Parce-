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
