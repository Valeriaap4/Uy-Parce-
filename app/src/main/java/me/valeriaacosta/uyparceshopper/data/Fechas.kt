package me.valeriaacosta.uyparceshopper.data

import com.google.firebase.Timestamp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Date

/*
 * Fechas de la app, siempre en hora de Colombia (sin importar la zona del celular).
 * Se escriben a mano los meses para que se vean igual en todos los celulares: "8 nov 2026".
 */

val zonaColombia: ZoneId = ZoneId.of("America/Bogota")
private val meses = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
private val formatoCampo = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)

fun Timestamp.enColombia(): ZonedDateTime = toDate().toInstant().atZone(zonaColombia)

fun ZonedDateTime.aTimestamp(): Timestamp = Timestamp(Date.from(toInstant()))

/** "8 nov 2026" */
fun fecha(ts: Timestamp?): String {
    val d = ts?.enColombia() ?: return "—"
    return "${d.dayOfMonth} ${meses[d.monthValue - 1]} ${d.year}"
}

/** "8 nov 2026, 3:12 p. m." */
fun fechaHora(ts: Timestamp?): String {
    val d = ts?.enColombia() ?: return "—"
    return "${fecha(ts)}, ${hora(d)}"
}

/** "8 nov, 3:12 p. m." (sin el año, para listas). */
fun fechaHoraCorta(ts: Timestamp?): String {
    val d = ts?.enColombia() ?: return "—"
    return "${d.dayOfMonth} ${meses[d.monthValue - 1]}, ${hora(d)}"
}

private fun hora(d: ZonedDateTime): String {
    val h12 = if (d.hour % 12 == 0) 12 else d.hour % 12
    val minutos = d.minute.toString().padStart(2, '0')
    return "$h12:$minutos ${if (d.hour < 12) "a. m." else "p. m."}"
}

/** "ahora", "hace 5 min", "hace 2 h", "ayer", "hace 3 días" o la fecha. */
fun hace(ts: Timestamp?, ahora: Timestamp = Timestamp.now()): String {
    if (ts == null) return "ahora"
    val minutos = ChronoUnit.MINUTES.between(ts.toDate().toInstant(), ahora.toDate().toInstant())
    val dias = ChronoUnit.DAYS.between(ts.enColombia().toLocalDate(), ahora.enColombia().toLocalDate())
    return when {
        minutos < 1 -> "ahora"
        minutos < 60 -> "hace $minutos min"
        dias == 0L -> "hace ${minutos / 60} h"
        dias == 1L -> "ayer"
        dias < 7 -> "hace $dias días"
        else -> fecha(ts)
    }
}

/** Fecha para un campo de formulario: "09/11/2026". */
fun fechaCampo(ts: Timestamp = Timestamp.now()): String = ts.enColombia().format(formatoCampo)

/**
 * Lee una fecha escrita como dd/mm/aaaa. Devuelve null si no existe (ej. 31/02/2026).
 * Queda a mediodía en Colombia para que no cambie de día por la zona horaria.
 */
fun leerFechaCampo(texto: String): Timestamp? = try {
    LocalDate.parse(texto.trim(), formatoCampo).atTime(LocalTime.NOON).atZone(zonaColombia).aTimestamp()
} catch (e: DateTimeParseException) {
    null
}

/** Ahora + n horas (plazo para pagar). */
fun Timestamp.masHoras(horas: Int): Timestamp = Timestamp(Date(toDate().time + horas * 3_600_000L))

/** "23 h 41 min" que quedan hasta [hasta]; null si ya pasó. */
fun tiempoRestante(hasta: Timestamp?, ahora: Timestamp = Timestamp.now()): String? {
    if (hasta == null) return null
    val minutos = ChronoUnit.MINUTES.between(ahora.toDate().toInstant(), hasta.toDate().toInstant())
    if (minutos <= 0) return null
    return if (minutos >= 60) "${minutos / 60} h ${minutos % 60} min" else "$minutos min"
}

/** Períodos de los reportes. */
enum class Periodo(val etiqueta: String) { HOY("Hoy"), SEMANA("Semana"), MES("Mes"), ANIO("Año") }

/** Inicio del período en hora de Colombia (la semana empieza el lunes). */
fun inicioDe(periodo: Periodo, ahora: ZonedDateTime = ZonedDateTime.now(zonaColombia)): Timestamp {
    val hoy = ahora.toLocalDate()
    val inicio = when (periodo) {
        Periodo.HOY -> hoy
        Periodo.SEMANA -> hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        Periodo.MES -> hoy.withDayOfMonth(1)
        Periodo.ANIO -> hoy.withDayOfYear(1)
    }
    return inicio.atStartOfDay(zonaColombia).aTimestamp()
}

/** true si la fecha es de hoy (hora de Colombia). */
fun esHoy(ts: Timestamp?): Boolean =
    ts != null && ts.enColombia().toLocalDate() == ZonedDateTime.now(zonaColombia).toLocalDate()

/** Para datos de ejemplo: una fecha fija en Colombia. */
fun fechaEjemplo(anio: Int, mes: Int, dia: Int, hora: Int = 12, minuto: Int = 0): Timestamp =
    ZonedDateTime.of(anio, mes, dia, hora, minuto, 0, 0, zonaColombia).aTimestamp()
