package me.valeriaacosta.uyparceshopper.ui.components

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.launch
import me.valeriaacosta.uyparceshopper.data.firebase.FotosRepositorio
import me.valeriaacosta.uyparceshopper.data.models.Producto
import me.valeriaacosta.uyparceshopper.ui.theme.Red
import java.io.File

// ─── Fotos ───

/**
 * Foto de un producto: la foto real si tiene, y si no (o mientras carga) un bloque de su color.
 * indice: cuál de sus fotos. completa = true para verla grande (si no, usa la miniatura).
 */
@Composable
fun FotoProducto(
    producto: Producto,
    modifier: Modifier = Modifier,
    redondeo: Dp = 16.dp,
    marcarAgotado: Boolean = false,
    indice: Int = 0,
    completa: Boolean = false
) {
    Box(
        modifier.clip(RoundedCornerShape(redondeo)).background(Color(producto.colorFoto)),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Outlined.Checkroom, null, tint = Color.White.copy(alpha = 0.55f), modifier = Modifier.fillMaxSize(0.4f))
        producto.fotos.getOrNull(indice)?.let { id ->
            FotoGuardada(id, Modifier.fillMaxSize(), miniatura = !completa)
        }
        if (marcarAgotado && producto.agotado) {
            Text(
                "AGOTADO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                    .clip(RoundedCornerShape(6.dp)).background(Red).padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

/** Muestra una foto guardada en Firestore. Mientras carga se ve lo que haya detrás. */
@Composable
fun FotoGuardada(id: String, modifier: Modifier = Modifier, miniatura: Boolean = true, contentScale: ContentScale = ContentScale.Crop) {
    if (LocalInspectionMode.current) return // en las vistas previas no hay Firebase
    val bytes by produceState<ByteArray?>(initialValue = null, id, miniatura) {
        value = FotosRepositorio.leer(id, miniatura)
    }
    val datos = bytes ?: return
    AsyncImage(
        model = ImageRequest.Builder(LocalPlatformContext.current)
            .data(datos)
            .memoryCacheKey(if (miniatura) "${id}_min" else id)
            .crossfade(true)
            .build(),
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier
    )
}

/** Foto recién tomada o elegida (todavía no guardada), desde el archivo comprimido del celular. */
@Composable
fun FotoLocal(ruta: String, modifier: Modifier = Modifier) {
    AsyncImage(model = File(ruta), contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
}

/** Foto a pantalla completa (por ejemplo, el comprobante de pago). Se cierra tocándola. */
@Composable
fun VisorFoto(id: String, alCerrar: () -> Unit) {
    Dialog(onDismissRequest = alCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize().background(Color.Black).clickable(onClick = alCerrar),
            contentAlignment = Alignment.Center
        ) {
            FotoGuardada(id, Modifier.fillMaxSize(), miniatura = false, contentScale = ContentScale.Fit)
            IconButton(onClick = alCerrar, modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp)) {
                Icon(Icons.Outlined.Close, contentDescription = "Cerrar", tint = Color.White)
            }
        }
    }
}

/** Acciones para conseguir una foto: con la cámara o de la galería. */
class SelectorFoto(val tomarFoto: () -> Unit, val elegirDeGaleria: () -> Unit, val procesando: Boolean)

/**
 * Cámara (TakePicture) y galería (selector de fotos de Android); ninguna pide permisos.
 * La foto se comprime apenas se elige y se entrega la ruta del archivo comprimido.
 * Debe llamarse al principio de la pantalla (antes de cualquier "return"), para que el resultado de
 * la cámara llegue aunque Android haya cerrado la app mientras se tomaba la foto.
 */
@Composable
fun rememberSelectorFoto(alElegir: (ruta: String) -> Unit): SelectorFoto {
    val context = LocalContext.current
    val mensaje = LocalMensaje.current
    val scope = rememberCoroutineScope()
    var procesando by remember { mutableStateOf(false) }
    var rutaCamara by rememberSaveable { mutableStateOf<String?>(null) }
    val alElegirActual by rememberUpdatedState(alElegir)

    fun procesar(uri: Uri, borrarDespues: File? = null) {
        procesando = true
        scope.launch {
            try {
                alElegirActual(FotosRepositorio.preparar(context, uri))
            } catch (e: Exception) {
                mensaje("No se pudo leer la foto. Intenta con otra.")
            } finally {
                borrarDespues?.delete()
                procesando = false
            }
        }
    }

    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val ruta = rutaCamara
        rutaCamara = null
        if (ruta != null) {
            val archivo = File(ruta)
            if (ok && archivo.exists() && archivo.length() > 0) procesar(Uri.fromFile(archivo), archivo) else archivo.delete()
        }
    }
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) procesar(uri)
    }

    return SelectorFoto(
        tomarFoto = {
            val archivo = File(FotosRepositorio.carpeta(context), "camara_${System.currentTimeMillis()}.jpg")
            rutaCamara = archivo.absolutePath
            try {
                camara.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo))
            } catch (e: ActivityNotFoundException) {
                rutaCamara = null
                mensaje("Este celular no tiene una app de cámara")
            }
        },
        elegirDeGaleria = {
            try {
                galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            } catch (e: ActivityNotFoundException) {
                mensaje("No se encontró la galería")
            }
        },
        procesando = procesando
    )
}
