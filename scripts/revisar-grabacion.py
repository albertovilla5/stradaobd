#!/usr/bin/env python3
"""Comprueba grabación y sensores en segundo plano con un ELM TCP en un AVD temporal."""
import argparse
import csv
import io
import socketserver
import threading
import json
import pathlib
import re
import subprocess
import time
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--serial', default='emulator-5560')
parser.add_argument('--apk', type=pathlib.Path, default=ROOT / 'app/build/outputs/apk/debug/app-debug.apk')
args = parser.parse_args()
if not args.serial.startswith('emulator-'):
    raise SystemExit('Solo emuladores dedicados a desarrollo.')
ADB = [str(pathlib.Path.home() / 'Library/Android/sdk/platform-tools/adb'), '-s', args.serial]
PACKAGE = 'com.stradaobd.dash'
OUT = ROOT / 'build/recording-review'
OUT.mkdir(parents=True, exist_ok=True)


def adb(*command, data=None):
    return subprocess.check_output(ADB + list(command), input=data, stderr=subprocess.STDOUT, timeout=40)


def stop():
    adb('shell', 'am', 'force-stop', PACKAGE)


def start():
    adb('shell', 'am', 'start', '-W', '-f', '0x10008000', '-n', PACKAGE + '/.MainActivity')
    time.sleep(.7)


def state():
    adb('shell', 'rm', '-f', '/sdcard/torque-alerts.xml')
    adb('shell', 'uiautomator', 'dump', '/sdcard/torque-alerts.xml')
    state.last_tree = ET.fromstring(adb('shell', 'cat', '/sdcard/torque-alerts.xml'))
    return state.last_tree


def rect(node):
    return tuple(map(int, re.findall(r'\d+', node.get('bounds', ''))))


def visible(node):
    bounds = rect(node)
    return len(bounds) == 4 and bounds[2] > bounds[0] and bounds[3] > bounds[1]


def find(label, tree=None, contains=False):
    tree = tree if tree is not None else state()
    return next((n for n in tree.iter('node') if visible(n) and (
        label in (n.get('text', '') if contains else [n.get('text'), n.get('content-desc')]))), None)


def tap_node(node):
    left, top, right, bottom = rect(node)
    adb('shell', 'input', 'tap', str((left + right) // 2), str((top + bottom) // 2))
    time.sleep(.2)


def tap(label, contains=False):
    node = find(label, contains=contains)
    if node is None and label in ('Deportivo', 'A medida', 'Vehículo', 'Sensores', 'Ajustes', 'Averías'):
        node = reach(label)
    assert node is not None, 'No visible: ' + label
    tap_node(node)


def reach(label, contains=False):
    prefer_up = label in ('Iniciar grabación', 'Detener grabación', 'Lectura en segundo plano', 'Límite inferior', 'Añadir aviso', 'Buscar sensor o PID', 'Deportivo', 'A medida')
    for upwards in [prefer_up] * 10 + [not prefer_up] * 10:
        tree = state()
        node = find(label, tree, contains)
        if node is not None:
            return node
        scrolls = [n for n in tree.iter('node') if n.get('scrollable') == 'true' and visible(n)]
        navigation = label in ('Deportivo', 'A medida', 'Vehículo', 'Sensores', 'Ajustes', 'Averías', 'Grabación', 'General')
        if navigation:
            candidates = [n for n in scrolls if any(c.get('text') in (('General', 'Conexión', 'Terminal', 'Avisos', 'Grabación') if label in ('Grabación', 'General') else ('Deportivo', 'A medida', 'Vehículo', 'Ajustes', 'Averías')) for c in n.iter('node'))]
            scrolls = candidates or scrolls
        tools = find('Herramientas del dashboard', tree)
        if tools is not None:
            scrolls = [n for n in tools.iter('node') if n.get('scrollable') == 'true' and visible(n)] or scrolls
            upwards = False
        assert scrolls, 'Sin scroll para: ' + label
        left, top, right, bottom = rect(max(scrolls, key=lambda n: (rect(n)[2] - rect(n)[0]) * (rect(n)[3] - rect(n)[1])))
        x = (left + right) // 2
        low = str(int(top + (bottom - top) * .8))
        high = str(int(top + (bottom - top) * .2))
        if navigation and right - left > 2 * (bottom - top):
            y = str((top + bottom) // 2)
            low_x = str(int(left + (right - left) * .2))
            high_x = str(int(left + (right - left) * .8))
            adb('shell', 'input', 'swipe', low_x if upwards else high_x, y, high_x if upwards else low_x, y, '350')
        else:
            adb('shell', 'input', 'swipe', str(x), high if upwards else low, str(x), low if upwards else high, '350')
    raise AssertionError('No alcanzable: ' + label)


def field(label, value):
    reach(label, contains=True)
    tree = state.last_tree
    parent = {child: node for node in tree.iter('node') for child in node}
    node = find(label, tree, contains=True)
    while node is not None:
        fields = [n for n in node.iter('node') if n.get('class') == 'android.widget.EditText' and visible(n)]
        if fields:
            break
        node = parent.get(node)
    assert node is not None and len(fields) == 1, label
    tap_node(fields[0])
    adb('shell', 'input', 'keyevent', 'KEYCODE_MOVE_END', *(['KEYCODE_DEL'] * 30))
    if value:
        adb('shell', 'input', 'text', value)
    adb('shell', 'input', 'keyevent', 'KEYCODE_BACK')
    time.sleep(.2)


def capture(name):
    tree = state()
    (OUT / (name + '.xml')).write_bytes(ET.tostring(tree))
    (OUT / (name + '.png')).write_bytes(adb('exec-out', 'screencap', '-p'))
    print(name + ': OK', flush=True)
    return tree


def prefs(name, values):
    root = ET.Element('map')
    for key, value in values.items():
        if isinstance(value, bool):
            ET.SubElement(root, 'boolean', name=key, value=str(value).lower())
        elif isinstance(value, int):
            ET.SubElement(root, 'int', name=key, value=str(value))
        else:
            ET.SubElement(root, 'string', name=key).text = str(value)
    adb('shell', 'run-as', PACKAGE, 'sh', '-c', "'cat > shared_prefs/" + name + ".xml'", data=ET.tostring(root))


def saved():
    root = ET.fromstring(adb('shell', 'run-as', PACKAGE, 'cat', 'shared_prefs/sensor_alerts.xml'))
    return {n.get('name'): n.text if n.tag == 'string' else n.get('value') for n in root}


def alerts_tab():
    tap('Ajustes')
    tap('Avisos')


def choose_sensor(search):
    tap('Seleccionar sensor')
    field('Buscar sensor o PID', search)
    tree = state()
    items = [n for n in tree.iter('node') if n.get('clickable') == 'true' and visible(n)
             and any(search in c.get('text', '') for c in n.iter('node'))
             and n.get('class') != 'android.widget.EditText']
    assert items, search
    tap_node(items[-1])


commands = []
fixture = {'offline': False, 'invalid': False, 'counter': 0}

class Elm(socketserver.StreamRequestHandler):
    def handle(self):
        while True:
            try:
                raw = bytearray()
                while not raw.endswith(b'\r'):
                    char = self.rfile.read(1)
                    if not char: return
                    raw.extend(char)
                if fixture['offline']: return
                command = raw.decode().strip().upper().replace(' ', '')
                commands.append((time.monotonic(), command))
                if command.startswith('AT'):
                    reply = 'ELM327 v1.5' if command in ('ATZ', 'ATI') else ('A6' if command == 'ATDPN' else 'OK')
                elif command == '0100': reply = '41 00 08 18 00 00'
                elif command == '010C':
                    fixture['counter'] += 1
                    rpm = (1000 + fixture['counter'] % 100) * 4
                    reply = f'41 0C {rpm // 256:02X} {rpm % 256:02X}'
                elif command == '0105': reply = 'NO DATA' if fixture['invalid'] else '41 05 7E'
                elif command == '010D': reply = '41 0D 32'
                else: reply = 'NO DATA'
                time.sleep(.025)
                self.wfile.write((reply + '\r>').encode()); self.wfile.flush()
            except OSError: return

class Server(socketserver.ThreadingTCPServer):
    allow_reuse_address = True
    daemon_threads = True

def recording_tab():
    tap('Ajustes'); tap_node(reach('Grabación'))

def click(label):
    tap_node(reach(label))

def switch_background(enabled):
    tree = state()
    parents = {child: node for node in tree.iter('node') for child in node}
    node = find('Lectura en segundo plano', tree)
    if node is None:
        reach('Lectura en segundo plano'); tree = state.last_tree
        parents = {child: node for node in tree.iter('node') for child in node}
        node = find('Lectura en segundo plano', tree)
    while node is not None:
        switches = [n for n in node.iter('node') if n.get('checkable') == 'true' and visible(n)]
        if switches: break
        node = parents.get(node)
    assert node is not None and len(switches) == 1
    if (switches[0].get('checked') == 'true') != enabled: tap_node(switches[0])

def sessions():
    names = adb('shell', 'run-as', PACKAGE, 'ls', 'no_backup/recordings').decode().split()
    return sorted(n[:-4] for n in names if n.endswith('.csv'))

def rows(id):
    content = adb('exec-out', 'run-as', PACKAGE, 'cat', 'no_backup/recordings/' + id + '.csv').decode(errors='ignore')
    parsed = list(csv.reader(io.StringIO(content)))
    if parsed and len(parsed[-1]) != 19: parsed.pop()
    assert all(len(row) == 19 for row in parsed), parsed[-3:]
    return parsed

def samples(id):
    return [r for r in rows(id) if r[0] == 'SENSOR']

def active_wakelocks():
    power = adb('shell', 'dumpsys', 'power').decode()
    return power.split('\nWake Locks:', 1)[1].split('\nSuspend Blockers:', 1)[0]

def active_id():
    for id in sessions():
        text = adb('shell', 'run-as', PACKAGE, 'cat', 'no_backup/recordings/' + id + '.info').decode()
        if '\nstatus=\n' in text: return id
    raise AssertionError('No hay una grabación activa')

def wait_for(predicate, timeout=20):
    until = time.monotonic() + timeout
    while time.monotonic() < until:
        if predicate(): return
        time.sleep(.4)
    raise AssertionError('Tiempo agotado')

results = []
def passed(name):
    results.append(name); print(name + ': OK', flush=True)

# Esta prueba reinicia preferencias y datos: se limita al AVD temporal creado para ella.
assert adb('shell', 'getprop', 'ro.boot.qemu.avd_name').decode().strip() == 'TorqueRecordingChecks', 'Necesita el AVD temporal TorqueRecordingChecks'
adb('install', '-r', str(args.apk)); stop()
adb('shell', 'pm', 'grant', PACKAGE, 'android.permission.BLUETOOTH_CONNECT')
adb('shell', 'pm', 'grant', PACKAGE, 'android.permission.BLUETOOTH_SCAN')
adb('shell', 'pm', 'grant', PACKAGE, 'android.permission.POST_NOTIFICATIONS')
adb('shell', 'run-as', PACKAGE, 'mkdir', '-p', 'shared_prefs', 'no_backup')
backup = adb('exec-out', 'run-as', PACKAGE, 'tar', '-cf', '-', 'shared_prefs', 'no_backup')
(OUT / 'original-data.tar').write_bytes(backup)
server = Server(('127.0.0.1', 0), Elm)
threading.Thread(target=server.serve_forever, daemon=True).start()
try:
    adb('shell', 'run-as', PACKAGE, 'rm', '-rf', 'shared_prefs', 'no_backup/recordings')
    adb('shell', 'run-as', PACKAGE, 'mkdir', '-p', 'shared_prefs')
    port = server.server_address[1]
    prefs('obd_connection', {'last_type': 'WIFI', 'last_name': 'ELM grabacion de prueba', 'last_address': f'10.0.2.2:{port}',
        'wifi_host': '10.0.2.2', 'wifi_port': port, 'simulation_enabled': False, 'polling_mode': 'VISIBLE', 'background_enabled': True})
    prefs('torque_obd_prefs', {'saved_vehicles_json': json.dumps([
        {'id': 'recording-car-a', 'name': 'Coche A', 'customInitCommands': ''},
        {'id': 'recording-car-b', 'name': 'Coche B', 'customInitCommands': ''}]), 'active_vehicle_id': 'recording-car-a'})
    adb('shell', 'wm', 'size', '1024x600'); adb('shell', 'wm', 'density', '160')
    adb('shell', 'settings', 'put', 'system', 'font_scale', '1.0')
    start(); recording_tab()
    wait_for(lambda: '010C' in [c for _, c in commands])
    capture('01-panel')
    assert find('Servicio de lectura activo') is not None
    click('Iniciar grabación'); time.sleep(2)
    first = active_id(); wait_for(lambda: len(samples(first)) > 20)
    data = samples(first)
    assert {r[12] for r in data} >= {'010C', '0105', '010D'}
    assert all(r[7] == 'WIFI' and r[5] == 'recording-car-a' and r[17] == 'AVAILABLE' for r in data)
    passed('todos-los-sensores-habilitados-en-modo-visible')
    count = len(data)
    adb('shell', 'settings', 'put', 'global', 'always_finish_activities', '1')
    adb('shell', 'input', 'keyevent', 'KEYCODE_HOME'); time.sleep(4)
    assert len(samples(first)) > count + 10
    passed('graba-al-cambiar-de-app-incluso-destruyendo-la-actividad')
    count = len(samples(first))
    adb('shell', 'input', 'keyevent', 'KEYCODE_SLEEP'); time.sleep(4)
    assert len(samples(first)) > count + 10
    assert 'StradaOBD:telemetry' in active_wakelocks()
    passed('graba-con-pantalla-apagada')
    fixture['offline'] = True; time.sleep(4)
    count = len(samples(first)); time.sleep(3)
    assert len(samples(first)) == count
    wait_for(lambda: any(r[17] == 'RECONNECTING' for r in rows(first) if r[0] == 'EVENT'), 20)
    fixture['offline'] = False
    wait_for(lambda: len(samples(first)) > count + 10, 30)
    assert active_id() == first
    passed('corte-y-reconexion-sin-inventar-lecturas')
    adb('shell', 'input', 'keyevent', 'KEYCODE_WAKEUP'); adb('shell', 'wm', 'dismiss-keyguard')
    adb('shell', 'cmd', 'statusbar', 'expand-notifications'); time.sleep(.5)
    capture('02-notificacion')
    click('Detener grabación'); time.sleep(2)
    assert ',COMPLETED,' in adb('shell', 'run-as', PACKAGE, 'cat', f'no_backup/recordings/{first}.csv').decode()
    adb('shell', 'cmd', 'statusbar', 'collapse')
    passed('detener-grabacion-desde-notificacion')
    before = len(commands); time.sleep(2); assert len(commands) > before
    passed('lectura-continua-tras-detener-grabacion')
    start(); recording_tab(); capture('03-sesion-finalizada')
    click('Exportar CSV'); time.sleep(.5)
    tree = state()
    filename = next(n.get('text') for n in tree.iter('node') if n.get('class') == 'android.widget.EditText' and n.get('text', '').endswith('.csv'))
    button = next(n for n in tree.iter('node') if n.get('text') in ('SAVE', 'Save', 'GUARDAR', 'Guardar') and n.get('enabled') == 'true')
    tap_node(button); time.sleep(1)
    exported = adb('shell', 'cat', '/sdcard/Download/' + filename)
    stored = adb('exec-out', 'run-as', PACKAGE, 'cat', f'no_backup/recordings/{first}.csv')
    assert exported == stored
    (OUT / 'exported-session.csv').write_bytes(exported)
    adb('shell', 'rm', '/sdcard/Download/' + filename)
    passed('exportacion-csv-completa')
    switch_background(False)
    click('Iniciar grabación'); time.sleep(2); second = active_id()
    adb('shell', 'input', 'keyevent', 'KEYCODE_HOME'); count = len(samples(second)); time.sleep(4)
    assert len(samples(second)) > count + 10
    passed('grabacion-mantiene-servicio-con-ajuste-desactivado')
    # Detención inesperada del proceso: conserva datos, no reinicia la grabación.
    stop(); fixture_count = len(commands); time.sleep(2); assert len(commands) == fixture_count
    start(); recording_tab(); time.sleep(1)
    info = adb('shell', 'run-as', PACKAGE, 'cat', f'no_backup/recordings/{second}.info').decode()
    assert '\nstatus=INTERRUPTED\n' in info
    assert find('Iniciar grabación') or reach('Iniciar grabación') is not None
    passed('recupera-sesion-interrumpida-sin-reiniciarla')
    before = len(commands)
    adb('shell', 'input', 'keyevent', 'KEYCODE_HOME'); time.sleep(3)
    settled = len(commands); time.sleep(3); assert len(commands) == settled
    assert 'isForeground=true' not in adb('shell', 'dumpsys', 'activity', 'services', PACKAGE).decode()
    start(); wait_for(lambda: len(commands) > settled)
    passed('suspende-y-reanuda-cuando-se-desactiva-segundo-plano')
    recording_tab(); switch_background(True)
    click('Iniciar grabación'); time.sleep(2); third = active_id()
    tap('Vehículo'); click('Garaje') if find('Garaje') else None
    # Cambio de origen mediante General cierra la sesión antes de generar muestras de prueba.
    recording_tab(); tap_node(reach('General'))
    tree = state(); node = find('Modo simulación', tree)
    if node is None: reach('Modo simulación'); tree = state.last_tree; node = find('Modo simulación', tree)
    parents = {child: node for node in tree.iter('node') for child in node}
    while node is not None:
        switches = [n for n in node.iter('node') if n.get('checkable') == 'true' and visible(n)]
        if switches: break
        node = parents.get(node)
    tap_node(switches[-1]); time.sleep(2)
    assert all(r[7] == 'WIFI' for r in samples(third))
    assert '\nstatus=COMPLETED\n' in adb('shell', 'run-as', PACKAGE, 'cat', f'no_backup/recordings/{third}.info').decode()
    recording_tab(); click('Iniciar grabación'); time.sleep(2); simulated = active_id()
    assert samples(simulated) and all(r[7] == 'SIMULATION' and r[17] == 'SIMULATED' for r in samples(simulated))
    capture('04-simulacion'); click('Detener grabación')
    passed('separa-y-marca-simulacion')
    # Formulario utilizable en vertical con letra grande.
    adb('shell', 'wm', 'size', '600x1024'); adb('shell', 'settings', 'put', 'system', 'font_scale', '1.5'); time.sleep(.5)
    start(); recording_tab(); click('Iniciar grabación'); time.sleep(2); click('Detener grabación')
    capture('05-vertical-letra-grande')
    passed('controles-en-vertical-con-letra-grande')
    click('Eliminar'); click('Cancelar')
    count = len(sessions()); click('Eliminar')
    # La confirmación es el último control Eliminar, en el diálogo.
    nodes = [n for n in state().iter('node') if n.get('text') == 'Eliminar' and visible(n)]
    tap_node(nodes[-1]); time.sleep(.5); assert len(sessions()) == count - 1
    passed('eliminacion-con-confirmacion')
    adb('shell', 'cmd', 'statusbar', 'expand-notifications'); time.sleep(.5)
    capture('06-detener-lectura'); click('Detener lectura')
    adb('shell', 'cmd', 'statusbar', 'collapse')
    time.sleep(2)
    stopped = len(commands); time.sleep(2); assert len(commands) == stopped
    assert 'isForeground=true' not in adb('shell', 'dumpsys', 'activity', 'services', PACKAGE).decode()
    assert 'StradaOBD:telemetry' not in active_wakelocks()
    passed('detener-lectura-libera-servicio-y-bloqueos')
    log = adb('shell', 'logcat', '-d', '-s', 'AndroidRuntime:E').decode()
    assert 'FATAL EXCEPTION' not in log, log
    (OUT / 'result.json').write_text(json.dumps({'checks': results, 'samples_first_session': len(samples(first)), 'passed': True}, ensure_ascii=False, indent=2))
finally:
    for id in sessions():
        (OUT / (id + '.csv')).write_bytes(adb('exec-out', 'run-as', PACKAGE, 'cat', 'no_backup/recordings/' + id + '.csv'))
    (OUT / 'runtime-log.txt').write_bytes(adb('shell', 'logcat', '-d', '-s', 'AndroidRuntime:E'))
    adb('shell', 'input', 'keyevent', 'KEYCODE_WAKEUP')
    stop(); server.shutdown(); server.server_close()
    adb('shell', 'run-as', PACKAGE, 'rm', '-rf', 'shared_prefs', 'no_backup')
    adb('shell', 'run-as', PACKAGE, 'tar', '-xf', '-', data=backup)
    adb('shell', 'wm', 'size', 'reset'); adb('shell', 'wm', 'density', 'reset')
    adb('shell', 'settings', 'put', 'system', 'font_scale', '1.0')
    adb('shell', 'settings', 'put', 'global', 'always_finish_activities', '0')
