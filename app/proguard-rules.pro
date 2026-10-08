# Reglas ProGuard / R8 para Torque OBD2 Dash
# Optimización, ofuscación y reducción de tamaño para Google Play Store

# Mantener modelos de datos del vehículo y PIDs
-keep class com.torqueobd2.dash.data.model.** { *; }

# Mantener repositorios y parsers (evaluador de expresiones matemáticas y CSV)
-keep class com.torqueobd2.dash.data.parser.** { *; }
-keep class com.torqueobd2.dash.data.repository.** { *; }

# Mantener conexión OBD y comandos ELM327
-keep class com.torqueobd2.dash.obd.** { *; }

# Reglas generales para Jetpack Compose y Kotlin Coroutines
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Preservar números de línea para stacktraces legibles en Play Console
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
