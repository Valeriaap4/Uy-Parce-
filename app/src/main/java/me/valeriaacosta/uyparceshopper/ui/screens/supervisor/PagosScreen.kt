package me.valeriaacosta.uyparceshopper.ui.screens.supervisor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.valeriaacosta.uyparceshopper.data.Listas
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.esHoy
import me.valeriaacosta.uyparceshopper.data.fechaHoraCorta
import me.valeriaacosta.uyparceshopper.data.models.EstadoPago
import me.valeriaacosta.uyparceshopper.data.models.Pago
import me.valeriaacosta.uyparceshopper.data.models.Rol
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── PagosScreen ─────────────────────────────────────────────────────────────

/**
 * Pantalla 25. Comprobantes por revisar (en vivo, del más antiguo al más nuevo) y los ya revisados
 * (se consultan al abrir la pestaña).
 */
@Composable
fun PagosScreen(
    onClickBack: (() -> Unit)? = null,
    onClickPago: (id: String) -> Unit
) {
    val estado = LocalEstadoApp.current
    var filtro by rememberSaveable { mutableIntStateOf(0) }
    val porRevisar = estado.pagos.filter { it.estado == EstadoPago.EN_REVISION }
    // Los revisados se vuelven a consultar cuando cambia la lista por revisar (por ejemplo, al aprobar uno).
    val revisados by produceState<List<Pago>?>(null, porRevisar.size) {
        value = estado.pagosRecientes().getOrDefault(emptyList()).filter { it.estado != EstadoPago.EN_REVISION }
    }
    val lista = if (filtro == 0) porRevisar else revisados.orEmpty()
    val cargando = if (filtro == 0) !estado.cargado(Listas.PAGOS) else revisados == null

    UpScreen("Pagos", onClickBack, cabecera = {
        ResumenCabecera(
            "${porRevisar.size}" to "Por revisar",
            "${revisados.orEmpty().count { it.estado == EstadoPago.APROBADO && esHoy(it.revisadoEn) }}" to "Aprobados hoy",
            "${revisados.orEmpty().count { it.estado == EstadoPago.RECHAZADO && esHoy(it.revisadoEn) }}" to "Rechazados hoy"
        )
    }) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { PestanasPildora(listOf("Por revisar", "Revisados"), filtro, { filtro = it }) }
            items(lista, key = { it.id }) { p ->
                Tarjeta(alPulsar = { onClickPago(p.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("#${p.numeroPedido}", fontFamily = Mono, color = Blue, fontWeight = FontWeight.Bold,
                            fontSize = 17.sp, modifier = Modifier.weight(1f))
                        Dinero(p.monto, p.moneda, 16.sp)
                    }
                    Text(p.clienteNombre, fontWeight = FontWeight.SemiBold)
                    Text("${p.medio} · ${p.referencia} · ${fechaHoraCorta(p.fecha)}", color = Muted, fontSize = 14.sp)
                    Chip(
                        p.estado.etiqueta, when (p.estado) {
                            EstadoPago.EN_REVISION -> TonoChip.MORADO
                            EstadoPago.APROBADO -> TonoChip.VERDE
                            EstadoPago.RECHAZADO -> TonoChip.ROJO
                        }
                    )
                }
            }
            if (lista.isEmpty()) item {
                if (cargando) Cargando() else EstadoVacio(Icons.Outlined.Payments, if (filtro == 0) "No hay pagos por revisar" else "No hay pagos revisados")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PagosScreenPreview() {
    VistaPrevia(Rol.SUPERVISOR) { PagosScreen(onClickPago = {}) }
}
