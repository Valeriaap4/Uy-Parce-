package me.valeriaacosta.uyparceshopper.ui.components

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import me.valeriaacosta.uyparceshopper.data.models.Desglose
import me.valeriaacosta.uyparceshopper.ui.theme.Line

// ─── TarjetaDesglose ─────────────────────────────────────────────────────────

/** Desglose del total según el tipo de cliente (productos, comisión, envíos). */
@Composable
fun TarjetaDesglose(d: Desglose, titulo: String = "Resumen") {
    Tarjeta {
        Text(titulo, style = MaterialTheme.typography.titleMedium)
        FilaDinero("Productos", d.productos, d.moneda)
        if (d.comision > 0) FilaDinero("Comisión Personal Shopper (${(d.porcentajeComision * 100).toInt()} %)", d.comision, d.moneda)
        if (d.envioInternacional > 0) FilaDinero("Envío internacional", d.envioInternacional, d.moneda)
        FilaDinero("Envío nacional", d.envioNacional, d.moneda)
        HorizontalDivider(color = Line)
        FilaDinero("Total a pagar", d.total, d.moneda, total = true)
    }
}
