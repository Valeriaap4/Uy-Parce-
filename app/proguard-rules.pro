# Firestore convierte los documentos en objetos de data/models por reflexión:
# si algún día se activa la minificación, estas clases deben conservar sus nombres y campos.
-keep class me.valeriaacosta.uyparceshopper.data.models.** { *; }
-keepclassmembers enum me.valeriaacosta.uyparceshopper.data.models.** { *; }
