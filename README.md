# Torque OBD2 Dashboard para Android

Aplicación Android moderna (desarrollada con **Jetpack Compose**, **Kotlin** y **Material 3**) para conectar adaptadores **ELM327** (Bluetooth SPP, WiFi TCP o Modo Simulación) y visualizar telemetría automotriz en tiempo real. 

Soporta importación de archivos CSV con PIDs específicos y fórmulas de cálculo idénticas a **Torque Pro**, perfil completo de vehículo con estimación física de potencia/consumo, detección de sensores soportados por la centralita del coche y un **diseñador de cuadros de mando personalizados**.

---

## 🚀 Nuevas Funcionalidades Agregadas

### 1. 🚗 Perfil del Vehículo (Igual que en Torque Pro)
- Permite configurar los parámetros del coche necesarios para los modelos matemáticos y cálculos de diagnóstico:
  - **Cilindrada del motor (Litros)**: ej. 2.2L, 2.0L, 1.6L (para cálculos de consumo Speed-Density, masa de aire MAF y potencia).
  - **Capacidad del depósito de combustible (Litros)**: ej. 50L (para cálculo exacto de litros restantes y autonomía en km).
  - **Peso del vehículo en vacío (kg)**: ej. 1450 kg (para estimación de aceleración, potencia en CV / HP y par en Nm).
  - **Tipo de combustible**: Diésel, Gasolina (E5/E10), GLP / Autogas, Híbrido (con sus densidades y ratios estequiométricos AFR específicos).
  - **Régimen máximo (Redline RPM)**: ej. 5000 RPM para diésel, 6500+ para gasolina.
  - **Eficiencia volumétrica del motor (%)**: ej. 85%.
  - **Plantillas rápidas preconfiguradas**: Honda 2.2 i-DTEC, VAG 2.0 TDI, Gasolina 1.6 Turbo.

### 2. 🔍 Detección y Filtrado de Sensores Soportados por la ECU
- En función del coche conectado, hay parámetros que están o no disponibles en la centralita.
- **Escaneo activo de compatibilidad**:
  - Detección de mapas de bits de PIDs soportados estándar (SAE J1979 PIDs `0100`, `0120`, `0140`, `0160`).
  - Detección de respuestas para PIDs extendidos Modo 22 (distinguiendo entre respuestas positivas `62...` y respuestas no soportadas `NO DATA`, `ERROR`, `7F 22`).
- **Filtro dinámico de visualización**:
  - Interruptor: *"Mostrar únicamente los sensores soportados por el coche"*.
  - Oculta automáticamente indicadores y datos en blanco en los cuadros de mandos si la centralita del coche no dispone de ese sensor.

### 3. 🎨 Diseñador de Dashboards Personalizados (*A Medida*)
- Crea tu propio cuadro de mandos combinando los sensores que tú elijas:
  - **Tipos de Indicador**:
    1. *Reloj Analógico con Aguja*: Dial circular deportivo con arco degradado y display LCD integrado.
    2. *Display Digital Grande*: Números de gran visibilidad para circuito o conducción nocturna.
    3. *Barra de Nivel Horizontal*: Barra de progreso con lecturas numéricas y unidades.
    4. *Gráfico Histórico en Tiempo Real*: Trazado continuo Sparkline de telemetría.
    5. *Tarjeta Compacta*: Vista rápida minimalista.
  - **Tamaños ajustables**: Formato estándar (1x1) o ancho completo (2x1).
  - **Modo Edición interactivo**: Añade nuevos sensores mediante un diálogo modal, redimensiona o elimina widgets.
  - **Persistencia**: La distribución diseñada se guarda en el dispositivo para que esté lista cada vez que abras la app.

---

## 📊 Cuadros de Mandos Disponibles

1. 🏁 **Deportivo (Sport Classic)**: Tacómetro y velocímetro analógicos gigantes, manómetro de presión de Turbo (Boost en bar) y termómetro de refrigerante.
2. 🎨 **A Medida (Personalizado)**: El dashboard diseñado por ti con los sensores y formatos que prefieras.
3. ⚡ **Digital HUD (Track Telemetry)**: Velocímetro digital grande, marcha estimada (Gear N, 1ª a 6ª) y barra secuencial de luces LED de cambio de marcha estilo F1 / GT3 (*Shift Lights* verdes, amarillas y rojas).
4. 💨 **DPF / Diésel (Honda i-DTEC & Genéricos)**: Monitor especializado de filtro de partículas con presión diferencial (kPa), temperatura EGT 1 (°C), hollín PM calculado (g), nivel de aceite con detector de dilución, y aviso de regeneración activa.
5. 📋 **Perfil Coche**: Configuración del vehículo y escáner de sensores soportados.
6. 📊 **Monitores en Cuadrícula (Torque Grid)**: Tarjetas para todos los sensores activos con valor, unidad y barra de progreso.
7. 📁 **PIDs CSV**: Importador de archivos CSV idéntico al de Torque Pro y selector de sensores activos en el polling.
8. 🔌 **Conexión**: Bluetooth Clásico (SPP), WiFi (TCP) y Modo Simulación.
9. 💻 **Terminal AT**: Consola de comandos para depuración cruda con el ELM327.
