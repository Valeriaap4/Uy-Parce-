package me.valeriaacosta.uyparceshopper.ui.screens.supervisor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.valeriaacosta.uyparceshopper.data.DatosEjemplo
import me.valeriaacosta.uyparceshopper.data.models.TipoCaja
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── RecibirCajaScreen ───────────────────────────────────────────────────────

/**
 * Pantalla 28. Mayorista: verificar que llegó todo. Detal: escanear cada prenda y la app
 * dice a qué pedido va ("→ Pedido #12, Laura (1 de 3)") para meterla en su bolsa.
 */
@Composable
fun RecibirCajaScreen(
    codigo: String,
    onClickBack: () -> Unit,
    onClickTerminar: () -> Unit
) {
    val caja = DatosEjemplo.cajas.firstOrNull { it.codigo == codigo } ?: return NoEncontrado("Caja $codigo", onClickBack)
    val pedidos = caja.pedidos.ifEmpty { listOf(DatosEjemplo.pedido(1012)) }
    val esperado = pedidos.sumOf { it.prendas }
    var leidas by remember { mutableIntStateOf(pedidos.sumOf { it.recibidas }) }
    var escaner by remember { mutableStateOf(false) }
    var ultimo by remember { mutableStateOf<String?>(null) }

    UpScreen(
        titulo = if (caja.tipo == TipoCaja.DETAL) "Separar $codigo" else "Recibir $codigo",
        onClickBack = onClickBack,
        subtitulo = if (caja.tipo == TipoCaja.DETAL) "Detal · una bolsa por pedido" else "Mayorista · ${caja.cliente?.nombre}",
        barraInferior = {
            BotonAmarillo("Escanear", { escaner = true }, Modifier.weight(1.3f), Icons.Outlined.QrCodeScanner)
            BotonSecundario("Terminar", onClickTerminar, Modifier.weight(1f))
        }
    ) {
        ContenidoScroll {
            // ── Resultado del último escaneo ──
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Navy).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val texto = ultimo
                if (texto != null) {
                    Text("→ $texto", color = Yellow, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                    Text("Mete la prenda en la bolsa de ese pedido", color = NavyText)
                } else {
                    Text("$leidas / $esperado", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, fontFamily = Mono)
                    Text("prendas confirmadas", color = NavyText)
                }
            }
            LinearProgressIndicator(
                progress = { leidas / esperado.toFloat() },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                color = Green, trackColor = GreySoft
            )

            // ── Avance por pedido ──
            pedidos.forEach { p ->
                Tarjeta {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("#${p.numero}", fontFamily = Mono, color = Blue, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("  ${p.cliente.nombre}", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Chip("${p.recibidas} de ${p.prendas}", if (p.recibidas == p.prendas) TonoChip.VERDE else TonoChip.AMARILLO)
                    }
                    p.lineas.forEach { l ->
                        val completa = l.recibidas >= l.cantidad
                        Text(
                            "${if (completa) "✓" else "○"} ${l.cantidad} × ${l.producto.nombre} · ${l.talla}",
                            color = if (completa) Green else Muted
                        )
                    }
                }
            }
        }
    }

    if (escaner) DialogoEscaner(alLeer = {
        escaner = false
        if (leidas < esperado) leidas++
        val p = pedidos.first()
        ultimo = "Pedido #${p.numero}, ${p.cliente.nombre.substringBefore(" ")} (${(p.recibidas + 1).coerceAtMost(p.prendas)} de ${p.prendas})"
    }, alCerrar = { escaner = false })
}

@Preview(showBackground = true)
@Composable
fun RecibirCajaScreenPreview() {
    UyParceTheme { RecibirCajaScreen(codigo = "CJ-0230", onClickBack = {}, onClickTerminar = {}) }
}
