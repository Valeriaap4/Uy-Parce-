package me.valeriaacosta.uyparceshopper.ui.screens.supervisor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
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
import kotlinx.coroutines.launch
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.buscarPorCodigo
import me.valeriaacosta.uyparceshopper.data.mensaje
import me.valeriaacosta.uyparceshopper.data.models.*
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── RecibirCajaScreen ───────────────────────────────────────────────────────

/**
 * Pantalla 28. Se escanea cada prenda de la caja: la app la marca recibida y dice a qué pedido va
 * ("→ Pedido #1012, Laura (1 de 3)") para meterla en su bolsa. Las unidades iguales se asignan al
 * pedido más antiguo de la caja que todavía la espera.
 */
@Composable
fun RecibirCajaScreen(
    codigo: String,
    onClickBack: () -> Unit,
    onClickTerminar: () -> Unit
) {
    SegunCarga(rememberCaja(codigo), "Caja $codigo", onClickBack) { caja -> RecibirContenido(caja, onClickBack, onClickTerminar) }
}

@Composable
private fun RecibirContenido(caja: Caja, onClickBack: () -> Unit, onClickTerminar: () -> Unit) {
    val estado = LocalEstadoApp.current
    val mensaje = LocalMensaje.current
    val scope = rememberCoroutineScope()
    var escaner by remember { mutableStateOf(false) }
    var ultimo by remember { mutableStateOf<String?>(null) }
    var trabajando by remember { mutableStateOf(false) }
    val enCamino = caja.estado == EstadoCaja.ENVIADA || caja.estado == EstadoCaja.EN_CUCUTA
    val porPedido = caja.contenido.groupBy { it.numeroPedido }.toSortedMap()

    fun nombreCliente(numero: Int) = estado.pedidoEnMemoria(numero)?.cliente?.nombre ?: ""

    fun recibir(productoId: String, talla: String) {
        trabajando = true
        scope.launch {
            estado.recibirPrenda(caja, productoId, talla)
                .onSuccess { prenda ->
                    val delPedido = caja.contenido.filter { it.numeroPedido == prenda.numeroPedido }
                    val listas = delPedido.count { it.recibida } + 1
                    ultimo = "Pedido #${prenda.numeroPedido}, ${nombreCliente(prenda.numeroPedido).substringBefore(" ")} " +
                        "(${listas.coerceAtMost(delPedido.size)} de ${delPedido.size})"
                }
                .onFailure { mensaje(it.mensaje()) }
            trabajando = false
        }
    }

    UpScreen(
        titulo = if (caja.tipo == TipoCaja.DETAL) "Separar ${caja.codigo}" else "Recibir ${caja.codigo}",
        onClickBack = onClickBack,
        subtitulo = if (caja.tipo == TipoCaja.DETAL) "Detal · una bolsa por pedido" else "Mayorista · ${caja.clienteNombre ?: ""}",
        barraInferior = if (!enCamino) null else ({
            BotonAmarillo("Escanear", { escaner = true }, Modifier.weight(1.3f), Icons.Outlined.QrCodeScanner, cargando = trabajando)
            BotonSecundario("Terminar", onClickTerminar, Modifier.weight(1f))
        })
    ) {
        ContenidoScroll {
            if (!enCamino) Aviso("Esta caja ya se cerró (${caja.estado.etiqueta.lowercase()}).", TonoChip.AZUL, Icons.Outlined.Info)
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
                    Text("${caja.recibidas} / ${caja.prendas}", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, fontFamily = Mono)
                    Text("prendas confirmadas", color = NavyText)
                }
            }
            LinearProgressIndicator(
                progress = { if (caja.prendas == 0) 0f else caja.recibidas / caja.prendas.toFloat() },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                color = Green, trackColor = GreySoft
            )

            // ── Avance por pedido ──
            porPedido.forEach { (numero, prendas) ->
                val recibidas = prendas.count { it.recibida }
                Tarjeta {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("#$numero", fontFamily = Mono, color = Blue, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("  ${nombreCliente(numero)}", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Chip("$recibidas de ${prendas.size}", if (recibidas == prendas.size) TonoChip.VERDE else TonoChip.AMARILLO)
                    }
                    prendas.groupBy { it.productoId to it.talla }.forEach { (_, grupo) ->
                        val llegaron = grupo.count { it.recibida }
                        val completa = llegaron == grupo.size
                        val primera = grupo.first()
                        Text(
                            "${if (completa) "✓" else "○"} ${grupo.size} × ${primera.nombre} · ${primera.talla}" +
                                if (!completa) " (llegaron $llegaron)" else "",
                            color = if (completa) Green else Muted
                        )
                    }
                }
            }
            if (enCamino) {
                val pendientes = caja.contenido.filter { !it.recibida }.groupBy { it.productoId to it.talla }
                if (pendientes.isNotEmpty()) {
                    TituloSeccion("¿El código no se deja leer?")
                    Text("Toca la prenda que tienes en la mano:", color = Muted, fontSize = 13.sp)
                    pendientes.forEach { (clave, grupo) ->
                        Tarjeta(alPulsar = { if (!trabajando) recibir(clave.first, clave.second) }) {
                            Text("${grupo.first().nombre} · ${clave.second}", fontWeight = FontWeight.SemiBold)
                            Text("Faltan ${grupo.size}", color = Muted, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    if (escaner) DialogoEscaner(alLeer = { codigo ->
        escaner = false
        val encontrado = buscarPorCodigo(estado.productos, codigo)
        if (encontrado == null) mensaje("Ese código no está guardado en ningún producto. Toca la prenda en la lista.")
        else recibir(encontrado.producto.id, encontrado.talla)
    }, alCerrar = { escaner = false })
}

@Preview(showBackground = true)
@Composable
fun RecibirCajaScreenPreview() {
    VistaPrevia(Rol.SUPERVISOR) { RecibirCajaScreen(codigo = "CJ-0230", onClickBack = {}, onClickTerminar = {}) }
}
