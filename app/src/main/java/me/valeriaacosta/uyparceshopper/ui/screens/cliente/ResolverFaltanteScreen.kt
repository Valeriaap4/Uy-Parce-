package me.valeriaacosta.uyparceshopper.ui.screens.cliente

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.mensaje
import me.valeriaacosta.uyparceshopper.data.models.*
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── ResolverFaltanteScreen ──────────────────────────────────────────────────

/**
 * Pantalla 16. El cliente elige qué hacer con el dinero de una prenda que no va a recibir:
 * saldo a favor (se descuenta solo en su próximo pedido) o devolución. Si hay varios faltantes,
 * se resuelven uno por uno. La elección no se puede cambiar.
 */
@Composable
fun ResolverFaltanteScreen(
    numero: Int,
    onClickBack: () -> Unit,
    onResuelto: () -> Unit
) {
    SegunCarga(rememberPedido(numero), "Producto faltante", onClickBack) { p -> ResolverContenido(p, onClickBack, onResuelto) }
}

@Composable
private fun ResolverContenido(pedido: Pedido, onClickBack: () -> Unit, onResuelto: () -> Unit) {
    val estado = LocalEstadoApp.current
    val mensaje = LocalMensaje.current
    val scope = rememberCoroutineScope()
    val pendientes = pedido.faltantesSinResolver
    val faltante = pendientes.firstOrNull()

    if (faltante == null) {
        UpScreen("Producto faltante", onClickBack, subtitulo = "Pedido #${pedido.numero}") {
            ContenidoScroll { Aviso("No tienes faltantes por resolver en este pedido.", TonoChip.VERDE, Icons.Outlined.CheckCircle) }
        }
        return
    }

    var opcion by rememberSaveable(faltante.id) { mutableIntStateOf(-1) }
    var cuenta by rememberSaveable(faltante.id) { mutableStateOf("") }
    var error by remember(faltante.id) { mutableStateOf<String?>(null) }
    var guardando by remember { mutableStateOf(false) }

    UpScreen(
        "Producto faltante", onClickBack,
        subtitulo = "Pedido #${pedido.numero}" + if (pendientes.size > 1) " · faltante 1 de ${pendientes.size}" else "",
        barraInferior = {
            BotonPrincipal("Confirmar", {
                error = when {
                    opcion < 0 -> "Elige una opción"
                    opcion == 1 && cuenta.filter(Char::isDigit).length < 6 -> "Escribe tu número de Nequi o cuenta"
                    else -> null
                }
                if (error == null) {
                    guardando = true
                    val tipo = if (opcion == 0) TipoResolucion.SALDO_A_FAVOR else TipoResolucion.DEVOLUCION
                    scope.launch {
                        estado.resolverFaltante(pedido.numero, faltante.id, tipo, cuenta)
                            .onSuccess {
                                mensaje(if (tipo == TipoResolucion.SALDO_A_FAVOR) "Saldo a favor agregado" else "Solicitud de devolución enviada")
                                if (pendientes.size <= 1) onResuelto()
                            }
                            .onFailure { mensaje(it.mensaje()) }
                        guardando = false
                    }
                }
            }, Modifier.fillMaxWidth(), cargando = guardando)
        }
    ) {
        ContenidoScroll {
            Aviso("${faltante.motivo}: ${faltante.detalle}.", TonoChip.ROJO, Icons.Outlined.ErrorOutline)
            Tarjeta { FilaDinero("Valor a tu favor", faltante.valor, faltante.moneda, total = true) }
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
            if (opcion == 1) CampoTexto(
                "Número de Nequi o cuenta", cuenta, { cuenta = it.filter { c -> c.isLetterOrDigit() || c == ' ' || c == '-' }.take(40) },
                placeholder = "Ej: Nequi 300 000 0000", teclado = KeyboardType.Text
            )
            error?.let { Text(it, color = Red) }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ResolverFaltanteScreenPreview() {
    VistaPrevia { ResolverFaltanteScreen(numero = 1012, onClickBack = {}, onResuelto = {}) }
}
