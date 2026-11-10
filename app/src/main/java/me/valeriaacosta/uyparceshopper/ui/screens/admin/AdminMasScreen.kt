package me.valeriaacosta.uyparceshopper.ui.screens.admin

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.valeriaacosta.uyparceshopper.navigation.Routes
import me.valeriaacosta.uyparceshopper.ui.components.ContenidoScroll
import me.valeriaacosta.uyparceshopper.ui.components.FilaMenu
import me.valeriaacosta.uyparceshopper.ui.components.TituloSeccion
import me.valeriaacosta.uyparceshopper.ui.components.UpScreen
import me.valeriaacosta.uyparceshopper.ui.theme.UyParceTheme

// ─── AdminMasScreen ──────────────────────────────────────────────────────────

/**
 * Pantalla 45. Menú "Más" del admin: sus herramientas y acceso a todas las pantallas
 * del operador y del supervisor. Como son muchas opciones, recibe la ruta a abrir.
 */
@Composable
fun AdminMasScreen(
    onClickOpcion: (ruta: String) -> Unit
) {
    UpScreen("Más") {
        ContenidoScroll(10.dp) {
            TituloSeccion("Administración")
            FilaMenu(Icons.Outlined.Settings, "Ajustes", "Tasa, márgenes, envíos y pagos") { onClickOpcion(Routes.AJUSTES) }
            FilaMenu(Icons.Outlined.DeleteOutline, "Papelera", "Restaurar lo eliminado") { onClickOpcion(Routes.PAPELERA) }
            FilaMenu(Icons.Outlined.History, "Auditoría", "Quién hizo qué y cuándo") { onClickOpcion(Routes.AUDITORIA) }

            TituloSeccion("Operación (operador)")
            FilaMenu(Icons.Outlined.Checkroom, "Catálogo") { onClickOpcion(Routes.OP_CATALOGO) }
            FilaMenu(Icons.Outlined.ShoppingCart, "Por comprar") { onClickOpcion(Routes.POR_COMPRAR) }
            FilaMenu(Icons.Outlined.Inventory2, "Cajas") { onClickOpcion(Routes.CAJAS) }

            TituloSeccion("Cúcuta (supervisor)")
            FilaMenu(Icons.Outlined.Payments, "Pagos") { onClickOpcion(Routes.PAGOS) }
            FilaMenu(Icons.Outlined.MoveToInbox, "Recepción") { onClickOpcion(Routes.CAJAS_EN_CAMINO) }
            FilaMenu(Icons.Outlined.LocalShipping, "Despachos") { onClickOpcion(Routes.POR_DESPACHAR) }
            FilaMenu(Icons.Outlined.Groups, "Clientes") { onClickOpcion(Routes.CLIENTES) }
            FilaMenu(Icons.Outlined.Description, "Facturas") { onClickOpcion(Routes.FACTURAS) }
            FilaMenu(Icons.Outlined.AccountBalanceWallet, "Cartera") { onClickOpcion(Routes.CARTERA) }
            FilaMenu(Icons.Outlined.RequestQuote, "Gastos") { onClickOpcion(Routes.GASTOS) }
            FilaMenu(Icons.Outlined.Insights, "Reportes") { onClickOpcion(Routes.REPORTES) }

            TituloSeccion("Cuenta")
            FilaMenu(Icons.Outlined.Person, "Perfil") { onClickOpcion(Routes.PERFIL) }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AdminMasScreenPreview() {
    UyParceTheme { AdminMasScreen(onClickOpcion = {}) }
}
