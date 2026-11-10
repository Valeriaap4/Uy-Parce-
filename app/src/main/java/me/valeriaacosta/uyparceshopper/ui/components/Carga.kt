package me.valeriaacosta.uyparceshopper.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.valeriaacosta.uyparceshopper.ui.theme.Navy

// ─── Estados de carga ───

/**
 * Algo que llega de Firebase puede estar cargando, no existir o estar listo
 * (la misma idea del MarsUiState visto en clase).
 */
sealed interface Carga<out T> {
    data object Cargando : Carga<Nothing>
    data object NoExiste : Carga<Nothing>
    data class Listo<out T>(val valor: T) : Carga<T>
}

/** Rueda de "cargando". */
@Composable
fun Cargando(modifier: Modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp)) {
    Box(modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Navy)
    }
}

/**
 * Para pantallas de detalle: muestra "cargando" mientras llega el dato, "no encontrado" si de verdad
 * no existe, y el contenido cuando está listo. Así nunca se ve "no encontrado" mientras carga.
 */
@Composable
fun <T> SegunCarga(carga: Carga<T>, titulo: String, onClickBack: (() -> Unit)?, contenido: @Composable (T) -> Unit) {
    when (carga) {
        is Carga.Listo -> contenido(carga.valor)
        Carga.Cargando -> UpScreen(titulo, onClickBack) { Cargando(Modifier.fillMaxSize()) }
        Carga.NoExiste -> NoEncontrado(titulo, onClickBack)
    }
}
