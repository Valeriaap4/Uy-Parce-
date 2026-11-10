package me.valeriaacosta.uyparceshopper.ui.screens.cliente

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.valeriaacosta.uyparceshopper.data.DatosEjemplo
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.models.Precios
import me.valeriaacosta.uyparceshopper.data.models.TipoCliente
import me.valeriaacosta.uyparceshopper.data.models.formatoDinero
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── ConfirmarPedidoScreen ───────────────────────────────────────────────────

/** Pantalla 10. Resumen del pedido, dirección de entrega y desglose del total. */
@Composable
fun ConfirmarPedidoScreen(
    onClickBack: () -> Unit,
    onClickCambiarDireccion: () -> Unit,
    onPedidoConfirmado: (numero: Int) -> Unit
) {
    val estado = LocalEstadoApp.current
    val u = estado.usuario ?: DatosEjemplo.laura
    val tipo = u.tipoCliente ?: TipoCliente.DETAL
    val zona = DatosEjemplo.zonaDe(u.ciudad)
    val d = Precios.desglose(estado.carrito.toList(), tipo, zona, estado.ajustes)

    UpScreen("Confirmar pedido", onClickBack, barraInferior = {
        BotonPrincipal("Confirmar y pagar", {
            estado.carrito.clear()
            onPedidoConfirmado(1016) // número de ejemplo; Firebase asignará el real
        }, Modifier.fillMaxWidth(), Icons.Outlined.Lock)
    }) {
        ContenidoScroll {
            Tarjeta {
                Text("Prendas", style = MaterialTheme.typography.titleMedium)
                estado.carrito.forEach { l ->
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
            TarjetaDesglose(d)
            Aviso("Después de confirmar tienes 24 horas para pagar y reportar el pago. El precio queda congelado.",
                TonoChip.AMARILLO, Icons.Outlined.Schedule)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ConfirmarPedidoScreenPreview() {
    UyParceTheme { ConfirmarPedidoScreen(onClickBack = {}, onClickCambiarDireccion = {}, onPedidoConfirmado = {}) }
}
