package me.valeriaacosta.uyparceshopper.ui.screens.supervisor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.valeriaacosta.uyparceshopper.ui.components.ContenidoScroll
import me.valeriaacosta.uyparceshopper.ui.components.FilaMenu
import me.valeriaacosta.uyparceshopper.ui.components.UpScreen
import me.valeriaacosta.uyparceshopper.ui.theme.UyParceTheme

// ─── SupMasScreen ────────────────────────────────────────────────────────────

/** Pantalla 32. Menú "Más" del supervisor. */
@Composable
fun SupMasScreen(
    onClickClientes: () -> Unit,
    onClickFacturas: () -> Unit,
    onClickCartera: () -> Unit,
    onClickGastos: () -> Unit,
    onClickReportes: () -> Unit,
    onClickPerfil: () -> Unit
) {
    UpScreen("Más") {
        ContenidoScroll(10.dp) {
            FilaMenu(Icons.Outlined.Groups, "Clientes", "Datos, pedidos y saldos", onClickClientes)
            FilaMenu(Icons.Outlined.Description, "Facturas", "Facturas generadas", onClickFacturas)
            FilaMenu(Icons.Outlined.AccountBalanceWallet, "Cartera", "Saldos a favor y devoluciones", onClickCartera)
            FilaMenu(Icons.Outlined.RequestQuote, "Gastos", "Flete, empaque, aranceles…", onClickGastos)
            FilaMenu(Icons.Outlined.Insights, "Reportes", "Ventas, gastos y ganancia", onClickReportes)
            FilaMenu(Icons.Outlined.Person, "Perfil", null, onClickPerfil)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SupMasScreenPreview() {
    UyParceTheme { SupMasScreen({}, {}, {}, {}, {}, {}) }
}
