package me.valeriaacosta.uyparceshopper.ui.screens.cliente

import me.valeriaacosta.uyparceshopper.data.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.valeriaacosta.uyparceshopper.data.DatosEjemplo
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.models.Precios
import me.valeriaacosta.uyparceshopper.data.models.TipoCliente
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── ResolverFaltanteScreen ──────────────────────────────────────────────────

/** Pantalla 16. El cliente elige saldo a favor o devolución por una prenda que no llegó. */
@Composable
fun ResolverFaltanteScreen(
    numero: Int,
    onClickBack: () -> Unit,
    onResuelto: () -> Unit
) {
    val estado = LocalEstadoApp.current
    val pedido = DatosEjemplo.buscarPedido(numero) ?: return NoEncontrado("Pedido #$numero", onClickBack)
    val linea = pedido.lineas.firstOrNull { it.recibidas < it.cantidad } ?: pedido.lineas.first()
    val faltan = (linea.cantidad - linea.recibidas).coerceAtLeast(1)
    val tipo = pedido.cliente.tipoCliente ?: TipoCliente.DETAL
    val valor = Precios.precioUnitario(linea.producto, tipo, estado.ajustes) * faltan
    val mensaje = LocalMensaje.current
    var opcion by remember { mutableIntStateOf(-1) }
    var cuenta by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    UpScreen("Producto faltante", onClickBack, subtitulo = "Pedido #$numero", barraInferior = {
        BotonPrincipal("Confirmar", {
            error = when {
                opcion < 0 -> "Elige una opción"
                opcion == 1 && cuenta.length < 6 -> "Escribe tu número de Nequi o cuenta"
                else -> null
            }
            if (error == null) {
                mensaje(if (opcion == 0) "Saldo a favor agregado" else "Solicitud de devolución enviada")
                onResuelto()
            }
        }, Modifier.fillMaxWidth())
    }) {
        ContenidoScroll {
            Aviso("No pudimos conseguir $faltan × ${linea.producto.nombre} (talla ${linea.talla}).", TonoChip.ROJO, Icons.Outlined.ErrorOutline)
            Tarjeta { FilaDinero("Valor a tu favor", valor, tipo.moneda, total = true) }
            Text("¿Qué quieres hacer con ese dinero?", style = MaterialTheme.typography.titleMedium)
            listOf(
                "Saldo a favor" to "Se descuenta automáticamente en tu próximo pedido.",
                "Devolución" to "Te devolvemos el dinero por Nequi o transferencia."
            ).forEachIndexed { i, (titulo, detalle) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(if (opcion == i) 2.dp else 1.dp, if (opcion == i) Navy else Line, RoundedCornerShape(16.dp))
                        .clickable { opcion = i; error = null }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = opcion == i, onClick = { opcion = i; error = null })
                    Column {
                        Text(titulo, fontWeight = FontWeight.Bold)
                        Text(detalle, color = Muted, fontSize = 14.sp)
                    }
                }
            }
            if (opcion == 1) CampoTexto("Número de Nequi o cuenta", cuenta, { cuenta = it.filter(Char::isDigit) })
            error?.let { Text(it, color = Red) }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ResolverFaltanteScreenPreview() {
    UyParceTheme { ResolverFaltanteScreen(numero = 1012, onClickBack = {}, onResuelto = {}) }
}
