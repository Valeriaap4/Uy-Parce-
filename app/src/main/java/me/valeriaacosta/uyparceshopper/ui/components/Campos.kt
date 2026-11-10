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

// ─── Formularios ───

@Composable
fun EtiquetaCampo(texto: String) {
    Text(texto.uppercase(), style = MaterialTheme.typography.labelMedium, color = Muted)
}

@Composable
fun CampoTexto(
    etiqueta: String,
    valor: String,
    alCambiar: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    error: String? = null,
    teclado: KeyboardType = KeyboardType.Text,
    iconoInicio: ImageVector? = null,
    iconoFin: (@Composable () -> Unit)? = null,
    contrasena: Boolean = false,
    lineas: Int = 1,
    soloLectura: Boolean = false
) {
    var visible by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (etiqueta.isNotEmpty()) EtiquetaCampo(etiqueta)
        TextField(
            value = valor,
            onValueChange = alCambiar,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = Muted.copy(alpha = 0.7f)) },
            leadingIcon = iconoInicio?.let { { Icon(it, null, tint = Muted) } },
            trailingIcon = when {
                contrasena -> {
                    {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (visible) "Ocultar" else "Mostrar", tint = Blue)
                        }
                    }
                }
                else -> iconoFin
            },
            visualTransformation = if (contrasena && !visible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (contrasena) KeyboardType.Password else teclado),
            singleLine = lineas == 1,
            minLines = lineas,
            readOnly = soloLectura,
            isError = error != null,
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Field,
                unfocusedContainerColor = Field,
                errorContainerColor = RedSoft,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            )
        )
        if (error != null) Text(error, color = Red, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Lista desplegable con el mismo estilo de los campos. */
@Composable
fun Selector(
    etiqueta: String,
    opciones: List<String>,
    seleccion: String?,
    alElegir: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Elige una opción",
    error: String? = null
) {
    var abierto by remember { mutableStateOf(false) }
    Box(modifier) {
        CampoTexto(
            etiqueta = etiqueta, valor = seleccion ?: "", alCambiar = {}, placeholder = placeholder,
            error = error, soloLectura = true,
            iconoFin = { Icon(Icons.Outlined.ExpandMore, null, tint = Muted) }
        )
        Box(Modifier.matchParentSize().clickable { abierto = true })
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            opciones.forEach { op ->
                DropdownMenuItem(text = { Text(op) }, onClick = { alElegir(op); abierto = false })
            }
        }
    }
}

@Composable
fun BarraBuscar(valor: String, alCambiar: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    CampoTexto("", valor, alCambiar, modifier, placeholder = placeholder, iconoInicio = Icons.Outlined.Search)
}

/** Control de cantidad − n + (entre mínimo y máximo). */
@Composable
fun SelectorCantidad(cantidad: Int, alCambiar: (Int) -> Unit, minimo: Int = 1, maximo: Int = 99) {
    Row(
        Modifier.clip(RoundedCornerShape(14.dp)).background(Field),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { if (cantidad > minimo) alCambiar(cantidad - 1) }, enabled = cantidad > minimo) { Icon(Icons.Outlined.Remove, "Menos") }
        Text("$cantidad", fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.widthIn(min = 24.dp))
        IconButton(onClick = { if (cantidad < maximo) alCambiar(cantidad + 1) }, enabled = cantidad < maximo) { Icon(Icons.Outlined.Add, "Más") }
    }
}
