package me.valeriaacosta.uyparceshopper.ui.screens.admin

import me.valeriaacosta.uyparceshopper.data.*
import me.valeriaacosta.uyparceshopper.data.models.*

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.valeriaacosta.uyparceshopper.data.DatosEjemplo
import me.valeriaacosta.uyparceshopper.data.models.Moneda
import me.valeriaacosta.uyparceshopper.data.models.formatoDinero
import me.valeriaacosta.uyparceshopper.navigation.Routes
import me.valeriaacosta.uyparceshopper.ui.components.ContenidoScroll
import me.valeriaacosta.uyparceshopper.ui.components.FilaMenu
import me.valeriaacosta.uyparceshopper.ui.components.UpScreen
import me.valeriaacosta.uyparceshopper.ui.theme.UyParceTheme

// ─── AjustesScreen ───────────────────────────────────────────────────────────

/** Pantalla 46. Menú de ajustes del negocio. */
@Composable
fun AjustesScreen(
    onClickBack: () -> Unit,
    onClickOpcion: (ruta: String) -> Unit
) {
    val a = DatosEjemplo.ajustes
    UpScreen("Ajustes", onClickBack) {
        ContenidoScroll(10.dp) {
            FilaMenu(Icons.Outlined.CurrencyExchange, "Precios detal",
                "Tasa ${formatoDinero(a.tasaDolar, Moneda.COP)} · margen ${porcentaje(a.margenDetal)}") { onClickOpcion(Routes.AJ_PRECIOS) }
            FilaMenu(Icons.Outlined.Percent, "Comisiones",
                "Avanzado ${porcentaje(a.comisionAvanzado)} · Parcero ${porcentaje(a.comisionParcero)}") { onClickOpcion(Routes.AJ_COMISIONES) }
            FilaMenu(Icons.Outlined.FlightTakeoff, "Envío internacional", "Tarifas por libra y seguro") { onClickOpcion(Routes.AJ_ENVIO) }
            FilaMenu(Icons.Outlined.Map, "Zonas de envío nacional", "${DatosEjemplo.zonas.size} zonas") { onClickOpcion(Routes.AJ_ZONAS) }
            FilaMenu(Icons.Outlined.AccountBalance, "Medios de pago", "${DatosEjemplo.mediosPago.size} cuentas") { onClickOpcion(Routes.AJ_MEDIOS) }
            FilaMenu(Icons.Outlined.Scale, "Categorías y pesos", "${DatosEjemplo.categorias.size} categorías") { onClickOpcion(Routes.AJ_CATEGORIAS) }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AjustesScreenPreview() {
    UyParceTheme { AjustesScreen(onClickBack = {}, onClickOpcion = {}) }
}
