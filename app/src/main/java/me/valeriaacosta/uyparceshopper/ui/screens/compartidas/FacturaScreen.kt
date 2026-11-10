package me.valeriaacosta.uyparceshopper.ui.screens.compartidas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.valeriaacosta.uyparceshopper.data.DatosEjemplo
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.models.Precios
import me.valeriaacosta.uyparceshopper.data.models.TipoCliente
import me.valeriaacosta.uyparceshopper.data.models.formatoDinero
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── FacturaScreen ───────────────────────────────────────────────────────────

/** Pantalla 15. Factura del pedido (se genera al aprobar el pago). */
@Composable
fun FacturaScreen(
    numero: Int,
    onClickBack: () -> Unit
) {
    val estado = LocalEstadoApp.current
    val pedido = DatosEjemplo.buscarPedido(numero) ?: return NoEncontrado("Factura", onClickBack)
    val factura = DatosEjemplo.facturas.firstOrNull { it.pedido.numero == numero }
    val tipo = pedido.cliente.tipoCliente ?: TipoCliente.DETAL
    val d = Precios.desglose(pedido, estado.ajustes)
    val mensaje = LocalMensaje.current
    val num = factura?.numero ?: "FAC-0143"

    UpScreen("Factura", onClickBack, barraInferior = {
        // Siguiente fase: PdfDocument para generar el PDF y un Intent para compartirlo.
        BotonPrincipal("Descargar PDF", { mensaje("PDF guardado en Descargas") }, Modifier.weight(1f), Icons.Outlined.Download)
        BotonSecundario("Compartir", { mensaje("Abriendo compartir…") }, Modifier.weight(1f), Icons.Outlined.Share, Blue)
    }) {
        ContenidoScroll {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(Green),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(30.dp)) }
                Column(modifier = Modifier.weight(1f)) {
                    Text(num, style = MaterialTheme.typography.titleLarge)
                    Text("${pedido.cliente.nombre} · ${factura?.fecha ?: pedido.fecha}", color = Muted)
                }
                Chip("Pagada", TonoChip.VERDE)
            }
            Tarjeta {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Navy).padding(4.dp)) {
                        LogoUyParce(alto = 52.dp)
                    }
                    Column {
                        Text("UyParce Shopper", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("Factura $num · Pedido #$numero", color = Muted)
                    }
                }
                HorizontalDivider(color = Line)
                FilaDato("Cliente", pedido.cliente.nombre, true)
                FilaDato("Cédula", pedido.cliente.cedula)
                FilaDato("Tipo", "${tipo.etiqueta} · ${tipo.moneda}", true)
                HorizontalDivider(color = Line)
                pedido.lineas.forEach { l ->
                    Row {
                        Text("${l.cantidad} × ${l.producto.nombre} · ${l.talla}", color = Muted, modifier = Modifier.weight(1f))
                        Text(
                            formatoDinero(Precios.precioUnitario(l.producto, tipo, estado.ajustes) * l.cantidad, tipo.moneda),
                            fontFamily = Mono, fontWeight = FontWeight.Bold
                        )
                    }
                }
                HorizontalDivider(color = Line)
                FilaDinero("Subtotal", d.productos, d.moneda)
                if (d.comision > 0) FilaDinero("Comisión UyParce (${(d.porcentajeComision * 100).toInt()} %)", d.comision, d.moneda)
                if (d.envioInternacional > 0) FilaDinero("Envío internacional", d.envioInternacional, d.moneda)
                FilaDinero("Envío nacional", d.envioNacional, d.moneda)
                FilaDinero("Total", d.total, d.moneda, total = true)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FacturaScreenPreview() {
    UyParceTheme { FacturaScreen(numero = 1014, onClickBack = {}) }
}
