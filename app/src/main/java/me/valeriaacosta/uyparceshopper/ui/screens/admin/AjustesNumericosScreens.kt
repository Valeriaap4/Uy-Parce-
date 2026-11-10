package me.valeriaacosta.uyparceshopper.ui.screens.admin

import me.valeriaacosta.uyparceshopper.data.*
import me.valeriaacosta.uyparceshopper.data.models.*

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import me.valeriaacosta.uyparceshopper.data.DatosEjemplo
import me.valeriaacosta.uyparceshopper.ui.components.*
import me.valeriaacosta.uyparceshopper.ui.theme.UyParceTheme

/*
 * Pantallas 47 a 50 y 52: ajustes con valores numéricos. Todas usan el mismo
 * formulario validado (FormularioNumeros), por eso viven juntas en este archivo.
 */

/** Un campo del formulario: etiqueta, valor inicial y rango permitido. */
data class CampoNumero(val etiqueta: String, val valor: String, val rango: ClosedFloatingPointRange<Double>)

// ─── FormularioNumeros ───────────────────────────────────────────────────────

@Composable
fun FormularioNumeros(
    titulo: String,
    nota: String?,
    campos: List<CampoNumero>,
    onClickBack: () -> Unit,
    onGuardado: () -> Unit
) {
    val mensaje = LocalMensaje.current
    val valores = remember { mutableStateListOf(*campos.map { it.valor }.toTypedArray()) }
    var errores by remember { mutableStateOf(mapOf<Int, String>()) }

    UpScreen(titulo, onClickBack, barraInferior = {
        BotonPrincipal("Guardar", {
            errores = campos.indices.mapNotNull { i ->
                val v = valores[i].replace(",", ".").toDoubleOrNull()
                val r = campos[i].rango
                if (v == null || v !in r) i to "Valor entre ${r.start} y ${r.endInclusive}" else null
            }.toMap()
            if (errores.isEmpty()) {
                mensaje("Ajustes guardados")
                onGuardado()
            }
        }, Modifier.fillMaxWidth())
    }) {
        ContenidoScroll {
            campos.forEachIndexed { i, c ->
                CampoTexto(c.etiqueta, valores[i], { valores[i] = it; errores = errores - i }, teclado = KeyboardType.Decimal, error = errores[i])
            }
            if (nota != null) Aviso(nota, TonoChip.AZUL)
        }
    }
}

// ─── 47 · Precios detal ──────────────────────────────────────────────────────
@Composable
fun AjPreciosScreen(onClickBack: () -> Unit, onGuardado: () -> Unit) {
    val a = DatosEjemplo.ajustes
    FormularioNumeros(
        "Precios detal",
        "Precio COP = (precio USD + envío estimado por peso) × tasa × (1 + margen). Los pedidos ya confirmados no cambian.",
        listOf(
            CampoNumero("Tasa del dólar (COP)", a.tasaDolar.toInt().toString(), 2000.0..8000.0),
            CampoNumero("Margen detal (%)", porcentaje(a.margenDetal).removeSuffix(" %"), 0.0..200.0)
        ),
        onClickBack, onGuardado
    )
}

// ─── 48 · Comisiones ─────────────────────────────────────────────────────────
@Composable
fun AjComisionesScreen(onClickBack: () -> Unit, onGuardado: () -> Unit) {
    val a = DatosEjemplo.ajustes
    FormularioNumeros(
        "Comisiones",
        "Las facturas ya generadas conservan el porcentaje con el que se cobraron.",
        listOf(
            CampoNumero("Mayorista Avanzado (%)", porcentaje(a.comisionAvanzado).removeSuffix(" %"), 0.0..100.0),
            CampoNumero("Mayorista Parcero (%)", porcentaje(a.comisionParcero).removeSuffix(" %"), 0.0..100.0)
        ),
        onClickBack, onGuardado
    )
}

// ─── 49 · Envío internacional ────────────────────────────────────────────────
@Composable
fun AjEnvioScreen(onClickBack: () -> Unit, onGuardado: () -> Unit) {
    val a = DatosEjemplo.ajustes
    FormularioNumeros(
        "Envío internacional",
        "Envío = peso × tarifa por libra + seguro por caja.",
        listOf(
            CampoNumero("Regular (USD/lb)", a.tarifaRegular.toString(), 0.5..50.0),
            CampoNumero("Prime 300 (USD/lb)", a.tarifaPrime300.toString(), 0.5..50.0),
            CampoNumero("Prime 800 (USD/lb)", a.tarifaPrime800.toString(), 0.5..50.0),
            CampoNumero("Seguro por caja (USD)", a.seguroUsd.toInt().toString(), 0.0..500.0)
        ),
        onClickBack, onGuardado
    )
}

// ─── 50 · Zonas de envío nacional ────────────────────────────────────────────
@Composable
fun AjZonasScreen(onClickBack: () -> Unit, onGuardado: () -> Unit) {
    FormularioNumeros(
        "Zonas de envío",
        "Ciudades: " + DatosEjemplo.zonas.joinToString(" | ") { "${it.nombre}: ${it.ciudades.joinToString()}" },
        DatosEjemplo.zonas.map { CampoNumero("${it.nombre} (COP)", it.precioCop.toString(), 0.0..200000.0) },
        onClickBack, onGuardado
    )
}

// ─── 52 · Categorías y pesos ─────────────────────────────────────────────────
@Composable
fun AjCategoriasScreen(onClickBack: () -> Unit, onGuardado: () -> Unit) {
    FormularioNumeros(
        "Categorías y pesos",
        "Peso estimado en libras. Se usa para el envío y el precio detal.",
        DatosEjemplo.categorias.map { CampoNumero("${it.nombre} (lb)", it.pesoLb.toString(), 0.1..30.0) },
        onClickBack, onGuardado
    )
}

@Preview(showBackground = true)
@Composable
fun AjPreciosScreenPreview() {
    UyParceTheme { AjPreciosScreen(onClickBack = {}, onGuardado = {}) }
}
