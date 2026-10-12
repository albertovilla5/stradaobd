#!/usr/bin/env python3
"""Comprueba el modo coche en un emulador y guarda capturas; restaura pantalla y preferencias."""
import argparse
import json
import pathlib
import re
import subprocess
import time
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parents[1]
p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--serial', default='emulator-5554')
p.add_argument('--simulate', action='store_true', help='Usar datos simulados durante la prueba')
p.add_argument('--scenes', nargs='+', default=['radio-1024', 'radio-800', 'radio-dense', 'portrait', 'large-text'])
a = p.parse_args()
if not a.serial.startswith('emulator-'):
    raise SystemExit('Esta prueba solo admite emuladores.')
ADB = [str(pathlib.Path.home() / 'Library/Android/sdk/platform-tools/adb'), '-s', a.serial]
PKG = 'com.stradaobd.dash'
OUT = ROOT / 'build/car-review'
OUT.mkdir(parents=True, exist_ok=True)
SCENES = {'radio-1024': (1024, 600, 160, 1), 'radio-800': (800, 480, 160, 1),
          'radio-dense': (1024, 600, 240, 1), 'portrait': (400, 800, 160, 1),
          'large-text': (1024, 600, 160, 1.5)}

def run(*args, data=None):
    return subprocess.check_output(ADB + list(args), input=data, stderr=subprocess.STDOUT, timeout=40)

def state():
    run('shell', 'uiautomator', 'dump', '/sdcard/car-review.xml')
    xml = run('shell', 'cat', '/sdcard/car-review.xml')
    root = ET.fromstring(xml)
    # A remembered adapter can cause a system Bluetooth prompt after restart.
    deny = next((n for n in root.iter('node') if n.get('package') == 'com.android.settings' and n.get('text') == 'Deny'), None)
    if deny is not None:
        tap(deny)
        run('shell', 'uiautomator', 'dump', '/sdcard/car-review.xml')
        xml = run('shell', 'cat', '/sdcard/car-review.xml')
    return xml

def bounds(n):
    return tuple(map(int, re.findall(r'\d+', n.get('bounds', ''))))

def tap(n):
    x1, y1, x2, y2 = bounds(n)
    run('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2))
    time.sleep(.25)

def find(label, xml=None):
    root = ET.fromstring(xml or state())
    parents = {c: n for n in root.iter() for c in n}
    for n in root.iter('node'):
        if label in (n.get('text'), n.get('content-desc')):
            while n.get('clickable') != 'true' and n in parents:
                n = parents[n]
            if n.get('clickable') == 'true':
                # Compose can expose a clickable row beyond its clipped viewport.
                # Tap the visible intersection, never the off-screen centre.
                x1, y1, x2, y2 = bounds(n)
                ancestor = n
                while ancestor in parents:
                    ancestor = parents[ancestor]
                    if ancestor.get('bounds'):
                        ax1, ay1, ax2, ay2 = bounds(ancestor)
                        x1, y1, x2, y2 = max(x1, ax1), max(y1, ay1), min(x2, ax2), min(y2, ay2)
                if x2 > x1 and y2 > y1:
                    visible = ET.Element('node', dict(n.attrib))
                    visible.set('bounds', f'[{x1},{y1}][{x2},{y2}]')
                    return visible
    return None

def click(label, scroll=False):
    for _ in range(12 if scroll else 2):
        n = find(label)
        if n is not None:
            tap(n)
            return
        if scroll:
            xml = ET.fromstring(state())
            scrollers = [n for n in xml.iter('node') if n.get('scrollable') == 'true']
            if not scrollers:
                break
            x1, y1, x2, y2 = bounds(scrollers[-1])
            run('shell', 'input', 'swipe', str((x1+x2)//2), str(int(y1+(y2-y1)*.8)), str((x1+x2)//2), str(int(y1+(y2-y1)*.2)), '250')
    raise AssertionError('Control inaccesible: ' + label)

results = []
def capture(scene, name, minimum_targets=False):
    xml = state()
    (OUT / f'{scene}-{name}.xml').write_bytes(xml)
    (OUT / f'{scene}-{name}.png').write_bytes(run('exec-out', 'screencap', '-p'))
    nodes = list(ET.fromstring(xml).iter('node'))
    if not run('shell', 'pidof', PKG).strip():
        raise AssertionError('La aplicación se ha detenido')
    if minimum_targets and a.simulate:
        assert any('SIMULACIÓN' in n.get('text', '').upper() or n.get('text') == 'Demo' for n in nodes), 'La simulación no ha conectado'
    if minimum_targets:
        ratio = SCENES[scene][2] / 160
        for label in ['Conducción', 'Paneles', 'Menú']:
            n = find(label, xml)
            assert n is not None, f'{scene}: falta {label}'
            x1, y1, x2, y2 = bounds(n)
            assert (x2-x1)/ratio >= 75 and (y2-y1)/ratio >= 75, f'{scene}: objetivo táctil pequeño: {label}'
    results.append({'scene': scene, 'screen': name, 'texts': [n.get('text') for n in nodes if n.get('text')]})
    (OUT / 'report.json').write_text(json.dumps(results, ensure_ascii=False, indent=2))
    print(scene + '/' + name, flush=True)

original_size = run('shell', 'wm', 'size').decode()
original_density = run('shell', 'wm', 'density').decode()
settings = {key: run('shell', 'settings', 'get', 'system', key).decode().strip()
            for key in ['font_scale', 'accelerometer_rotation', 'user_rotation']}
run('shell', 'am', 'force-stop', PKG)
prefs = run('exec-out', 'run-as', PKG, 'tar', '-cf', '-', 'shared_prefs')
(OUT / 'original-preferences.tar').write_bytes(prefs)
try:
    # Ensure the test starts in the default presentation without touching vehicle/dashboard data.
    run('shell', 'run-as', PKG, 'rm', '-f', 'shared_prefs/interface.xml')
    if a.simulate:
        try:
            xml = run('exec-out', 'run-as', PKG, 'cat', 'shared_prefs/obd_connection.xml')
            preferences = ET.fromstring(xml)
        except subprocess.CalledProcessError:
            preferences = ET.Element('map')
        option = next((n for n in preferences if n.get('name') == 'simulation_enabled'), None)
        if option is None:
            option = ET.SubElement(preferences, 'boolean', {'name': 'simulation_enabled'})
        option.set('value', 'true')
        run('shell', f"run-as {PKG} sh -c 'cat > shared_prefs/obd_connection.xml'", data=ET.tostring(preferences, encoding='utf-8', xml_declaration=True))
    for scene in a.scenes:
        w, h, dpi, font = SCENES[scene]
        run('shell', 'am', 'force-stop', PKG)
        run('shell', 'wm', 'size', f'{w}x{h}')
        run('shell', 'wm', 'density', str(dpi))
        run('shell', 'settings', 'put', 'system', 'font_scale', str(font))
        run('shell', 'settings', 'put', 'system', 'accelerometer_rotation', '0')
        run('shell', 'wm', 'user-rotation', 'lock', '0')
        run('shell', 'am', 'start', '-W', '-n', PKG + '/.MainActivity')
        time.sleep(1)
        capture(scene, 'driving', True)
        click('Paneles'); capture(scene, 'panels')
        click('Editar dashboard'); capture(scene, 'editor')
        click('Herramientas'); capture(scene, 'tools')
        if scene == a.scenes[0]:
            click('Añadir indicador', True); capture(scene, 'add-widget')
            assert find('Cancelar') is not None, 'Falta cancelar en el editor de indicador'
            assert any(n.get('text') == 'RPM' for n in ET.fromstring(state()).iter('node')), 'La lista de sensores no es visible'
            click('Configurar apariencia'); capture(scene, 'widget-appearance')
            click('Elegir sensor')
            click('Cancelar')
            click('Herramientas'); click('Disposición de rejilla', True); capture(scene, 'grid-layout')
            click('Cerrar')
            click('Herramientas'); click('Nueva pestaña', True); capture(scene, 'new-panel')
            click('Cancelar')
            click('Herramientas')
        click('Volver al panel'); click('Listo')
        click('Menú'); capture(scene, 'menu')
        if scene == a.scenes[0]:
            click('Vehículo', True); capture(scene, 'vehicle')
            click('Menú'); click('Averías', True); capture(scene, 'dtc')
            click('Menú'); click('Grabación', True); capture(scene, 'recording')
            click('Menú'); click('Ajustes', True); click('Avisos', True); capture(scene, 'alerts')
            click('Menú'); click('Ajustes', True); click('Terminal', True); capture(scene, 'terminal')
            click('Menú')
        click('Sensores', True); capture(scene, 'sensors')
        click('Filtrar sensores'); capture(scene, 'filters')
        run('shell', 'input', 'keyevent', '4')
        click('Menú'); click('Conexión', True); capture(scene, 'connection')
        click('WiFi', True); capture(scene, 'wifi')
        run('shell', 'input', 'keyevent', '4')
        click('Menú'); click('Ajustes', True); capture(scene, 'settings')
        click('Pantalla y lectura', True); capture(scene, 'preferences')
        click('Modo móvil', True); capture(scene, 'mobile')
        assert find('Conducción') is None, 'El selector no ha activado el modo móvil'
        click('Modo coche', True); capture(scene, 'restored-car')
        run('shell', 'am', 'force-stop', PKG)
        run('shell', 'am', 'start', '-W', '-n', PKG + '/.MainActivity')
        capture(scene, 'persisted-car', True)
    (OUT / 'report.json').write_text(json.dumps(results, ensure_ascii=False, indent=2))
finally:
    run('shell', 'am', 'force-stop', PKG)
    run('shell', 'run-as', PKG, 'rm', '-f', 'shared_prefs/interface.xml')
    run('shell', 'run-as', PKG, 'tar', '-xf', '-', data=prefs)
    for kind, original in [('size', original_size), ('density', original_density)]:
        override = re.search(r'Override .*?: (\S+)', original)
        run('shell', 'wm', kind, override.group(1) if override else 'reset')
    for key, value in settings.items():
        run('shell', 'settings', 'put', 'system', key, value)
    run('shell', 'am', 'start', '-W', '-n', PKG + '/.MainActivity')
print(f'{len(results)} pantallas comprobadas; preferencias y configuración restauradas.', flush=True)
