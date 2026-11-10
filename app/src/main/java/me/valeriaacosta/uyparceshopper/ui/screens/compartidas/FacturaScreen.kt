package me.valeriaacosta.uyparceshopper.ui.screens.compartidas

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.valeriaacosta.uyparceshopper.data.FacturaPdf
import me.valeriaacosta.uyparceshopper.data.fecha
import me.valeriaacosta.uyparceshopper.data.models.*
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── FacturaScreen ───────────────────────────────────────────────────────────

/**
 * Pantalla 15. Factura del pedido. Se genera al aprobar el pago y queda congelada (no cambia si
 * después cambian precios o estados). Se puede descargar en PDF o compartir.
 */
@Composable
fun FacturaScreen(
    numero: Int,
    onClickBack: () -> Unit
) {
    when (val carga = rememberFactura(numero)) {
        is Carga.Listo -> FacturaContenido(carga.valor, onClickBack)
        Carga.Cargando -> UpScreen("Factura", onClickBack) { Cargando(Modifier.fillMaxSize()) }
        Carga.NoExiste -> UpScreen("Factura", onClickBack) {
            EstadoVacio(Icons.Outlined.HourglassTop, "Este pedido todavía no tiene factura: se genera cuando aprobamos el pago.")
        }
    }
}

@Composable
private fun FacturaContenido(factura: Factura, onClickBack: () -> Unit) {
    val context = LocalContext.current
    val mensaje = LocalMensaje.current
    val scope = rememberCoroutineScope()
    val pedido = factura.pedido
    val tipo = pedido.cliente.tipoCliente ?: TipoCliente.DETAL
    val d = pedido.desglose
    var trabajando by remember { mutableStateOf(false) }

    // "Descargar": la persona elige dónde guardar el PDF (Descargas, Drive…). No necesita permisos.
    val guardar = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { destino ->
        if (destino == null) return@rememberLauncherForActivityResult
        trabajando = true
        scope.launch {
            try {
                FacturaPdf.guardarEn(context, FacturaPdf.crear(context, factura), destino)
                mensaje("Factura guardada")
            } catch (e: Exception) {
                mensaje("No se pudo guardar el PDF")
            }
            trabajando = false
        }
    }

    UpScreen("Factura", onClickBack, barraInferior = {
        BotonPrincipal("Descargar PDF", {
            try {
                guardar.launch("${factura.numero}.pdf")
            } catch (e: ActivityNotFoundException) {
                mensaje("Este celular no permite elegir dónde guardar")
            }
        }, Modifier.weight(1f), Icons.Outlined.Download, cargando = trabajando)
        BotonSecundario("Compartir", {
            trabajando = true
            scope.launch {
                try {
                    FacturaPdf.compartir(context, FacturaPdf.crear(context, factura), factura.numero)
                } catch (e: Exception) {
                    mensaje("No se pudo compartir el PDF")
                }
                trabajando = false
            }
        }, Modifier.weight(1f), Icons.Outlined.Share, Blue, habilitado = !trabajando)
    }) {
        ContenidoScroll {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(Green),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(30.dp)) }
                Column(modifier = Modifier.weight(1f)) {
                    Text(factura.numero, style = MaterialTheme.typography.titleLarge)
                    Text("${pedido.cliente.nombre} · ${fecha(factura.fecha)}", color = Muted)
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
                        Text("Factura ${factura.numero} · Pedido #${pedido.numero}", color = Muted)
                    }
                }
                HorizontalDivider(color = Line)
                FilaDato("Cliente", pedido.cliente.nombre, true)
                FilaDato("Cédula", pedido.cliente.cedula)
                FilaDato("Tipo", "${tipo.etiqueta} · ${d.moneda}", true)
                HorizontalDivider(color = Line)
                pedido.lineas.forEach { l ->
                    Row {
                        Text("${l.cantidad} × ${l.producto.nombre} · ${l.talla}", color = Muted, modifier = Modifier.weight(1f))
                        Text(formatoDinero(l.precioUnitario * l.cantidad, d.moneda), fontFamily = Mono, fontWeight = FontWeight.Bold)
                    }
                }
                HorizontalDivider(color = Line)
                FilaDinero("Subtotal", d.productos, d.moneda)
                if (d.comision > 0) FilaDinero("Comisión UyParce (${porcentaje(d.porcentajeComision)})", d.comision, d.moneda)
                if (d.envioInternacional > 0) FilaDinero("Envío internacional", d.envioInternacional, d.moneda)
                FilaDinero("Envío nacional", d.envioNacional, d.moneda)
                if (pedido.saldoAplicado > 0) {
                    FilaDinero("Total del pedido", d.total, d.moneda)
                    FilaDinero("Saldo a favor", -pedido.saldoAplicado, d.moneda)
                }
                FilaDinero("Total pagado", pedido.totalAPagar, d.moneda, total = true)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FacturaScreenPreview() {
    VistaPrevia { FacturaScreen(numero = 1012, onClickBack = {}) }
}
