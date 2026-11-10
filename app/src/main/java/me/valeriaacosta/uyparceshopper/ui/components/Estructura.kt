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

// ─── Estructura de pantalla ───

/**
 * Esqueleto de todas las pantallas: cabecera azul marino (como en los mockups),
 * contenido y, opcionalmente, una barra fija abajo con los botones de acción.
 * onClickBack = null → es una pestaña del rol: no hay flecha y la barra inferior la pone el Scaffold.
 */
@Composable
fun UpScreen(
    titulo: String,
    onClickBack: (() -> Unit)? = null,
    subtitulo: String? = null,
    acciones: @Composable RowScope.() -> Unit = {},
    cabecera: (@Composable ColumnScope.() -> Unit)? = null,
    barraInferior: (@Composable RowScope.() -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit
) {
    val esPestana = onClickBack == null

    // Fuera de las pestañas, el contenido sube con el teclado para que los botones de abajo
    // de los formularios no queden tapados.
    Column(
        Modifier.fillMaxSize().background(Bg)
            // En horizontal la barra del sistema queda a un lado: el contenido no debe pasar por debajo.
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .then(if (esPestana) Modifier else Modifier.imePadding())
    ) {
        Column(
            Modifier.fillMaxWidth().background(Navy).statusBarsPadding()
                .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 52.dp)) {
                if (onClickBack != null) {
                    IconButton(onClick = onClickBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                } else Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f).padding(start = 4.dp)) {
                    Text(titulo, color = Color.White, style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (subtitulo != null) Text(subtitulo, color = NavyText, style = MaterialTheme.typography.bodyMedium)
                }
                acciones()
            }
            if (cabecera != null) {
                Column(Modifier.padding(start = 8.dp, top = 10.dp), content = cabecera)
            }
        }
        Column(Modifier.weight(1f).fillMaxWidth(), content = contenido)
        if (barraInferior != null) {
            HorizontalDivider(color = Line)
            Row(
                Modifier.fillMaxWidth().background(Color.White)
                    .then(if (esPestana) Modifier else Modifier.navigationBarsPadding())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = barraInferior
            )
        } else if (!esPestana) {
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

/**
 * Pantalla para cuando el elemento pedido no existe (por ejemplo, se eliminó).
 * Así nunca queda una pantalla en blanco sin forma de volver.
 */
@Composable
fun NoEncontrado(titulo: String, onClickBack: (() -> Unit)?) {
    UpScreen(titulo, onClickBack) {
        EstadoVacio(Icons.Outlined.SearchOff, "No encontramos esta información. Puede que se haya eliminado.")
    }
}

/** Contenido con scroll vertical y separación estándar. */
@Composable
fun ColumnScope.ContenidoScroll(
    espacio: Dp = 14.dp,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(espacio),
        content = contenido
    )
}

/** Fila de cifras grandes en la cabecera azul (como "Resumen de hoy"). */
@Composable
fun ResumenCabecera(vararg cifras: Pair<String, String>) {
    Row(Modifier.fillMaxWidth().padding(end = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        cifras.forEach { (numero, etiqueta) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(numero, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text(etiqueta, color = NavyText, fontSize = 13.sp)
            }
        }
    }
}
