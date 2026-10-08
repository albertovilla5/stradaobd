#!/usr/bin/env python3
"""Diagnóstico DTC integral con ELM TCP de prueba. Solo emuladores; restaura preferencias y pantalla."""
import argparse
import json
import pathlib
import socketserver
import subprocess
import threading
import time
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--serial', default='emulator-5554')
parser.add_argument('--adb', default=str(pathlib.Path.home() / 'Library/Android/sdk/platform-tools/adb'))
parser.add_argument('--apk', type=pathlib.Path, default=ROOT / 'app/build/outputs/apk/debug/app-debug.apk')
parser.add_argument('--release-apk', type=pathlib.Path, help='Verifica release y lo deja instalado; usa debug solo para respaldar/restaurar preferencias.')
args = parser.parse_args()
if not args.serial.startswith('emulator-'):
    raise SystemExit('Usa un emulador dedicado a desarrollo.')
ADB = [args.adb, '-s', args.serial]
PACKAGE = 'com.torqueobd2.dash'
OUT = ROOT / 'build/dtc-review'
OUT.mkdir(parents=True, exist_ok=True)
commands = []
fixture = {'cleared': False, 'rpm': 0, 'partial': False, 'invalid': False}


def adb(*command, data=None):
    return subprocess.check_output(ADB + list(command), input=data, stderr=subprocess.STDOUT, timeout=40)


def stop():
    adb('shell', 'am', 'force-stop', PACKAGE)


def start():
    adb('shell', 'am', 'start', '-W', '-f', '0x10008000', '-n', PACKAGE + '/.MainActivity')
    time.sleep(1)


def write_prefs(name, values):
    root = ET.Element('map')
    for key, value in values.items():
        if isinstance(value, bool):
            ET.SubElement(root, 'boolean', name=key, value=str(value).lower())
        elif isinstance(value, int):
            ET.SubElement(root, 'int', name=key, value=str(value))
        else:
            ET.SubElement(root, 'string', name=key).text = value
    adb('shell', 'run-as', PACKAGE, 'sh', '-c', "'cat > shared_prefs/" + name + ".xml'", data=ET.tostring(root))


def nodes():
    adb('shell', 'uiautomator', 'dump', '/sdcard/torque-dtc.xml')
    result = list(ET.fromstring(adb('shell', 'cat', '/sdcard/torque-dtc.xml')).iter('node'))
    deny = next((n for n in result if n.get('package') == 'com.android.settings' and n.get('text') == 'Deny'), None)
    if deny is not None:
        tap_node(deny)
        return nodes()
    return result


def rect(node):
    return tuple(map(int, node.get('bounds').replace('][', ',').strip('[]').split(',')))


def tap_node(node):
    left, top, right, bottom = rect(node)
    adb('shell', 'input', 'tap', str((left + right) // 2), str((top + bottom) // 2))
    time.sleep(.3)


def tap(label, contains=False):
    for node in nodes():
        text = node.get('text', '')
        if (label in text if contains else text == label) or node.get('content-desc') == label:
            left, top, right, bottom = rect(node)
            if right > left and bottom > top:
                tap_node(node)
                return
    raise AssertionError('Control no visible: ' + label)


def tap_scroll(label, contains=False):
    for _ in range(20):
        try:
            tap(label, contains); return
        except AssertionError:
            swipe_content()
    raise AssertionError('No se encuentra el control: ' + label)


def save_document():
    tap_scroll('Guardar TXT')
    state = nodes()
    (OUT / 'save-picker.png').write_bytes(adb('exec-out', 'screencap', '-p'))
    filename = next((n.get('text') for n in state if n.get('class') == 'android.widget.EditText' and n.get('text', '').endswith('.txt')), None)
    assert filename, [n.attrib for n in state]
    button = next((n for n in state if n.get('text') in ('SAVE', 'Save', 'GUARDAR', 'Guardar') and n.get('enabled') == 'true'), None)
    assert button is not None, [n.get('text') for n in state]
    tap_node(button)
    time.sleep(.5)
    text = adb('shell', 'cat', '/sdcard/Download/' + filename).decode()
    assert 'P0300' in text and 'P0420' in text and 'ECU 7E9' in text
    assert 'Respuestas originales' in text and 'SIMULACIÓN' not in text
    (OUT / 'exported-report.txt').write_text(text)
    adb('shell', 'rm', '/sdcard/Download/' + filename)
    top()


def swipe_content(up=True):
    adb('shell', 'input', 'swipe', '760', '490' if up else '170', '760', '300' if up else '490', '500' if up else '200')
    time.sleep(.2)


def top():
    for _ in range(7):
        swipe_content(False)


def navigate_dtc():
    for _ in range(6):
        try:
            tap('Averías'); return
        except AssertionError:
            adb('shell', 'input', 'swipe', '42', '460', '42', '150', '400')
    raise AssertionError('No se encuentra la navegación Averías')


def capture(name):
    state = nodes()
    texts = [node.get('text') for node in state if node.get('text')]
    (OUT / (name + '.png')).write_bytes(adb('exec-out', 'screencap', '-p'))
    (OUT / (name + '.json')).write_text(json.dumps(texts, ensure_ascii=False, indent=2))
    assert adb('shell', 'pidof', PACKAGE).strip()
    print(name + ': OK', flush=True)
    return texts


def pages(name, count=10):
    seen = []
    for page in range(count):
        seen.extend(capture(name + '-' + str(page)))
        swipe_content()
    return '\n'.join(seen)


def read(count=10):
    top()
    try:
        tap('Volver a leer')
    except AssertionError:
        tap('Leer averías')
    time.sleep(1)
    return pages('read-' + str(len(commands)), count)


def clear_dialog():
    top(); tap('Borrar averías')
    assert 'Confirmar borrado de averías' in capture('clear-confirmation')


def confirm_clear():
    checks = [n for n in nodes() if n.get('checkable') == 'true']
    assert len(checks) == 1
    tap_node(checks[0]); tap('Confirmar borrado'); time.sleep(.7)


class Elm(socketserver.StreamRequestHandler):
    def handle(self):
        headers = False
        while True:
            raw = bytearray()
            while not raw.endswith(b'\r'):
                char = self.rfile.read(1)
                if not char: return
                raw.extend(char)
            command = raw.decode().strip().upper().replace(' ', '')
            commands.append(command)
            def frame(payload, ecu='7E8'):
                return f'{ecu} {len(payload.split()):02X} {payload}' if headers else payload
            if command in ('ATD', 'ATH0'): headers = False
            if command == 'ATH1': headers = True
            if command.startswith('AT'):
                reply = 'A6' if command == 'ATDPN' else ('ELM327 v1.5' if command in ('ATZ', 'ATI') else 'OK')
            elif command == '0100': reply = frame('41 00 08 18 00 00')
            elif command == '0101': reply = frame('41 01 00 77 21 21' if fixture['cleared'] else '41 01 82 07 21 20') + '\r' + frame('41 01 00 07 00 00' if fixture['cleared'] else '41 01 01 07 00 00', '7E9')
            elif command == '03':
                reply = frame('43 00' if fixture['cleared'] else '43 02 03 00 01 33') + '\r' + frame('43 00' if fixture['cleared'] else '43 01 C1 00', '7E9')
                if fixture['invalid']: reply = '7E8 06 43 02 03 00'
            elif command == '07': reply = 'NO DATA' if fixture['partial'] else frame('47 00' if fixture['cleared'] else '47 01 01 71')
            elif command == '0A': reply = frame('7F 0A 11' if fixture['partial'] else '4A 01 04 20')
            elif command == '04':
                fixture['cleared'] = True; reply = frame('44') + '\r' + frame('44', '7E9')
            elif command == '020200': reply = frame('42 02 00 00 00' if fixture['cleared'] else '42 02 00 03 00')
            elif command == '020000': reply = frame('42 00 00 08 18 00 00')
            elif command == '020500': reply = frame('42 05 00 7E')
            elif command == '020C00': reply = frame('42 0C 00 0C 80')
            elif command == '020D00': reply = frame('42 0D 00 00')
            elif command == '010C':
                rpm = fixture['rpm'] * 4; reply = frame(f'41 0C {rpm // 256:02X} {rpm % 256:02X}')
            elif command == '0105': reply = frame('41 05 7E')
            elif command == '010D': reply = frame('41 0D 00')
            else: reply = 'NO DATA'
            try:
                time.sleep(.03)
                self.wfile.write((reply + '\r>').encode()); self.wfile.flush()
            except OSError: return


class Server(socketserver.ThreadingTCPServer):
    allow_reuse_address = True
    daemon_threads = True


adb('install', '-r', str(args.apk)); stop()
backup = adb('exec-out', 'run-as', PACKAGE, 'tar', '-cf', '-', 'shared_prefs')
(OUT / 'original-preferences.tar').write_bytes(backup)
server = Server(('0.0.0.0', 0), Elm)
threading.Thread(target=server.serve_forever, daemon=True).start()
try:
    adb('shell', 'run-as', PACKAGE, 'rm', '-rf', 'shared_prefs')
    adb('shell', 'run-as', PACKAGE, 'mkdir', 'shared_prefs')
    write_prefs('interface', {'car_mode': False})
    write_prefs('torque_obd_prefs', {'saved_vehicles_json': json.dumps([{'id': 'dtc-vehicle', 'name': 'Vehículo DTC de prueba', 'modelYear': '2020', 'engineDisplacementLiters': 1.5, 'fuelTankCapacityLiters': 50, 'vehicleWeightKg': 1300, 'maxPowerHp': 150, 'maxTorqueNm': 250, 'maxRpm': 6500, 'fuelType': 'GASOLINE', 'volumetricEfficiencyPercent': 85, 'boostPressureAdjustmentBar': 0, 'customInitCommands': '', 'hideUnsupportedSensors': True}]), 'active_vehicle_id': 'dtc-vehicle'})
    port = server.server_address[1]
    write_prefs('obd_connection', {'last_type': 'WIFI', 'last_name': 'ELM DTC de prueba', 'last_address': f'10.0.2.2:{port}',
        'wifi_host': '10.0.2.2', 'wifi_port': port, 'simulation_enabled': False, 'polling_mode': 'VISIBLE'})
    if args.release_apk:
        adb('install', '-r', str(args.release_apk))
    start(); navigate_dtc()
    text = read()
    for value in ['P0300', 'P0133', 'P0171', 'P0420', 'U0100', 'ECU 7E8', 'ECU 7E9']:
        assert value in text, (value, text)
    assert 'Lectura completa' in text
    assert '04' not in commands
    # Detalle y datos congelados, disponibles mediante desplazamiento.
    top(); tap_scroll('P0300', contains=True); capture('code-detail'); tap('Cerrar detalle')
    tap_scroll('Ver datos congelados')
    freeze = pages('freeze', 4)
    assert '800,0' in freeze or '800.0' in freeze
    assert '86,0' in freeze or '86.0' in freeze
    top(); tap_scroll('Copiar informe'); assert 'Informe copiado' in '\n'.join(capture('report-copied'))
    top(); save_document(); capture('report-saved')
    # Cancelar el diálogo nunca envía 04.
    clear_dialog(); tap('Cancelar borrado'); assert '04' not in commands
    # Negativa y silencio se muestran sin afirmar ausencia de averías.
    fixture['partial'] = True
    text = read(3); assert 'Modo no admitido' in text and 'Sin respuesta / sin datos' in text
    fixture['partial'] = False; fixture['invalid'] = True
    text = read(3); assert 'Respuesta inválida' in text
    fixture['invalid'] = False; read(2)
    # Aunque el usuario confirme, RPM positivas bloquean el borrado.
    fixture['rpm'] = 800
    clear_dialog(); confirm_clear(); assert '04' not in commands
    assert 'Motor en marcha' in '\n'.join(capture('engine-running-blocked'))
    fixture['rpm'] = 0
    clear_dialog(); confirm_clear()
    text = pages('after-clear')
    assert commands.count('04') == 1
    assert 'Borrado confirmado' in text and 'P0420' in text
    assert 'P0300' not in text and 'P0171' not in text and 'U0100' not in text
    top(); tap_scroll('Copiar informe'); capture('cleared-report')
    # Volver al cuadro comprueba la restauración del formato del ELM.
    tap('Deportivo'); time.sleep(.7); dashboard = capture('dashboard-after-dtc')
    assert 'Sin respuesta' not in dashboard and 'Respuesta inválida' not in dashboard
    (OUT / 'commands.json').write_text(json.dumps(commands, indent=2))
    (OUT / 'result.json').write_text(json.dumps({'passed': True, 'checks': ['all-three-modes', 'multi-ecu', 'descriptions', 'freeze-frame',
        'copy-report', 'export-txt', 'cancel-clear-no-command', 'unsupported-and-no-data', 'invalid-response', 'engine-running-blocked',
        'confirmed-clear-reread', 'permanent-retained', 'polling-format-restored']}, indent=2))
finally:
    stop()
    if args.release_apk:
        adb('install', '-r', str(args.apk))
    adb('shell', 'run-as', PACKAGE, 'rm', '-rf', 'shared_prefs')
    adb('shell', 'run-as', PACKAGE, 'tar', '-xf', '-', data=backup)
    server.shutdown(); server.server_close()
    if args.release_apk:
        adb('install', '-r', str(args.release_apk))
    start()
    print('Preferencias originales restauradas.', flush=True)
