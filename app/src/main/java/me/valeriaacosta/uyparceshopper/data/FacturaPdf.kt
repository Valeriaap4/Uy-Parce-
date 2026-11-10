package me.valeriaacosta.uyparceshopper.data

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.valeriaacosta.uyparceshopper.R
import me.valeriaacosta.uyparceshopper.data.models.Factura
import me.valeriaacosta.uyparceshopper.data.models.formatoDinero
import me.valeriaacosta.uyparceshopper.data.models.porcentaje
import java.io.File

/**
 * Arma el PDF de una factura con PdfDocument (viene con Android, no necesita librerías) y lo
 * comparte o lo guarda donde la persona elija.
 */
object FacturaPdf {

    private const val ANCHO = 595 // tamaño A4 en puntos
    private const val ALTO = 842
    private const val MARGEN = 40f
    private const val NAVY = 0xFF0B1230.toInt()
    private const val GRIS = 0xFF6B7280.toInt()
    private const val LINEA = 0xFFE3E7EF.toInt()

    /** Crea el PDF en el caché de la app y devuelve el archivo. */
    suspend fun crear(context: Context, factura: Factura): File = withContext(Dispatchers.IO) {
        val pdf = PdfDocument()
        val texto = Paint().apply { color = NAVY; textSize = 11f; isAntiAlias = true }
        val negrita = Paint(texto).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val gris = Paint(texto).apply { color = GRIS; textSize = 10f }
        val titulo = Paint(negrita).apply { textSize = 20f }
        val linea = Paint().apply { color = LINEA; strokeWidth = 1f }

        var numeroPagina = 1
        var pagina = pdf.startPage(PdfDocument.PageInfo.Builder(ANCHO, ALTO, numeroPagina).create())
        var canvas: Canvas = pagina.canvas
        var y = MARGEN

        fun nuevaPaginaSiHaceFalta(alto: Float) {
            if (y + alto <= ALTO - MARGEN - 30) return
            pdf.finishPage(pagina)
            numeroPagina++
            pagina = pdf.startPage(PdfDocument.PageInfo.Builder(ANCHO, ALTO, numeroPagina).create())
            canvas = pagina.canvas
            y = MARGEN
        }

        fun derecha(t: String, x: Float, p: Paint) = canvas.drawText(t, x - p.measureText(t), y, p)

        /** Corta un texto para que quepa en el ancho dado. */
        fun recortar(t: String, ancho: Float, p: Paint): String {
            if (p.measureText(t) <= ancho) return t
            val n = p.breakText(t, true, ancho - p.measureText("…"), null)
            return t.take(n) + "…"
        }

        val pedido = factura.pedido
        val d = pedido.desglose
        val cliente = pedido.cliente

        // Encabezado: logo, nombre del negocio y número de factura
        BitmapFactory.decodeResource(context.resources, R.drawable.logo_uyparce)?.let { logo ->
            val alto = 56f
            val ancho = logo.width * alto / logo.height
            canvas.drawBitmap(logo, null, RectF(MARGEN, y, MARGEN + ancho, y + alto), null)
        }
        y += 22
        derecha("Factura ${factura.numero}", ANCHO - MARGEN, titulo)
        y += 18
        derecha("Pedido #${pedido.numero} · ${fechaHora(factura.fecha)}", ANCHO - MARGEN, gris)
        y += 40
        canvas.drawText("UyParce Shopper", MARGEN, y, negrita)
        y += 14
        canvas.drawText("Personal shopping de outlets de EE. UU. · Cúcuta, Colombia", MARGEN, y, gris)
        y += 22
        canvas.drawLine(MARGEN, y, ANCHO - MARGEN, y, linea)
        y += 22

        // Cliente
        canvas.drawText("Cliente", MARGEN, y, negrita)
        y += 16
        listOf(
            cliente.nombre,
            "Cédula ${cliente.cedula}",
            "${cliente.direccion}, ${cliente.ciudad}",
            "Teléfono ${cliente.telefono}",
            "${cliente.tipoCliente?.etiqueta ?: "Detal"} · pagos en ${d.moneda}"
        ).forEach {
            canvas.drawText(recortar(it, ANCHO - 2 * MARGEN, texto), MARGEN, y, texto)
            y += 15
        }
        y += 12

        // Tabla de prendas
        val colTalla = 330f
        val colCant = 390f
        val colPrecio = 470f
        val colTotal = ANCHO - MARGEN
        canvas.drawText("Producto", MARGEN, y, negrita)
        canvas.drawText("Talla", colTalla, y, negrita)
        derecha("Cant.", colCant + 30, negrita)
        derecha("Precio", colPrecio + 30, negrita)
        derecha("Subtotal", colTotal, negrita)
        y += 8
        canvas.drawLine(MARGEN, y, ANCHO - MARGEN, y, linea)
        y += 16
        pedido.lineas.forEach { l ->
            nuevaPaginaSiHaceFalta(18f)
            canvas.drawText(recortar(l.producto.nombre, colTalla - MARGEN - 10, texto), MARGEN, y, texto)
            canvas.drawText(recortar(l.talla, 50f, texto), colTalla, y, texto)
            derecha("${l.cantidad}", colCant + 30, texto)
            derecha(formatoDinero(l.precioUnitario, d.moneda), colPrecio + 30, texto)
            derecha(formatoDinero(l.precioUnitario * l.cantidad, d.moneda), colTotal, texto)
            y += 18
        }
        canvas.drawLine(MARGEN, y - 6, ANCHO - MARGEN, y - 6, linea)
        y += 12

        // Totales
        fun total(etiqueta: String, valor: Double, fuerte: Boolean = false) {
            nuevaPaginaSiHaceFalta(18f)
            val p = if (fuerte) negrita else texto
            derecha(etiqueta, colPrecio, p)
            derecha("${formatoDinero(valor, d.moneda)} ${d.moneda}", colTotal, p)
            y += 18
        }
        total("Productos", d.productos)
        if (d.comision > 0) total("Comisión (${porcentaje(d.porcentajeComision)})", d.comision)
        if (d.envioInternacional > 0) total("Envío internacional", d.envioInternacional)
        total("Envío nacional", d.envioNacional)
        if (pedido.saldoAplicado > 0) {
            total("Total del pedido", d.total)
            total("Saldo a favor", -pedido.saldoAplicado)
        }
        total("Total pagado", pedido.totalAPagar, fuerte = true)

        y += 24
        nuevaPaginaSiHaceFalta(30f)
        canvas.drawText("Gracias por comprar con UyParce Shopper.", MARGEN, y, gris)
        pdf.finishPage(pagina)

        val carpeta = File(context.cacheDir, "facturas").apply { mkdirs() }
        val archivo = File(carpeta, "${factura.numero}.pdf")
        archivo.outputStream().use { pdf.writeTo(it) }
        pdf.close()
        archivo
    }

    /** Abre el menú de Android para compartir el PDF (WhatsApp, correo, Drive…). */
    fun compartir(context: Context, archivo: File, numero: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
        val intent = Intent(Intent.ACTION_SEND)
            .setType("application/pdf")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, "Factura $numero · UyParce Shopper")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(intent, "Compartir factura"))
    }

    /** Copia el PDF al lugar que eligió la persona (por ejemplo, Descargas). */
    suspend fun guardarEn(context: Context, archivo: File, destino: Uri) = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(destino)?.use { salida -> archivo.inputStream().use { it.copyTo(salida) } }
            ?: throw IllegalStateException("No se pudo abrir el archivo de destino")
    }
}
