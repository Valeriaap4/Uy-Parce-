package me.valeriaacosta.uyparceshopper.ui.screens.supervisor

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import me.valeriaacosta.uyparceshopper.data.DatosEjemplo
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── ResumenRecepcionScreen ──────────────────────────────────────────────────

/** Pantalla 29. Qué llegó y qué faltó de cada pedido. Los faltantes se avisan al cliente. */
@Composable
fun ResumenRecepcionScreen(
    codigo: String,
    onClickBack: () -> Unit,
    onCerrada: () -> Unit
) {
    val caja = DatosEjemplo.cajas.firstOrNull { it.codigo == codigo } ?: return NoEncontrado("Caja $codigo", onClickBack)
    val pedidos = caja.pedidos.ifEmpty { listOf(DatosEjemplo.pedido(1012)) }
    val mensaje = LocalMensaje.current
    val faltan = pedidos.sumOf { it.prendas - it.recibidas }

    UpScreen("Resumen $codigo", onClickBack, barraInferior = {
        BotonPrincipal("Cerrar recepción", { mensaje("Recepción cerrada · clientes avisados"); onCerrada() }, Modifier.fillMaxWidth())
    }) {
        ContenidoScroll {
            if (faltan > 0) Aviso("${if (faltan == 1) "Faltó 1 prenda" else "Faltaron $faltan prendas"}. Avisaremos a los clientes para que elijan saldo a favor o devolución.",
                TonoChip.ROJO, Icons.Outlined.ErrorOutline)
            else Aviso("Llegó todo completo.", TonoChip.VERDE, Icons.Outlined.CheckCircle)

            pedidos.forEach { p ->
                val completo = p.recibidas == p.prendas
                Tarjeta {
                    Text("#${p.numero} · ${p.cliente.nombre}", fontWeight = FontWeight.Bold)
                    p.lineas.forEach { l ->
                        Row {
                            Text("${l.producto.nombre} · ${l.talla}", modifier = Modifier.weight(1f), color = Muted)
                            Text("${l.recibidas}/${l.cantidad}", fontFamily = Mono, fontWeight = FontWeight.Bold,
                                color = if (l.recibidas < l.cantidad) Red else Green)
                        }
                    }
                    Chip(if (completo) "Listo para despachar" else "Con faltantes", if (completo) TonoChip.VERDE else TonoChip.ROJO)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ResumenRecepcionScreenPreview() {
    UyParceTheme { ResumenRecepcionScreen(codigo = "CJ-0230", onClickBack = {}, onCerrada = {}) }
}
