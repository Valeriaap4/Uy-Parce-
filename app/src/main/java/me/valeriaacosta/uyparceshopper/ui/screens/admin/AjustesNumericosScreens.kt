package me.valeriaacosta.uyparceshopper.ui.screens.admin

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import me.valeriaacosta.uyparceshopper.data.Listas
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.mensaje
import me.valeriaacosta.uyparceshopper.data.models.*
import me.valeriaacosta.uyparceshopper.ui.components.*

/*
 * Pantallas 47 a 50 y 52: ajustes con valores numéricos. Todas usan el mismo formulario validado
 * (FormularioNumeros), por eso viven juntas en este archivo. Al guardar se pide confirmación con el
 * "antes → después" y queda registrado en la auditoría. Los pedidos ya confirmados no cambian:
 * guardan sus valores congelados.
 */

/** Un campo del formulario: etiqueta, valor actual y rango permitido. */
data class CampoNumero(val etiqueta: String, val valor: Double, val rango: ClosedFloatingPointRange<Double>)

/** 4000.0 → "4000", 3.3 → "3.3" (para mostrar en un campo). */
fun numeroCampo(valor: Double): String =
    if (valor % 1.0 == 0.0) valor.toLong().toString() else valor.toBigDecimal().stripTrailingZeros().toPlainString()

// ─── FormularioNumeros ───────────────────────────────────────────────────────

/**
 * Formulario de números con validación de rango. guardar recibe los valores nuevos (en el mismo orden
 * de los campos) y un resumen del cambio; si sale bien se vuelve atrás.
 */
@Composable
fun FormularioNumeros(
    titulo: String,
    nota: String?,
    campos: List<CampoNumero>,
    onClickBack: () -> Unit,
    onGuardado: () -> Unit,
    guardar: suspend (valores: List<Double>, cambios: String) -> Result<Unit>
) {
    val estado = LocalEstadoApp.current
    val mensaje = LocalMensaje.current
    val scope = rememberCoroutineScope()
    // El formulario se arma con la cantidad de campos que hay (si cambian, se arma de nuevo).
    val valores = remember(campos.size) { mutableStateListOf(*campos.map { numeroCampo(it.valor) }.toTypedArray()) }
    var errores by remember(campos.size) { mutableStateOf(mapOf<Int, String>()) }
    var confirmar by remember { mutableStateOf<Pair<List<Double>, String>?>(null) }
    var guardando by remember { mutableStateOf(false) }

    if (estado.cargado(Listas.AJUSTES) && !estado.hayAjustes) {
        UpScreen(titulo, onClickBack) {
            ContenidoScroll { Aviso("Primero prepara la base de datos desde Ajustes.", TonoChip.AMARILLO, Icons.Outlined.Info) }
        }
        return
    }

    UpScreen(titulo, onClickBack, barraInferior = {
        BotonPrincipal("Guardar", {
            val leidos = campos.indices.map { i -> valores.getOrElse(i) { "" }.replace(",", ".").toDoubleOrNull() }
            errores = campos.indices.mapNotNull { i ->
                val v = leidos[i]
                val r = campos[i].rango
                if (v == null || v !in r) i to "Valor entre ${numeroCampo(r.start)} y ${numeroCampo(r.endInclusive)}" else null
            }.toMap()
            if (errores.isEmpty()) {
                val nuevos = leidos.map { it ?: 0.0 }
                val cambios = campos.indices.filter { nuevos[it] != campos[it].valor }
                    .joinToString(" · ") { "${campos[it].etiqueta}: ${numeroCampo(campos[it].valor)} → ${numeroCampo(nuevos[it])}" }
                if (cambios.isEmpty()) mensaje("No hay cambios para guardar") else confirmar = nuevos to cambios
            }
        }, Modifier.fillMaxWidth(), cargando = guardando)
    }) {
        ContenidoScroll {
            campos.forEachIndexed { i, c ->
                CampoTexto(c.etiqueta, valores.getOrElse(i) { "" }, { if (i < valores.size) valores[i] = it; errores = errores - i },
                    teclado = KeyboardType.Decimal, error = errores[i])
            }
            if (nota != null) Aviso(nota, TonoChip.AZUL)
        }
    }

    confirmar?.let { (nuevos, cambios) ->
        DialogoConfirmar("Guardar cambios", cambios, "Guardar", {
            guardando = true
            scope.launch {
                guardar(nuevos, cambios)
                    .onSuccess { mensaje("Ajustes guardados"); onGuardado() }
                    .onFailure { mensaje(it.mensaje()) }
                guardando = false
            }
        }, { confirmar = null })
    }
}

// ─── 47 · Precios detal ──────────────────────────────────────────────────────
@Composable
fun AjPreciosScreen(onClickBack: () -> Unit, onGuardado: () -> Unit) {
    val estado = LocalEstadoApp.current
    val a = estado.ajustes
    FormularioNumeros(
        "Precios detal",
        "Precio COP = (precio USD + envío estimado por peso) × tasa × (1 + margen). Los pedidos ya confirmados no cambian.",
        listOf(
            CampoNumero("Tasa del dólar (COP)", a.tasaDolar, 2000.0..8000.0),
            CampoNumero("Margen detal (%)", a.margenDetal * 100, 0.0..200.0)
        ),
        onClickBack, onGuardado
    ) { v, cambios ->
        estado.actualizarAjustes(mapOf("tasaDolar" to v[0], "margenDetal" to v[1] / 100), "Cambió precios detal", cambios)
    }
}

// ─── 48 · Comisiones ─────────────────────────────────────────────────────────
@Composable
fun AjComisionesScreen(onClickBack: () -> Unit, onGuardado: () -> Unit) {
    val estado = LocalEstadoApp.current
    val a = estado.ajustes
    FormularioNumeros(
        "Comisiones",
        "Los pedidos y facturas ya hechos conservan el porcentaje con el que se cobraron.",
        listOf(
            CampoNumero("Mayorista Avanzado (%)", a.comisionAvanzado * 100, 0.0..100.0),
            CampoNumero("Mayorista Parcero (%)", a.comisionParcero * 100, 0.0..100.0)
        ),
        onClickBack, onGuardado
    ) { v, cambios ->
        estado.actualizarAjustes(mapOf("comisionAvanzado" to v[0] / 100, "comisionParcero" to v[1] / 100), "Cambió comisiones", cambios)
    }
}

// ─── 49 · Envío internacional ────────────────────────────────────────────────
@Composable
fun AjEnvioScreen(onClickBack: () -> Unit, onGuardado: () -> Unit) {
    val estado = LocalEstadoApp.current
    val a = estado.ajustes
    FormularioNumeros(
        "Envío internacional",
        "Envío = peso × tarifa por libra + seguro por caja. Al cliente se le cobra un estimado con la tarifa Regular.",
        listOf(
            CampoNumero("Regular (USD/lb)", a.tarifaRegular, 0.5..50.0),
            CampoNumero("Prime 300 (USD/lb)", a.tarifaPrime300, 0.5..50.0),
            CampoNumero("Prime 800 (USD/lb)", a.tarifaPrime800, 0.5..50.0),
            CampoNumero("Seguro por caja (USD)", a.seguroUsd, 0.0..500.0)
        ),
        onClickBack, onGuardado
    ) { v, cambios ->
        estado.actualizarAjustes(
            mapOf("tarifaRegular" to v[0], "tarifaPrime300" to v[1], "tarifaPrime800" to v[2], "seguroUsd" to v[3]),
            "Cambió tarifas de envío", cambios
        )
    }
}

// ─── 50 · Zonas de envío nacional ────────────────────────────────────────────
@Composable
fun AjZonasScreen(onClickBack: () -> Unit, onGuardado: () -> Unit) {
    val estado = LocalEstadoApp.current
    val zonas = estado.ajustes.zonas
    FormularioNumeros(
        "Zonas de envío",
        "Ciudades: " + zonas.joinToString(" | ") { "${it.nombre}: ${it.ciudades.joinToString()}" },
        zonas.map { CampoNumero("${it.nombre} (COP)", it.precioCop.toDouble(), 0.0..200000.0) },
        onClickBack, onGuardado
    ) { v, cambios ->
        estado.actualizarAjustes(mapOf("preciosZona" to v.map { it.toInt() }), "Cambió precios de zonas", cambios)
    }
}

// ─── 52 · Categorías y pesos ─────────────────────────────────────────────────
@Composable
fun AjCategoriasScreen(onClickBack: () -> Unit, onGuardado: () -> Unit) {
    val estado = LocalEstadoApp.current
    val categorias = estado.ajustes.categorias
    FormularioNumeros(
        "Categorías y pesos",
        "Peso estimado en libras. Se usa para el envío y el precio detal de los productos nuevos " +
            "(los productos ya creados conservan su peso; se cambia editando el producto).",
        categorias.map { CampoNumero("${it.nombre} (lb)", it.pesoLb, 0.1..30.0) },
        onClickBack, onGuardado
    ) { v, cambios ->
        estado.actualizarAjustes(
            mapOf("categorias" to categorias.mapIndexed { i, c -> c.copy(pesoLb = v[i]) }), "Cambió pesos de categorías", cambios
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AjPreciosScreenPreview() {
    VistaPrevia(Rol.ADMIN) { AjPreciosScreen(onClickBack = {}, onGuardado = {}) }
}
