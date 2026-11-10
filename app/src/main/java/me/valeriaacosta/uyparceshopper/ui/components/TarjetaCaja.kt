package me.valeriaacosta.uyparceshopper.ui.components

import me.valeriaacosta.uyparceshopper.data.*

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.valeriaacosta.uyparceshopper.data.models.Caja
import me.valeriaacosta.uyparceshopper.data.models.EstadoCaja
import me.valeriaacosta.uyparceshopper.data.models.TipoCaja
import me.valeriaacosta.uyparceshopper.ui.theme.Blue
import me.valeriaacosta.uyparceshopper.ui.theme.Mono

// ─── TarjetaCaja ─────────────────────────────────────────────────────────────

/** Tarjeta de una caja de envío. La usan el operador (Cajas) y el supervisor (Recepción). */
@Composable
fun TarjetaCaja(c: Caja, alPulsar: () -> Unit) {
    Tarjeta(alPulsar = alPulsar) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(c.codigo, fontFamily = Mono, color = Blue, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
            Chip(c.estado.etiqueta, if (c.estado == EstadoCaja.ABIERTA) TonoChip.AMARILLO else TonoChip.AZUL)
        }
        Text(
            if (c.tipo == TipoCaja.MAYORISTA) "Mayorista · ${c.cliente?.nombre ?: "sin cliente"}" else "Detal · ${cantidad(c.pedidos.size, "pedido")}",
            fontWeight = FontWeight.SemiBold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(cantidad(c.prendas, "prenda"), TonoChip.GRIS)
            c.pesoLb?.let { Chip("$it lb · ${c.tipoEnvio?.etiqueta}", TonoChip.GRIS) }
            c.fechaEnvio?.let { Chip("Enviada ${fecha(it)}", TonoChip.GRIS) }
        }
    }
}
