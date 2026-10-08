#!/usr/bin/env bash
# Instala el APK sin borrar datos y conserva evidencias de una prueba breve.
set -eu

project_dir="$(cd "$(dirname "$0")/.." && pwd)"
apk_path="${1:-$project_dir/app/build/outputs/apk/release/app-release.apk}"
if [ ! -f "$apk_path" ]; then
    echo "No se encuentra el APK: $apk_path" >&2
    exit 1
fi

adb_bin="$(command -v adb || true)"
if [ -z "$adb_bin" ] && [ -f "$project_dir/local.properties" ]; then
    sdk_dir="$(sed -n 's/^sdk.dir=//p' "$project_dir/local.properties" | head -n 1)"
    adb_bin="$sdk_dir/platform-tools/adb"
fi
if [ -z "$adb_bin" ] || [ ! -x "$adb_bin" ]; then
    echo "No se encuentra adb. Añade Android SDK/platform-tools al PATH." >&2
    exit 1
fi

device_serial="${2:-}"
if [ -z "$device_serial" ]; then
    device_serial="$("$adb_bin" get-serialno)"
fi
"$adb_bin" -s "$device_serial" get-state > /dev/null
mkdir -p "$project_dir/build/radio-check"
report_dir="$(mktemp -d "$project_dir/build/radio-check/run-XXXXXX")"
echo "Informe: $report_dir"
for property in ro.build.version.release ro.build.version.sdk ro.product.cpu.abilist ro.product.manufacturer ro.product.model; do
    value="$("$adb_bin" -s "$device_serial" shell getprop "$property")"
    printf '%s=%s\n' "$property" "$value" >> "$report_dir/device.txt"
done
if ! "$adb_bin" -s "$device_serial" install -r "$apk_path" > "$report_dir/install.txt" 2>&1; then
    cat "$report_dir/install.txt"
    echo "Instalación rechazada. Conserva el informe; no se han borrado datos."
    exit 1
fi
cat "$report_dir/install.txt"
launch_status=0
"$adb_bin" -s "$device_serial" shell am start -W -n com.torqueobd2.dash/.MainActivity > "$report_dir/launch.txt" 2>&1 || launch_status=$?
sleep 3
"$adb_bin" -s "$device_serial" shell pidof com.torqueobd2.dash > "$report_dir/process.txt" || true
"$adb_bin" -s "$device_serial" logcat -d -t 300 -v threadtime AndroidRuntime:E '*:S' > "$report_dir/crashes.txt" || true
if [ "$launch_status" -ne 0 ] || ! grep -q '^Status: ok' "$report_dir/launch.txt" || [ ! -s "$report_dir/process.txt" ]; then
    echo "No se ha confirmado el arranque. Revisa launch.txt y crashes.txt en el informe."
    exit 1
fi
echo "Instalación y proceso de la app confirmados. Prueba ahora Simulación y Conexión en la radio."
