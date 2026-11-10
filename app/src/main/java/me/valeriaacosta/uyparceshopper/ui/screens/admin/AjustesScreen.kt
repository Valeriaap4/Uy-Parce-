package me.valeriaacosta.uyparceshopper.ui.screens.admin

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.valeriaacosta.uyparceshopper.data.Listas
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.mensaje
import me.valeriaacosta.uyparceshopper.data.models.*
import me.valeriaacosta.uyparceshopper.navigation.Routes
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── AjustesScreen ───────────────────────────────────────────────────────────

/**
 * Pantalla 46. Menú de ajustes del negocio. Si la base de datos todavía no está preparada, muestra
 * primero el botón para crear los ajustes y los contadores (y los productos de ejemplo).
 */
@Composable
fun AjustesScreen(
    onClickBack: () -> Unit,
    onClickOpcion: (ruta: String) -> Unit
) {
    val estado = LocalEstadoApp.current
    val mensaje = LocalMensaje.current
    val scope = rememberCoroutineScope()
    val a = estado.ajustes
    var preparar by remember { mutableStateOf(false) }
    var preparando by remember { mutableStateOf(false) }

    UpScreen("Ajustes", onClickBack) {
        ContenidoScroll(10.dp) {
            if (estado.cargado(Listas.AJUSTES) && !estado.hayAjustes) {
                Tarjeta {
                    Text("La base de datos está vacía", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Antes de recibir pedidos hay que crear los ajustes del negocio y los contadores " +
                            "(el primer pedido será el #1001, la primera factura FAC-0001 y la primera caja CJ-0001).",
                        color = Muted
                    )
                    BotonPrincipal("Preparar base de datos", { preparar = true }, Modifier.fillMaxWidth(), cargando = preparando)
                }
            }
            FilaMenu(Icons.Outlined.CurrencyExchange, "Precios detal",
                "Tasa ${formatoDinero(a.tasaDolar, Moneda.COP)} · margen ${porcentaje(a.margenDetal)}") { onClickOpcion(Routes.AJ_PRECIOS) }
            FilaMenu(Icons.Outlined.Percent, "Comisiones",
                "Avanzado ${porcentaje(a.comisionAvanzado)} · Parcero ${porcentaje(a.comisionParcero)}") { onClickOpcion(Routes.AJ_COMISIONES) }
            FilaMenu(Icons.Outlined.FlightTakeoff, "Envío internacional", "Tarifas por libra y seguro") { onClickOpcion(Routes.AJ_ENVIO) }
            FilaMenu(Icons.Outlined.Map, "Zonas de envío nacional", "${a.zonas.size} zonas") { onClickOpcion(Routes.AJ_ZONAS) }
            FilaMenu(Icons.Outlined.AccountBalance, "Medios de pago", cantidad(a.mediosPago.size, "cuenta")) { onClickOpcion(Routes.AJ_MEDIOS) }
            FilaMenu(Icons.Outlined.Scale, "Categorías y pesos", cantidad(a.categorias.size, "categoría")) { onClickOpcion(Routes.AJ_CATEGORIAS) }
        }
    }

    fun prepararBase(conProductos: Boolean) {
        preparar = false
        preparando = true
        scope.launch {
            estado.prepararBaseDeDatos(conProductos)
                .onSuccess { mensaje(if (conProductos) "Listo: ajustes, contadores y 8 productos de ejemplo" else "Listo: ajustes y contadores") }
                .onFailure { mensaje(it.mensaje()) }
            preparando = false
        }
    }

    if (preparar) AlertDialog(
        onDismissRequest = { preparar = false },
        containerColor = Color.White,
        title = { Text("Preparar base de datos", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "Se crean los ajustes con valores iniciales (los puedes cambiar después) y los contadores. " +
                    "¿Quieres cargar también 8 productos de ejemplo (ficticios) para la demostración?"
            )
        },
        confirmButton = { TextButton(onClick = { prepararBase(true) }) { Text("Con productos de ejemplo", fontWeight = FontWeight.Bold, color = Blue) } },
        dismissButton = { TextButton(onClick = { prepararBase(false) }) { Text("Solo ajustes", color = Navy) } }
    )
}

@Preview(showBackground = true)
@Composable
fun AjustesScreenPreview() {
    VistaPrevia(Rol.ADMIN) { AjustesScreen(onClickBack = {}, onClickOpcion = {}) }
}
