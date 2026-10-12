#!/usr/bin/env python3
"""Prueba los avisos por vehículo en un emulador dedicado; restaura preferencias y pantalla."""
import argparse
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
OUT = ROOT / 'build/alerts-review'
OUT.mkdir(parents=True, exist_ok=True)


def adb(*command, data=None):
    return subprocess.check_output(ADB + list(command), input=data, stderr=subprocess.STDOUT, timeout=40)


def stop():
    adb('shell', 'am', 'force-stop', PACKAGE)


def start():
    adb('shell', 'am', 'start', '-W', '-f', '0x10008000', '-n', PACKAGE + '/.MainActivity')
    time.sleep(.7)


def state():
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
    prefer_up = label in ('Límite inferior', 'Añadir aviso', 'Buscar sensor o PID', 'Deportivo', 'A medida')
    for upwards in [prefer_up] * 10 + [not prefer_up] * 10:
        tree = state()
        node = find(label, tree, contains)
        if node is not None:
            return node
        scrolls = [n for n in tree.iter('node') if n.get('scrollable') == 'true' and visible(n)]
        navigation = label in ('Deportivo', 'A medida', 'Vehículo', 'Sensores', 'Ajustes', 'Averías')
        if navigation:
            candidates = [n for n in scrolls if any(c.get('text') in ('Deportivo', 'A medida', 'Vehículo', 'Ajustes', 'Averías') for c in n.iter('node'))]
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


adb('install', '-r', str(args.apk))
stop()
adb('shell', 'run-as', PACKAGE, 'mkdir', '-p', 'shared_prefs')
backup = adb('exec-out', 'run-as', PACKAGE, 'tar', '-cf', '-', 'shared_prefs')
(OUT / 'original-preferences.tar').write_bytes(backup)
original_size = adb('shell', 'wm', 'size').decode()
original_density = adb('shell', 'wm', 'density').decode()
original_font = adb('shell', 'settings', 'get', 'system', 'font_scale').decode().strip()
try:
    adb('shell', 'run-as', PACKAGE, 'rm', '-rf', 'shared_prefs')
    adb('shell', 'run-as', PACKAGE, 'mkdir', 'shared_prefs')
    prefs('obd_connection', {'simulation_enabled': True, 'polling_mode': 'VISIBLE'})
    prefs('torque_obd_prefs', {'saved_vehicles_json': json.dumps([
        {'id': 'alerts-car-a', 'name': 'Coche A', 'customInitCommands': ''},
        {'id': 'alerts-car-b', 'name': 'Coche B', 'customInitCommands': ''}]), 'active_vehicle_id': 'alerts-car-a'})
    adb('shell', 'pm', 'grant', PACKAGE, 'android.permission.BLUETOOTH_CONNECT')
    adb('shell', 'wm', 'size', '1024x600')
    adb('shell', 'wm', 'density', '240')
    adb('shell', 'settings', 'put', 'system', 'font_scale', '1.0')
    start()
    alerts_tab()
    capture('empty-radio')
    tap('Añadir aviso')
    tap('Guardar aviso')
    assert find('Selecciona un sensor disponible', contains=True) is not None
    choose_sensor('010C')
    field('Límite superior', '1')
    field('Persistencia', '30')
    capture('editor-radio')
    tap('Guardar aviso')
    assert saved()['rule_0_vehicle'] == 'alerts-car-a'
    assert saved()['rule_0_pid'] == 'SAE_010C_RPM'
    reach('Esperando persistencia', contains=True)
    capture('pending-radio')
    tap_node(reach('Editar'))
    field('Límite inferior', '5')
    tap('Guardar aviso')
    assert find('El límite inferior debe ser menor', contains=True) is not None
    field('Límite inferior', '')
    field('Persistencia', '1')
    tap('Guardar aviso')
    time.sleep(1.2)
    assert find('AVISO DE LA APP', contains=True) is not None
    capture('active-radio')
    tap('Deportivo')
    assert find('AVISO DE LA APP', contains=True) is not None
    capture('active-sport')
    tap('A medida')
    tap('Editar dashboard')
    tap_node(reach('Pantalla completa'))
    assert find('AVISO DE LA APP', contains=True) is not None
    capture('active-fullscreen')
    tap('Avisos de la aplicación')
    assert find('Por encima del límite superior') is not None
    capture('details-radio')
    tap('Cerrar')
    adb('shell', 'input', 'keyevent', 'KEYCODE_BACK')
    # Persistencia de la regla; la sesión vuelve a confirmar con muestras nuevas.
    stop(); start(); alerts_tab()
    assert saved()['rule_0_duration'] == '1'
    time.sleep(1.2)
    reach('Aviso activo', contains=True)
    # La misma regla no aparece ni genera avisos al elegir otro coche.
    tap('Vehículo')
    tap_node(reach('Activar este Vehículo'))
    alerts_tab()
    assert find('Este vehículo aún no tiene avisos', contains=True) is not None
    assert find('AVISO DE LA APP', contains=True) is None
    capture('other-vehicle')
    tap('Vehículo')
    tap_node(reach('Activar este Vehículo'))
    alerts_tab()
    time.sleep(1.2)
    tap_node(reach('Editar'))
    field('Límite superior', '99999')
    tap('Guardar aviso')
    assert find('AVISO DE LA APP', contains=True) is None
    reach('Dentro de límites', contains=True)
    capture('recovered-radio')
    # Límite inferior con coma decimal, segundo aviso y resumen múltiple.
    tap_node(reach('Añadir aviso'))
    choose_sensor('0142')
    field('Límite inferior', '99,2')
    field('Persistencia', '0')
    tap('Guardar aviso')
    assert saved()['rule_1_lower'] == '99.2'
    tap_node(reach('Editar'))
    field('Límite superior', '1')
    tap('Guardar aviso')
    time.sleep(1.2)
    assert find('2 activos', contains=True) is not None
    capture('multiple-radio')
    # Ventana estrecha con letra grande: formulario desplazable y botones visibles.
    adb('shell', 'wm', 'size', '600x1024')
    adb('shell', 'wm', 'density', '240')
    adb('shell', 'settings', 'put', 'system', 'font_scale', '1.3')
    time.sleep(.5)
    capture('active-portrait-large-text')
    tap_node(reach('Editar'))
    capture('editor-portrait-large-text')
    tap('Cancelar')
    # Desactivación persistente sin borrar la configuración.
    reach('Añadir aviso')
    tree = state()
    switch = next(n for n in tree.iter('node') if n.get('checkable') == 'true' and visible(n))
    tap_node(switch)
    assert saved()['rule_0_enabled'] == 'false'
    stop(); start(); alerts_tab()
    assert saved()['rule_0_enabled'] == 'false'
    tap_node(reach('Eliminar'))
    tap('Cancelar')
    assert saved()['count'] == '2'
    tap_node(reach('Eliminar'))
    tap('Eliminar')
    assert saved()['count'] == '1'
    capture('deleted-portrait')
    (OUT / 'result.json').write_text(json.dumps({'passed': True, 'checks': [
        'create', 'invalid-input', 'inverted-limits', 'duration', 'hidden-sensor-polling', 'simulation-label',
        'all-screens-banner', 'fullscreen', 'details', 'restart-persistence', 'vehicle-scope', 'recovery',
        'lower-decimal-comma', 'multiple-alerts', 'portrait-large-text', 'disable-persistence', 'delete-confirmation']}, indent=2))
except Exception:
    capture('failure')
    raise
finally:
    stop()
    adb('shell', 'run-as', PACKAGE, 'rm', '-rf', 'shared_prefs')
    adb('shell', 'run-as', PACKAGE, 'tar', '-xf', '-', data=backup)
    for prop, original in [('size', original_size), ('density', original_density)]:
        override = re.search(r'Override (?:size|density): (\S+)', original)
        adb('shell', 'wm', prop, override.group(1) if override else 'reset')
    if original_font == 'null':
        adb('shell', 'settings', 'delete', 'system', 'font_scale')
    else:
        adb('shell', 'settings', 'put', 'system', 'font_scale', original_font)
    adb('shell', 'rm', '-f', '/sdcard/torque-alerts.xml')
    print('Preferencias y pantalla restauradas.', flush=True)
