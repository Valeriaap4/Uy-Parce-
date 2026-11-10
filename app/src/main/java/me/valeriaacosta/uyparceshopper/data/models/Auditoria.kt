package me.valeriaacosta.uyparceshopper.data.models

import com.google.firebase.Timestamp

/** Registro de quién hizo qué. Solo se agregan registros; nadie los edita. */
data class RegistroAuditoria(
    val id: String = "",
    val usuarioId: String = "",
    val usuario: String = "",
    val accion: String = "",
    val detalle: String = "",
    val fecha: Timestamp? = null
)

/** Algo que se eliminó y el admin puede restaurar. Guarda una copia del documento original. */
data class ElementoPapelera(
    val id: String = "",
    /** "Producto", "Caja"… */
    val tipo: String = "",
    val nombre: String = "",
    /** Colección y documento de donde salió (para restaurarlo en el mismo lugar). */
    val coleccion: String = "",
    val docId: String = "",
    val datos: Map<String, Any?> = emptyMap(),
    val eliminadoPor: String = "",
    val fecha: Timestamp? = null
)
