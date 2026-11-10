package me.valeriaacosta.uyparceshopper.ui.screens.supervisor

import me.valeriaacosta.uyparceshopper.data.*

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.valeriaacosta.uyparceshopper.data.DatosEjemplo
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.models.EstadoPago
import me.valeriaacosta.uyparceshopper.data.models.Precios
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── PagosScreen ─────────────────────────────────────────────────────────────

/** Pantalla 25. Comprobantes de pago por revisar y ya revisados. */
@Composable
fun PagosScreen(
    onClickBack: (() -> Unit)? = null,
    onClickPago: (id: String) -> Unit
) {
    val ajustes = LocalEstadoApp.current.ajustes
    var filtro by remember { mutableIntStateOf(0) }
    val lista = DatosEjemplo.pagos.filter {
        if (filtro == 0) it.estado == EstadoPago.EN_REVISION else it.estado != EstadoPago.EN_REVISION
    }

    UpScreen("Pagos", onClickBack, cabecera = {
        ResumenCabecera(
            "${DatosEjemplo.pagos.count { it.estado == EstadoPago.EN_REVISION }}" to "Por revisar",
            "${DatosEjemplo.pagos.count { it.estado == EstadoPago.APROBADO }}" to "Aprobados hoy",
            "0" to "Rechazados"
        )
    }) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { PestanasPildora(listOf("Por revisar", "Revisados"), filtro, { filtro = it }) }
            items(lista, key = { it.id }) { p ->
                val d = Precios.desglose(p.pedido, ajustes)
                Tarjeta(alPulsar = { onClickPago(p.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("#${p.pedido.numero}", fontFamily = Mono, color = Blue, fontWeight = FontWeight.Bold,
                            fontSize = 17.sp, modifier = Modifier.weight(1f))
                        Dinero(d.total, d.moneda, 16.sp)
                    }
                    Text(p.pedido.cliente.nombre, fontWeight = FontWeight.SemiBold)
                    Text("${p.medio} · ${p.referencia} · ${fechaHoraCorta(p.fecha)}", color = Muted, fontSize = 14.sp)
                    Chip(p.estado.etiqueta, if (p.estado == EstadoPago.EN_REVISION) TonoChip.MORADO else TonoChip.VERDE)
                }
            }
            if (lista.isEmpty()) item { EstadoVacio(Icons.Outlined.Payments, "No hay pagos aquí") }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PagosScreenPreview() {
    UyParceTheme { PagosScreen(onClickPago = {}) }
}
