package me.valeriaacosta.uyparceshopper.ui.screens.supervisor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.valeriaacosta.uyparceshopper.data.DatosEjemplo
import me.valeriaacosta.uyparceshopper.data.models.EstadoPedido
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── PorDespacharScreen ──────────────────────────────────────────────────────

/** Pantalla 30. Pedidos completos listos para la transportadora y los ya enviados. */
@Composable
fun PorDespacharScreen(
    onClickBack: (() -> Unit)? = null,
    onClickDespachar: (numero: Int) -> Unit,
    onClickPedido: (numero: Int) -> Unit
) {
    val listos = DatosEjemplo.pedidos.filter { it.estado == EstadoPedido.EN_CUCUTA }
    val enviados = DatosEjemplo.pedidos.filter { it.estado == EstadoPedido.ENVIADO }

    UpScreen("Despachos", onClickBack, cabecera = {
        ResumenCabecera("${listos.size}" to "Por despachar", "${enviados.size}" to "Enviados", "1" to "Entregados hoy")
    }) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { TituloSeccion("Listos para despachar") }
            items(listos, key = { it.numero }) { p ->
                Tarjeta(alPulsar = { onClickDespachar(p.numero) }) {
                    Text("#${p.numero} · ${p.cliente.nombre}", fontWeight = FontWeight.Bold)
                    Text("${p.cliente.ciudad} · ${p.zona.nombre}", color = Muted)
                    Chip(cantidad(p.prendas, "prenda"), TonoChip.GRIS)
                }
            }
            item { TituloSeccion("Enviados") }
            items(enviados, key = { it.numero }) { p ->
                Tarjeta(alPulsar = { onClickPedido(p.numero) }) {
                    Text("#${p.numero} · ${p.cliente.nombre}", fontWeight = FontWeight.Bold)
                    Text("${p.transportadora} · guía ${p.guia}", color = Muted, fontFamily = Mono, fontSize = 14.sp)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PorDespacharScreenPreview() {
    UyParceTheme { PorDespacharScreen(onClickDespachar = {}, onClickPedido = {}) }
}
