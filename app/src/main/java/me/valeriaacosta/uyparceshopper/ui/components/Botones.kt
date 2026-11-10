package me.valeriaacosta.uyparceshopper.ui.components

import me.valeriaacosta.uyparceshopper.data.*

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

// ─── Botones ───

/*
 * cargando = true: el botón muestra una rueda y no se puede tocar. Así, mientras se guarda algo en
 * Firebase, un doble toque no crea dos pedidos o dos pagos.
 */

@Composable
fun BotonPrincipal(
    texto: String, alPulsar: () -> Unit, modifier: Modifier = Modifier, icono: ImageVector? = null,
    habilitado: Boolean = true, cargando: Boolean = false
) {
    Button(
        onClick = alPulsar, enabled = habilitado && !cargando,
        modifier = modifier.heightIn(min = 54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Navy, contentColor = Color.White)
    ) {
        ContenidoBoton(texto, icono, cargando, Color.White)
    }
}

@Composable
fun BotonAmarillo(
    texto: String, alPulsar: () -> Unit, modifier: Modifier = Modifier, icono: ImageVector? = null,
    habilitado: Boolean = true, cargando: Boolean = false
) {
    Button(
        onClick = alPulsar, enabled = habilitado && !cargando, modifier = modifier.heightIn(min = 54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Yellow, contentColor = Navy)
    ) {
        ContenidoBoton(texto, icono, cargando, Navy)
    }
}

@Composable
fun BotonSecundario(
    texto: String, alPulsar: () -> Unit, modifier: Modifier = Modifier, icono: ImageVector? = null,
    color: Color = Navy, habilitado: Boolean = true, cargando: Boolean = false
) {
    OutlinedButton(
        onClick = alPulsar, enabled = habilitado && !cargando, modifier = modifier.heightIn(min = 54.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Line),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = color)
    ) {
        ContenidoBoton(texto, icono, cargando, color)
    }
}

@Composable
private fun RowScope.ContenidoBoton(texto: String, icono: ImageVector?, cargando: Boolean, colorRueda: Color) {
    if (cargando) {
        CircularProgressIndicator(Modifier.size(20.dp), color = colorRueda, strokeWidth = 2.5.dp)
        Spacer(Modifier.width(10.dp))
    } else if (icono != null) {
        Icon(icono, null)
        Spacer(Modifier.width(8.dp))
    }
    Text(texto, fontWeight = FontWeight.Bold, fontSize = 16.sp)
}
