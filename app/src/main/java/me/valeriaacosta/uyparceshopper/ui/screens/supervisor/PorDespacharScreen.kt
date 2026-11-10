package me.valeriaacosta.uyparceshopper.ui.screens.supervisor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.valeriaacosta.uyparceshopper.data.Listas
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.models.EstadoPedido
import me.valeriaacosta.uyparceshopper.data.models.Rol
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── PorDespacharScreen ──────────────────────────────────────────────────────

/** Pantalla 30. Pedidos en Cúcuta listos para la transportadora y los ya enviados (en vivo). */
@Composable
fun PorDespacharScreen(
    onClickBack: (() -> Unit)? = null,
    onClickDespachar: (numero: Int) -> Unit,
    onClickPedido: (numero: Int) -> Unit
) {
    val estado = LocalEstadoApp.current
    val listos = estado.pedidos.filter { it.estado == EstadoPedido.EN_CUCUTA }.sortedBy { it.numero }
    val enviados = estado.pedidos.filter { it.estado == EstadoPedido.ENVIADO }.sortedByDescending { it.despachadoEn }

    UpScreen("Despachos", onClickBack, cabecera = {
        ResumenCabecera(
            "${listos.size}" to "Por despachar",
            "${enviados.size}" to "Enviados",
            "${listos.count { it.faltantesSinResolver.isNotEmpty() }}" to "Con faltante"
        )
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
                    androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip(cantidad(p.recibidas, "prenda"), TonoChip.GRIS)
                        if (p.faltantesSinResolver.isNotEmpty()) Chip("Faltante sin resolver", TonoChip.ROJO)
                    }
                }
            }
            if (listos.isEmpty()) item {
                if (estado.cargado(Listas.PEDIDOS)) Text("No hay pedidos esperando despacho.", color = Muted) else Cargando()
            }
            item { TituloSeccion("Enviados") }
            items(enviados, key = { it.numero }) { p ->
                Tarjeta(alPulsar = { onClickPedido(p.numero) }) {
                    Text("#${p.numero} · ${p.cliente.nombre}", fontWeight = FontWeight.Bold)
                    Text("${p.transportadora ?: ""} · guía ${p.guia ?: ""}", color = Muted, fontFamily = Mono, fontSize = 14.sp)
                }
            }
            if (enviados.isEmpty() && estado.cargado(Listas.PEDIDOS)) item {
                EstadoVacio(Icons.Outlined.LocalShipping, "No hay pedidos en manos de la transportadora")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PorDespacharScreenPreview() {
    VistaPrevia(Rol.SUPERVISOR) { PorDespacharScreen(onClickDespachar = {}, onClickPedido = {}) }
}
