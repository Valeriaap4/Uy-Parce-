package me.valeriaacosta.uyparceshopper.data.models

data class RegistroAuditoria(val usuario: String, val accion: String, val detalle: String, val fecha: String)

data class ElementoPapelera(val tipo: String, val nombre: String, val eliminadoPor: String, val fecha: String)
