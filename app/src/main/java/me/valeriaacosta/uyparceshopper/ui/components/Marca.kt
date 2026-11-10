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

// ─── Marca ───

@Composable
fun LogoUyParce(modifier: Modifier = Modifier, alto: Dp = 120.dp) {
    Image(
        painter = painterResource(R.drawable.logo_uyparce),
        contentDescription = "Logo Uy Parce",
        contentScale = ContentScale.Fit,
        modifier = modifier.height(alto).aspectRatio(160f / 121f).clip(RoundedCornerShape(20.dp))
    )
}

@Composable
fun Avatar(iniciales: String, tamano: Dp = 44.dp, fondo: Color = Yellow, texto: Color = Navy) {
    Box(Modifier.size(tamano).clip(CircleShape).background(fondo), contentAlignment = Alignment.Center) {
        Text(iniciales.uppercase(), color = texto, fontWeight = FontWeight.ExtraBold, fontSize = (tamano.value * 0.36f).sp)
    }
}
