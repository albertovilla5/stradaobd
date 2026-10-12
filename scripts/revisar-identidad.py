#!/usr/bin/env python3
"""Prueba integral en emulador: ELM WiFi local, IDs, migración y estados. Restaura preferencias."""
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
args = parser.parse_args()
if not args.serial.startswith('emulator-'):
    raise SystemExit('Esta prueba solo admite un emulador de desarrollo.')
ADB = [args.adb, '-s', args.serial]
PACKAGE = 'com.stradaobd.dash'
OUT = ROOT / 'build/identity-review'
OUT.mkdir(parents=True, exist_ok=True)


def adb(*command, data=None):
    return subprocess.check_output(ADB + list(command), input=data, stderr=subprocess.STDOUT, timeout=40)


def stop():
    adb('shell', 'am', 'force-stop', PACKAGE)


def start():
    adb('shell', 'am', 'start', '-W', '-n', PACKAGE + '/.MainActivity')
    time.sleep(1)


def prefs(values):
    node = ET.Element('map')
    for name, value in values.items():
        if isinstance(value, bool):
            ET.SubElement(node, 'boolean', name=name, value=str(value).lower())
        elif isinstance(value, int):
            ET.SubElement(node, 'int', name=name, value=str(value))
        elif isinstance(value, float):
            ET.SubElement(node, 'float', name=name, value=str(value))
        else:
            ET.SubElement(node, 'string', name=name).text = value
    return ET.tostring(node, encoding='utf-8', xml_declaration=True)


def write_prefs(name, values):
    path = 'shared_prefs/' + name + '.xml'
    adb('shell', 'run-as', PACKAGE, 'sh', '-c', "'cat > " + path + "'", data=prefs(values))


def nodes():
    adb('shell', 'uiautomator', 'dump', '/sdcard/torque-identity.xml')
    return ET.fromstring(adb('shell', 'cat', '/sdcard/torque-identity.xml')).iter('node')


def tap(label):
    for node in nodes():
        if node.get('text') == label or node.get('content-desc') == label:
            bounds = node.get('bounds').replace('][', ',').strip('[]').split(',')
            left, top, right, bottom = map(int, bounds)
            if right > left and bottom > top:
                adb('shell', 'input', 'tap', str((left + right) // 2), str((top + bottom) // 2))
                time.sleep(.5)
                return
    raise AssertionError('Control no visible: ' + label)


def capture(name):
    texts = [node.get('text') for node in nodes() if node.get('text')]
    (OUT / (name + '.png')).write_bytes(adb('exec-out', 'screencap', '-p'))
    (OUT / (name + '.json')).write_text(json.dumps(texts, ensure_ascii=False, indent=2))
    assert adb('shell', 'pidof', PACKAGE).strip(), 'Proceso detenido'
    print(name + ': OK', flush=True)
    return texts


class Elm(socketserver.StreamRequestHandler):
    def handle(self):
        header = ''
        while True:
            raw = bytearray()
            while not raw.endswith(b'\r'):
                try:
                    char = self.rfile.read(1)
                except OSError:
                    return
                if not char:
                    return
                raw.extend(char)
            command = raw.decode().strip().upper().replace(' ', '')
            if command.startswith('ATSH'):
                header = command[4:]
            if command.startswith('AT'):
                reply = 'ELM327 v1.5' if command in ('ATZ', 'ATI') else 'OK'
            elif command.startswith('01') and command[2:] in ['00', '20', '40', '60', '80', 'A0', 'C0', 'E0']:
                reply = '41 ' + command[2:] + ' FF FF FF FF'
            else:
                reply = {
                    '010C': '41 0C 11 30', '010D': '41 0D 28', '0105': '41 05 82', '0142': '41 42 36 B0',
                    '221001': '62 10 01 ' + ('0A' if header == '7E0' else '14'),
                    '221002': '62 10 02 01', '221003': 'NO DATA', '221004': '7F 22 31', '221005': '62 10 05 01'
                }.get(command, 'NO DATA')
            time.sleep(.025)
            try:
                self.wfile.write((reply + '\r>').encode())
                self.wfile.flush()
            except OSError:
                return


class Server(socketserver.ThreadingTCPServer):
    allow_reuse_address = True
    daemon_threads = True


adb('install', '-r', str(args.apk))
stop()
backup = adb('exec-out', 'run-as', PACKAGE, 'tar', '-cf', '-', 'shared_prefs')
(OUT / 'original-preferences.tar').write_bytes(backup)
server = Server(('0.0.0.0', 0), Elm)
threading.Thread(target=server.serve_forever, daemon=True).start()
port = server.server_address[1]
try:
    # Configuración aislada; el respaldo original se restaura incluso si una comprobación falla.
    adb('shell', 'run-as', PACKAGE, 'rm', '-rf', 'shared_prefs')
    adb('shell', 'run-as', PACKAGE, 'mkdir', 'shared_prefs')
    write_prefs('obd_connection', {'last_type': 'WIFI', 'last_name': 'ELM de prueba', 'last_address': '10.0.2.2:' + str(port),
                                  'wifi_host': '10.0.2.2', 'wifi_port': port, 'simulation_enabled': False, 'polling_mode': 'VISIBLE'})
    start()
    initial = capture('initial-driving')
    assert 'Refrigerante' in initial and 'Voltaje ECU' in initial, initial
    assert not any('Potencia' in text or 'Par ECU' in text or text == 'MARCHA' for text in initial), initial
    tap('A Medida')
    defaults = capture('initial-custom')
    assert all(label in defaults for label in ['Revoluciones', 'Velocidad', 'Refrigerante', 'Voltaje ECU']), defaults
    stop()
    definitions = [('ecu-a', 'DUP', 'ECU A', '221001', 'A', '7E0', ''),
                   ('ecu-b', 'DUP', 'ECU B', '221001', 'A', '7E1', ''),
                   ('invalid', 'INVALIDO', 'Respuesta inválida', '221002', 'B', '7E0', ''),
                   ('empty', 'VACIO', 'Sin respuesta', '221003', 'A', '7E0', ''),
                   ('unsupported', 'NO_ADMITE', 'No admitido', '221004', 'A', '7E0', ''),
                   ('field', 'CAMPO', 'Campo opcional', '221005', 'A', '7E0', 'A&128')]
    library = {'sensor_count': len(definitions)}
    dashboard = {'dash_tabs_count': 1, 'dash_tabs_selected': 'legacy', 'dash_tab_0_id': 'legacy',
                 'dash_tab_0_name': 'Identidad', 'dash_tab_0_grid': '2x3', 'dash_tab_0_slot_count': 6}
    for index, (pid_id, short, name, command, equation, header, availability) in enumerate(definitions):
        key = f'sensor_{index}_'
        library.update({key+'id': pid_id, key+'name': name, key+'short': short, key+'command': command,
                        key+'equation': equation, key+'header': header, key+'units': 'u', key+'min': 0.0,
                        key+'max': 100.0, key+'custom': True, key+'enabled': True, key+'category': 'CUSTOM',
                        key+'availability': availability})
        key = f'dash_tab_0_slot_{index}_'
        # Referencias antiguas sin pid_id: la migración debe distinguir ECU A y ECU B por título.
        dashboard.update({key+'index': index, key+'id': f'widget-{index}', key+'type': 'DIGITAL_HUD',
                          key+'pid': short, key+'title': name, key+'color': '#00E5FF', key+'size': 'STANDARD'})
    write_prefs('torque_sensors', library)
    write_prefs('torque_obd_prefs', dashboard)
    start()
    tap('A Medida')
    time.sleep(1.5)
    texts = capture('duplicate-ids-and-reasons')
    assert any('10.0' in text for text in texts) and any('20.0' in text for text in texts), texts
    for reason in ['Respuesta inválida', 'Sin respuesta', 'Sensor incompatible', 'Campo no disponible']:
        assert reason in texts, (reason, texts)
    stop()
    saved = ET.fromstring(adb('shell', 'run-as', PACKAGE, 'cat', 'shared_prefs/torque_obd_prefs.xml'))
    stored = {node.get('name'): node.text for node in saved if node.tag == 'string'}
    assert stored['dash_tab_0_slot_0_pid_id'] == 'ecu-a', stored
    assert stored['dash_tab_0_slot_1_pid_id'] == 'ecu-b', stored
    start()
    tap('A Medida')
    reopened = capture('after-restart')
    assert 'ECU A' in reopened and 'ECU B' in reopened, reopened
    assert any('10.0' in text for text in reopened) and any('20.0' in text for text in reopened), reopened
    (OUT / 'result.json').write_text(json.dumps({'passed': True, 'checks': ['default-driving', 'default-custom',
        'duplicate-ecu-values', 'four-absence-reasons', 'legacy-id-migration', 'restart-persistence']}, indent=2))
finally:
    stop()
    adb('shell', 'run-as', PACKAGE, 'rm', '-rf', 'shared_prefs')
    adb('shell', 'run-as', PACKAGE, 'tar', '-xf', '-', data=backup)
    server.shutdown()
    server.server_close()
    start()
    print('Preferencias originales restauradas.', flush=True)
