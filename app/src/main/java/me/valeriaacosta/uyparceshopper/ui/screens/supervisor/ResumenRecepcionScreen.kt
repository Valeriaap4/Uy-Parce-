package me.valeriaacosta.uyparceshopper.ui.screens.supervisor

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.mensaje
import me.valeriaacosta.uyparceshopper.data.models.*
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.*

// ─── ResumenRecepcionScreen ──────────────────────────────────────────────────

/**
 * Pantalla 29. Qué llegó y qué faltó de cada pedido de la caja. Al cerrar la recepción, los
 * pedidos que ya tienen todo en Cúcuta pasan a "En Cúcuta" y lo que no llegó queda como faltante
 * (se le avisa a cada cliente para que elija saldo a favor o devolución).
 */
@Composable
fun ResumenRecepcionScreen(
    codigo: String,
    onClickBack: () -> Unit,
    onCerrada: () -> Unit
) {
    SegunCarga(rememberCaja(codigo), "Resumen $codigo", onClickBack) { caja -> ResumenContenido(caja, onClickBack, onCerrada) }
}

@Composable
private fun ResumenContenido(caja: Caja, onClickBack: () -> Unit, onCerrada: () -> Unit) {
    val estado = LocalEstadoApp.current
    val mensaje = LocalMensaje.current
    val scope = rememberCoroutineScope()
    var confirmar by remember { mutableStateOf(false) }
    var cerrando by remember { mutableStateOf(false) }
    val faltan = caja.prendas - caja.recibidas
    val abierta = caja.estado == EstadoCaja.ENVIADA || caja.estado == EstadoCaja.EN_CUCUTA

    UpScreen("Resumen ${caja.codigo}", onClickBack, barraInferior = if (!abierta) null else ({
        BotonPrincipal("Cerrar recepción", { confirmar = true }, Modifier.fillMaxWidth(), cargando = cerrando)
    })) {
        ContenidoScroll {
            when {
                !abierta -> Aviso("La recepción de esta caja ya se cerró.", TonoChip.AZUL, Icons.Outlined.Info)
                faltan > 0 -> Aviso(
                    "${if (faltan == 1) "Falta 1 prenda" else "Faltan $faltan prendas"} por escanear. Si de verdad no llegaron, " +
                        "al cerrar se avisa a los clientes para que elijan saldo a favor o devolución.",
                    TonoChip.ROJO, Icons.Outlined.ErrorOutline
                )
                else -> Aviso("Llegó todo completo.", TonoChip.VERDE, Icons.Outlined.CheckCircle)
            }
            caja.contenido.groupBy { it.numeroPedido }.toSortedMap().forEach { (numero, prendas) ->
                val completo = prendas.all { it.recibida }
                Tarjeta {
                    Text("#$numero · ${estado.pedidoEnMemoria(numero)?.cliente?.nombre ?: ""}", fontWeight = FontWeight.Bold)
                    prendas.groupBy { it.productoId to it.talla }.forEach { (_, grupo) ->
                        val llegaron = grupo.count { it.recibida }
                        Row {
                            Text("${grupo.first().nombre} · ${grupo.first().talla}", modifier = Modifier.weight(1f), color = Muted)
                            Text("$llegaron/${grupo.size}", fontFamily = Mono, fontWeight = FontWeight.Bold,
                                color = if (llegaron < grupo.size) Red else Green)
                        }
                    }
                    Chip(if (completo) "Llegó completo" else "Con faltantes", if (completo) TonoChip.VERDE else TonoChip.ROJO)
                }
            }
        }
    }

    if (confirmar) DialogoConfirmar(
        "Cerrar recepción",
        if (faltan > 0) "Se registran ${cantidad(faltan, "prenda")} como faltantes y se avisa a los clientes. Ya no se podrán escanear más prendas de esta caja."
        else "La caja queda recibida y sus pedidos listos para despachar.",
        "Cerrar recepción", {
            cerrando = true
            scope.launch {
                estado.cerrarRecepcion(caja)
                    .onSuccess { n -> mensaje(if (n > 0) "Recepción cerrada · ${cantidad(n, "pedido")} con faltantes, ya avisamos" else "Recepción cerrada"); onCerrada() }
                    .onFailure { mensaje(it.mensaje()) }
                cerrando = false
            }
        }, { confirmar = false }, peligro = faltan > 0
    )
}

@Preview(showBackground = true)
@Composable
fun ResumenRecepcionScreenPreview() {
    VistaPrevia(Rol.SUPERVISOR) { ResumenRecepcionScreen(codigo = "CJ-0230", onClickBack = {}, onCerrada = {}) }
}
