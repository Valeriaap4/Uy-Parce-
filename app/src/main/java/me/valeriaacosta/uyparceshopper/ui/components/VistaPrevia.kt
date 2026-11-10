package me.valeriaacosta.uyparceshopper.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import me.valeriaacosta.uyparceshopper.data.EstadoApp
import me.valeriaacosta.uyparceshopper.data.LocalEstadoApp
import me.valeriaacosta.uyparceshopper.data.models.Rol
import me.valeriaacosta.uyparceshopper.ui.theme.UyParceTheme

/**
 * Envoltorio para los @Preview: tema de la app + datos de ejemplo del rol indicado, sin tocar Firebase.
 * Uso: VistaPrevia(Rol.SUPERVISOR) { PagosScreen(...) }
 */
@Composable
fun VistaPrevia(rol: Rol = Rol.CLIENTE, contenido: @Composable () -> Unit) {
    val estado = remember(rol) { EstadoApp.ejemplo(rol) }
    UyParceTheme {
        CompositionLocalProvider(LocalEstadoApp provides estado) { contenido() }
    }
}
