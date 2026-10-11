# Política de Privacidad y Datos para Google Play Console — StradaOBD (`com.stradaobd.dash`)

- **Aplicación / Application:** StradaOBD — Telemetría ECU, Cockpit y Diagnosis (`com.stradaobd.dash`)
- **Desarrollador / Developer:** Alberto Villa (`albertovilla5`)
- **Correo electrónico de contacto / Contact Email:** `22d7rrcwzm@privaterelay.appleid.com`
- **Soporte e incidencias / Support & Issues (GitHub):** https://github.com/albertovilla5/stradaobd/issues
- **URL Pública HTTPS de Política de Privacidad (GitHub Pages):** https://albertovilla5.github.io/stradaobd/privacy-policy.html
- **Sitio Web del Desarrollador / Portal Principal (GitHub Pages):** https://albertovilla5.github.io/stradaobd/
- **Última actualización / Last Updated:** 11 de octubre de 2026 / October 11, 2026

---

## 🇪🇸 Política de Privacidad (Español)

### 1. Procesamiento 100% Local y Sin Servidores de Terceros
**StradaOBD** (`com.stradaobd.dash`) no recopila, no vende ni transmite datos personales ni telemetría del vehículo a servidores propios ni de terceros. Toda la lectura OBD-II con adaptadores ELM327 (Bluetooth SPP, Wi-Fi TCP o Modo Simulación) se procesa directamente en el dispositivo Android del usuario.

### 2. Uso de Permisos de Android
- **Bluetooth y Dispositivos Cercanos (`BLUETOOTH`, `BLUETOOTH_ADMIN`, `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN` con `neverForLocation`):** Se utilizan exclusivamente para conectar con adaptadores ELM327 Bluetooth emparejados y descubrir adaptadores cercanos a petición del usuario. En Android 12+ se declara `neverForLocation` para garantizar que el escaneo Bluetooth nunca derive la ubicación física.
- **Ubicación (`ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION` con `maxSdkVersion="30"`):** Solicitado únicamente en Android 6 a 11 como requisito técnico del sistema operativo para descubrir adaptadores Bluetooth cercanos. **La aplicación nunca rastrea, registra, almacena ni comparte coordenadas GPS ni ubicación geográfica.**
- **Red Local (`INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`):** Utilizado exclusivamente para abrir conexiones locales por socket TCP hacia adaptadores ELM327 Wi-Fi (p. ej. `192.168.0.10:35000`).
- **Servicio en Primer Plano y Notificaciones (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_CONNECTED_DEVICE`, `POST_NOTIFICATIONS`, `WAKE_LOCK`):** Permite mantener activa la lectura de telemetría ECU, la evaluación de avisos configurados por el usuario y la grabación de sesiones CSV cuando la pantalla se apaga o la app pasa a segundo plano mientras el vehículo está conectado, mostrando una notificación persistente con botones para detener la grabación o desconectar.

### 3. Almacenamiento, Copias de Seguridad (`Auto Backup`) y Exportación Manual
- **Almacenamiento privado:** Los perfiles de vehículos, pantallas personalizadas, sensores PID, alertas y sesiones CSV se almacenan en la memoria privada interna del dispositivo.
- **Copia de seguridad automática de Android (Google Drive del usuario):** Respalda de forma cifrada únicamente la configuración del usuario (`torque_obd_prefs.xml`, `torque_sensors.xml`, `sensor_alerts.xml` e `interface.xml`: vehículos, pantallas a medida, sensores, avisos e idioma). Por privacidad y compatibilidad entre dispositivos, `backup_rules.xml` y `data_extraction_rules.xml` **excluyen expresamente** las direcciones MAC Bluetooth, las direcciones IP Wi-Fi, el modo simulación (`obd_connection.xml`) y las grabaciones CSV.
- **Exportación e importación manual:** Realizada únicamente por acción explícita del usuario mediante el selector de archivos del sistema Android (*Storage Access Framework*) en formatos `.json`, `.csv` o `.txt`.

### 4. Sin Anuncios ni Rastreadores
Esta aplicación no contiene SDKs publicitarios ni de análisis de comportamiento y no utiliza el identificador de publicidad de Android (`AD_ID`).

### 5. Seguridad Vial y Responsabilidad Mecánica
- **Seguridad al volante:** El conductor es el único responsable de mantener la atención en la carretera. No manipules la aplicación, no edites pantallas ni utilices la terminal o el borrado de averías con el vehículo en movimiento.
- **Operaciones ECU:** El borrado de códigos de avería (Modo `04`, con contacto encendido y motor parado) y el envío de comandos `AT`/OBD personalizados se realizan bajo la exclusiva responsabilidad del usuario.

---

## 🇬🇧 Privacy Policy (English)

### 1. 100% Local Processing & No Third-Party Servers
**StradaOBD** (`com.stradaobd.dash`) does not collect, sell, or transmit personal data or vehicle telemetry to any developer-owned or third-party servers. All OBD-II communication with ELM327 adapters (Bluetooth SPP, Wi-Fi TCP, or Simulation Mode) is processed locally on the user's Android device.

### 2. Android Permissions Usage
- **Bluetooth & Nearby Devices (`BLUETOOTH`, `BLUETOOTH_ADMIN`, `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN` with `neverForLocation`):** Used solely to connect to paired Bluetooth ELM327 adapters and scan for nearby adapters when requested by the user. On Android 12+, `neverForLocation` ensures Bluetooth scanning is never used to derive physical location.
- **Location (`ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION` with `maxSdkVersion="30"`):** Requested only on Android 6–11 as an OS requirement to discover nearby Bluetooth adapters. **The app never tracks, logs, stores, or shares GPS coordinates or geographic location.**
- **Local Network (`INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`):** Used solely for local TCP socket connections to Wi-Fi ELM327 adapters (e.g., `192.168.0.10:35000`).
- **Foreground Service & Notifications (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_CONNECTED_DEVICE`, `POST_NOTIFICATIONS`, `WAKE_LOCK`):** Keeps ECU sensor polling, user-configured sensor alerts, and CSV telemetry recording active when the screen is off or the app is in the background, showing a persistent notification with controls to stop recording or disconnect at any time.

### 3. Storage, Backups (`Auto Backup`) & Manual Export
- **Private storage:** Vehicle profiles, custom dashboards, PID definitions, alert rules, and CSV recordings are stored in the device's private internal storage.
- **Android Auto Backup (User's Google Drive):** Encrypts and backs up only user configuration preferences (`torque_obd_prefs.xml`, `torque_sensors.xml`, `sensor_alerts.xml`, and `interface.xml`: vehicles, custom dashboards, sensors, alerts, and language). For privacy and cross-device compatibility, `backup_rules.xml` and `data_extraction_rules.xml` **strictly exclude** Bluetooth MAC addresses, Wi-Fi IP addresses, simulation mode state (`obd_connection.xml`), and CSV telemetry recordings.
- **Manual export/import:** Triggered only by explicit user action via the Android system file picker (*Storage Access Framework*) as `.json`, `.csv`, or `.txt` files.

### 4. No Ads or Trackers
This app contains no advertising or behavioral analytics SDKs and does not use the Android Advertising ID (`AD_ID`).

### 5. Road Safety & Mechanical Disclaimer
- **Driver safety:** The driver is solely responsible for maintaining full attention on the road. Do not manipulate the app, edit dashboards, or use the terminal or fault code clearing while the vehicle is moving.
- **ECU operations:** Clearing diagnostic trouble codes (Mode `04`, requiring ignition ON and engine OFF) and sending custom `AT`/OBD commands are performed at the user's sole responsibility.

---

## 📋 Datos Listos para Copiar en Google Play Console

| Campo en Google Play Console | Valor a Introducir |
| :--- | :--- |
| **Nombre de la aplicación** | `StradaOBD` |
| **ID de paquete (`applicationId`)** | `com.stradaobd.dash` |
| **Nombre del desarrollador / Contacto** | `Alberto Villa` (`albertovilla5`) |
| **Correo electrónico de contacto** | `22d7rrcwzm@privaterelay.appleid.com` |
| **Sitio web de soporte / Issues** | `https://github.com/albertovilla5/stradaobd/issues` |
| **Sitio web principal (HTTPS)** | `https://albertovilla5.github.io/stradaobd/` |
| **URL de la Política de Privacidad (HTTPS)** | `https://albertovilla5.github.io/stradaobd/privacy-policy.html` |
| **¿La app recoge o comparte datos del usuario? (*Data Safety*)** | **No** (procesamiento 100% local en el dispositivo; copia de seguridad cifrada gestionada por el propio sistema Android Auto Backup en el Google Drive del usuario). |
| **¿Utiliza ID de publicidad (`AD_ID`)?** | **No** |
