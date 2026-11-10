package me.valeriaacosta.uyparceshopper.ui.screens.supervisor

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import me.valeriaacosta.uyparceshopper.data.models.Moneda
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── CarteraScreen ───────────────────────────────────────────────────────────

/** Pantalla 36. Saldos a favor y devoluciones pendientes, separados en USD y COP. */
@Composable
fun CarteraScreen(
    onClickBack: () -> Unit,
    onClickCliente: (id: String) -> Unit
) {
    var moneda by remember { mutableIntStateOf(1) }
    val mensaje = LocalMensaje.current

    UpScreen("Cartera", onClickBack, cabecera = {
        ResumenCabecera("$87.000" to "Saldos a favor", "$240.000" to "Devoluciones", "2" to "Clientes")
    }) {
        ContenidoScroll {
            PestanasPildora(listOf("USD · Mayoristas", "COP · Detal"), moneda, { moneda = it })
            if (moneda == 0) {
                EstadoVacio(Icons.Outlined.AccountBalanceWallet, "Sin saldos ni devoluciones en USD")
            } else {
                TituloSeccion("Saldos a favor")
                Tarjeta(alPulsar = { onClickCliente("c2") }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Paola Lizarazo", fontWeight = FontWeight.Bold)
                            Text("Faltante pedido #1012 · hace 2 días", color = Muted, fontSize = 14.sp)
                        }
                        Dinero(87000.0, Moneda.COP, 15.sp)
                    }
                }
                TituloSeccion("Devoluciones pendientes")
                Tarjeta {
                    Text("Laura Méndez · Nequi 300 555 0104", fontWeight = FontWeight.Bold)
                    Text("Jean Levi's 511 · pedido #1012", color = Muted, fontSize = 14.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Dinero(240000.0, Moneda.COP, 15.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = { mensaje("Devolución marcada como hecha") }) {
                            Text("Marcar hecha", color = Blue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CarteraScreenPreview() {
    UyParceTheme { CarteraScreen(onClickBack = {}, onClickCliente = {}) }
}
