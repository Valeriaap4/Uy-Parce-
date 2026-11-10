package me.valeriaacosta.uyparceshopper.ui.screens.cliente

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.mensaje
import me.valeriaacosta.uyparceshopper.data.models.*
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── ConfirmarPedidoScreen ───────────────────────────────────────────────────

/**
 * Pantalla 10. Resumen del pedido, dirección de entrega y desglose del total. Al confirmar, el
 * carrito se convierte en un pedido con número propio, precios congelados y 24 h para pagar.
 */
@Composable
fun ConfirmarPedidoScreen(
    onClickBack: () -> Unit,
    onClickCambiarDireccion: () -> Unit,
    onPedidoConfirmado: (numero: Int) -> Unit
) {
    val estado = LocalEstadoApp.current
    val u = estado.usuario
    if (u == null) {
        UpScreen("Confirmar pedido", onClickBack) { Cargando() }
        return
    }
    val mensaje = LocalMensaje.current
    val scope = rememberCoroutineScope()
    val tipo = u.tipoCliente ?: TipoCliente.DETAL
    val carrito = estado.carrito
    val zona = estado.ajustes.zonaDe(u.ciudad)
    val d = Precios.desgloseCarrito(carrito, tipo, zona, estado.ajustes)
    val saldo = Precios.redondear(minOf(estado.miSaldo, d.total), d.moneda)
    val hayAgotados = carrito.any { it.producto.agotado }
    var confirmando by remember { mutableStateOf(false) }

    UpScreen("Confirmar pedido", onClickBack, barraInferior = {
        BotonPrincipal("Confirmar y pagar", {
            confirmando = true
            scope.launch {
                estado.confirmarPedido()
                    .onSuccess { onPedidoConfirmado(it.numero) }
                    .onFailure { mensaje(it.mensaje()); confirmando = false }
            }
        }, Modifier.fillMaxWidth(), Icons.Outlined.Lock, habilitado = carrito.isNotEmpty() && !hayAgotados, cargando = confirmando)
    }) {
        ContenidoScroll {
            if (hayAgotados) Aviso("Un producto de tu carrito se agotó. Vuelve al carrito y quítalo.", TonoChip.ROJO, Icons.Outlined.Warning)
            Tarjeta {
                Text("Prendas", style = MaterialTheme.typography.titleMedium)
                carrito.forEach { l ->
                    Row {
                        Text("${l.cantidad} × ${l.producto.nombre} · ${l.talla}", modifier = Modifier.weight(1f), color = Muted)
                        Text(formatoDinero(Precios.precioUnitario(l.producto, tipo, estado.ajustes) * l.cantidad, tipo.moneda), fontFamily = Mono)
                    }
                }
            }
            Tarjeta {
                Text("Entrega", style = MaterialTheme.typography.titleMedium)
                FilaDato("Dirección", u.direccion)
                FilaDato("Ciudad", u.ciudad)
                FilaDato("Zona", zona.nombre)
                TextButton(onClick = onClickCambiarDireccion, contentPadding = PaddingValues(0.dp)) {
                    Text("Cambiar dirección", color = Blue)
                }
            }
            TarjetaDesglose(d, saldoAplicado = saldo)
            if (saldo > 0) Aviso("Se descuenta tu saldo a favor de ${formatoDinero(saldo, d.moneda)} ${d.moneda}.", TonoChip.VERDE, Icons.Outlined.Savings)
            Aviso(
                "Después de confirmar tienes ${estado.ajustes.horasParaPagar} horas para pagar y reportar el pago. El precio queda congelado.",
                TonoChip.AMARILLO, Icons.Outlined.Schedule
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ConfirmarPedidoScreenPreview() {
    VistaPrevia { ConfirmarPedidoScreen(onClickBack = {}, onClickCambiarDireccion = {}, onPedidoConfirmado = {}) }
}
