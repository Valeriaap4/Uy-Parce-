package me.valeriaacosta.uyparceshopper.ui.components

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import me.valeriaacosta.uyparceshopper.data.models.Desglose
import me.valeriaacosta.uyparceshopper.data.models.Precios
import me.valeriaacosta.uyparceshopper.data.models.porcentaje
import me.valeriaacosta.uyparceshopper.ui.theme.Line

// ─── TarjetaDesglose ─────────────────────────────────────────────────────────

/**
 * Desglose del total según el tipo de cliente (productos, comisión, envíos). Si el cliente usó saldo
 * a favor, se muestra el total del pedido, el saldo descontado y lo que queda por pagar.
 */
@Composable
fun TarjetaDesglose(d: Desglose, titulo: String = "Resumen", saldoAplicado: Double = 0.0) {
    Tarjeta {
        Text(titulo, style = MaterialTheme.typography.titleMedium)
        FilaDinero("Productos", d.productos, d.moneda)
        if (d.comision > 0) FilaDinero("Comisión Personal Shopper (${porcentaje(d.porcentajeComision)})", d.comision, d.moneda)
        if (d.envioInternacional > 0) FilaDinero("Envío internacional", d.envioInternacional, d.moneda)
        FilaDinero("Envío nacional", d.envioNacional, d.moneda)
        HorizontalDivider(color = Line)
        if (saldoAplicado > 0) {
            FilaDinero("Total del pedido", d.total, d.moneda)
            FilaDinero("Saldo a favor", -saldoAplicado, d.moneda)
            FilaDinero("Total a pagar", Precios.redondear((d.total - saldoAplicado).coerceAtLeast(0.0), d.moneda), d.moneda, total = true)
        } else {
            FilaDinero("Total a pagar", d.total, d.moneda, total = true)
        }
    }
}
