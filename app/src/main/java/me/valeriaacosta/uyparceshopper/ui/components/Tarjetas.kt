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

// ─── Tarjetas y etiquetas ───

@Composable
fun Tarjeta(modifier: Modifier = Modifier, alPulsar: (() -> Unit)? = null, contenido: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, Line, RoundedCornerShape(20.dp))
            .then(if (alPulsar != null) Modifier.clickable(onClick = alPulsar) else Modifier)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = contenido
    )
}

@Composable
fun TituloSeccion(texto: String, modifier: Modifier = Modifier, accion: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(texto, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        accion?.invoke()
    }
}

enum class TonoChip(val fondo: Color, val texto: Color) {
    VERDE(GreenSoft, Green), AZUL(BlueSoft, Blue), AMARILLO(AmberSoft, Amber),
    MORADO(PurpleSoft, Purple), ROJO(RedSoft, Red), GRIS(GreySoft, Grey)
}

@Composable
fun Chip(texto: String, tono: TonoChip) {
    Text(
        texto.uppercase(),
        color = tono.texto, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(tono.fondo).padding(horizontal = 12.dp, vertical = 5.dp)
    )
}

fun tonoDe(estado: EstadoPedido): TonoChip = when (estado) {
    EstadoPedido.PENDIENTE_PAGO -> TonoChip.AMARILLO
    EstadoPedido.PAGO_EN_REVISION -> TonoChip.MORADO
    EstadoPedido.PAGADO, EstadoPedido.COMPRADO -> TonoChip.AZUL
    EstadoPedido.EN_CAMINO, EstadoPedido.EN_CUCUTA, EstadoPedido.ENVIADO -> TonoChip.AZUL
    EstadoPedido.ENTREGADO -> TonoChip.VERDE
    EstadoPedido.CANCELADO -> TonoChip.ROJO
}

@Composable
fun ChipEstado(estado: EstadoPedido) = Chip(estado.etiqueta, tonoDe(estado))

/** Monto en letra monoespaciada con la moneda pequeña al lado: "$106.80 USD". */
@Composable
fun Dinero(valor: Double, moneda: Moneda, tamano: TextUnit = 17.sp, color: Color = Ink) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(formatoDinero(valor, moneda), fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = tamano, color = color)
        Spacer(Modifier.width(4.dp))
        Text(moneda.name, fontSize = (tamano.value * 0.7f).sp, color = Muted, modifier = Modifier.padding(bottom = 1.dp))
    }
}

@Composable
fun FilaDato(etiqueta: String, valor: String, negrita: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Text(etiqueta, color = Muted, modifier = Modifier.weight(1f))
        Text(valor, fontWeight = if (negrita) FontWeight.Bold else FontWeight.SemiBold)
    }
}

@Composable
fun FilaDinero(etiqueta: String, valor: Double, moneda: Moneda, total: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(etiqueta, color = if (total) Ink else Muted, fontWeight = if (total) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
        Dinero(valor, moneda, if (total) 22.sp else 16.sp)
    }
}

/** Pestañas tipo píldora (como "1 · Comprar  2 · Verificar  3 · Productos"). */
@Composable
fun PestanasPildora(opciones: List<String>, seleccion: Int, alElegir: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        opciones.forEachIndexed { i, texto ->
            val activa = i == seleccion
            Text(
                texto,
                color = if (activa) Color.White else Navy,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(50))
                    .background(if (activa) Navy else Color.White)
                    .border(1.dp, if (activa) Navy else Line, RoundedCornerShape(50))
                    .clickable { alElegir(i) }
                    .padding(horizontal = 18.dp, vertical = 11.dp)
            )
        }
    }
}

/** Foto del producto. Mientras no hay fotos reales se dibuja un bloque de color con la inicial. */
@Composable
fun FotoProducto(producto: Producto, modifier: Modifier = Modifier, redondeo: Dp = 16.dp, marcarAgotado: Boolean = false) {
    Box(
        modifier.clip(RoundedCornerShape(redondeo)).background(Color(producto.colorFoto)),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Outlined.Checkroom, null, tint = Color.White.copy(alpha = 0.55f), modifier = Modifier.fillMaxSize(0.4f))
        if (marcarAgotado && producto.agotado) {
            Text("AGOTADO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                    .clip(RoundedCornerShape(6.dp)).background(Red).padding(horizontal = 6.dp, vertical = 2.dp))
        }
    }
}

/** Fila de menú con icono y flecha (pantallas "Más" y "Ajustes"). */
@Composable
fun FilaMenu(icono: ImageVector, titulo: String, detalle: String? = null, alPulsar: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White)
            .border(1.dp, Line, RoundedCornerShape(16.dp))
            .clickable(onClick = alPulsar).padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icono, null, tint = Navy)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(titulo, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            if (detalle != null) Text(detalle, color = Muted, style = MaterialTheme.typography.bodyMedium)
        }
        Icon(Icons.Outlined.ChevronRight, null, tint = Muted)
    }
}

@Composable
fun EstadoVacio(icono: ImageVector, texto: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icono, null, tint = Muted, modifier = Modifier.size(48.dp))
        Text(texto, color = Muted)
    }
}

/** Aviso destacado (amarillo, rojo o verde). */
@Composable
fun Aviso(texto: String, tono: TonoChip = TonoChip.AMARILLO, icono: ImageVector = Icons.Outlined.Info) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(tono.fondo).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icono, null, tint = tono.texto)
        Text(texto, color = tono.texto, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

/** "1 prenda", "3 prendas", "1 pedido", "2 pedidos"… */
fun cantidad(n: Int, singular: String): String = if (n == 1) "1 $singular" else "$n ${singular}s"
