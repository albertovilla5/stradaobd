#!/usr/bin/env python3
"""Verifica Inicio y todas las ranuras de Paneles sin scroll vertical; solo en emulador."""
import argparse
import json
import pathlib
import re
import subprocess
import time
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parents[1]
ADB = [str(pathlib.Path.home() / 'Library/Android/sdk/platform-tools/adb'), '-s', 'emulator-5554']
PKG = 'com.stradaobd.dash'
OUT = ROOT / 'build/cockpit-review'
OUT.mkdir(parents=True, exist_ok=True)
SCENES = {'radio-1024': (1024, 600, 160, 1), 'radio-800': (800, 480, 160, 1),
          'radio-dense': (1024, 600, 240, 1), 'portrait': (400, 800, 160, 1),
          'large-text': (1024, 600, 160, 1.5)}
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--scenes', nargs='+', choices=list(SCENES), default=list(SCENES))
parser.add_argument('--quick', action='store_true', help='Comprobar Inicio/Paneles/editor sin recorrer las ocho combinaciones de coche')
args = parser.parse_args()

def run(*args, data=None):
    return subprocess.check_output(ADB + list(args), input=data, stderr=subprocess.STDOUT, timeout=40)

def state():
    run('shell', 'uiautomator', 'dump', '/sdcard/cockpit-review.xml')
    tree=ET.fromstring(run('shell', 'cat', '/sdcard/cockpit-review.xml'))
    deny=next((n for n in tree.iter('node') if n.get('package')=='com.android.settings' and n.get('text')=='Deny'),None)
    if deny is not None:
        x1,y1,x2,y2=bounds(deny)
        run('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
        return state()
    return tree

def bounds(n):
    return tuple(map(int, re.findall(r'\d+', n.get('bounds', ''))))

def click(label):
    tree = state()
    parents = {c: n for n in tree.iter() for c in n}
    for n in tree.iter('node'):
        if label in (n.get('text'), n.get('content-desc')):
            while n.get('clickable') != 'true' and n in parents:
                n = parents[n]
            assert n.get('enabled') == 'true', 'Control desactivado: ' + label
            x1, y1, x2, y2 = bounds(n)
            run('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2))
            time.sleep(.2)
            return
    raise AssertionError('Falta: ' + label)

def capture(name):
    tree = state()
    (OUT / (name + '.xml')).write_bytes(ET.tostring(tree))
    (OUT / (name + '.png')).write_bytes(run('exec-out', 'screencap', '-p'))
    return tree

def slot_bounds(tree):
    return {int(n.get('content-desc').split()[-1]): bounds(n) for n in tree.iter('node')
            if re.fullmatch(r'Ranura \d+', n.get('content-desc', ''))}

def same_after_vertical_swipe(tree, scene, slots=False):
    w, h, dpi, _ = SCENES[scene]
    before = slot_bounds(tree) if slots else {n.get('text'): bounds(n) for n in tree.iter('node')
                                             if n.get('text') in ['Velocidad', 'km/h', 'RPM', '°C', 'V']}
    assert before, 'Faltan lecturas/ranuras'
    run('shell', 'input', 'swipe', str(int(w*.56)), str(int(h*.65)), str(int(w*.56)), str(int(h*.25)), '350')
    after_tree = state()
    after = slot_bounds(after_tree) if slots else {n.get('text'): bounds(n) for n in after_tree.iter('node')
                                                   if n.get('text') in ['Velocidad', 'km/h', 'RPM', '°C', 'V']}
    assert before == after, f'{scene}: el gesto vertical ha desplazado los instrumentos'
    return after_tree

def write_prefs(name, tree):
    run('shell', f"run-as {PKG} sh -c 'cat > shared_prefs/{name}.xml'",
        data=ET.tostring(tree, encoding='utf-8', xml_declaration=True))

def dashboard_fixture(layout, count):
    tree = ET.Element('map')
    def pref(kind, key, value):
        n = ET.SubElement(tree, kind, {'name': key})
        if kind == 'string': n.text = str(value)
        else: n.set('value', str(value))
    pref('string', 'saved_vehicles_json', json.dumps([{'id': 'cockpit-vehicle', 'name': 'Vehículo de prueba', 'modelYear': '2020', 'engineDisplacementLiters': 1.5, 'fuelTankCapacityLiters': 50, 'vehicleWeightKg': 1300, 'maxPowerHp': 150, 'maxTorqueNm': 250, 'maxRpm': 6500, 'fuelType': 'GASOLINE', 'volumetricEfficiencyPercent': 85, 'boostPressureAdjustmentBar': 0, 'customInitCommands': '', 'hideUnsupportedSensors': True}]))
    pref('string', 'active_vehicle_id', 'cockpit-vehicle')
    pref('int', 'dash_tabs_count', 1)
    pref('string', 'dash_tabs_selected', 'cockpit-review')
    for key, value in [('id', 'cockpit-review'), ('name', 'Telemetría'), ('grid', layout), ('density', 'COMFORTABLE')]:
        pref('string', 'dash_tab_0_' + key, value)
    pref('int', 'dash_tab_0_slot_count', count)
    sensors = [('SAE_010C_RPM', 'RPM', 'Motor', '#9DBAFF'), ('SAE_010D_Velocidad', 'Velocidad', 'Velocidad', '#8FCDFF'),
               ('SAE_0105_Temp Liq', 'Temp Liq', 'Refrigerante', '#8AE6D2'), ('SAE_0142_Voltaje', 'Voltaje', 'Voltaje ECU', '#E9D5A1')]
    types = ['ANALOG_GAUGE', 'DIGITAL_HUD', 'HORIZONTAL_BAR', 'COMPACT_TILE', 'SPARKLINE_CHART']
    for index in range(count):
        key = f'dash_tab_0_slot_{index}_'
        pid, short, title, color = sensors[index % 4]
        for suffix, value in [('id', f'widget-{index}'), ('pid_id', pid), ('pid', short), ('title', title),
                              ('color', color), ('type', types[index % 5]), ('size', 'STANDARD')]:
            pref('string', key + suffix, value)
        pref('int', key + 'index', index)
    write_prefs('torque_obd_prefs', tree)

def launch():
    run('shell', 'am', 'start', '-W', '-n', PKG + '/.MainActivity')
    time.sleep(.7)

original_size = run('shell', 'wm', 'size').decode()
original_density = run('shell', 'wm', 'density').decode()
settings = {key: run('shell', 'settings', 'get', 'system', key).decode().strip()
            for key in ['font_scale', 'accelerometer_rotation', 'user_rotation']}
run('shell', 'am', 'force-stop', PKG)
prefs = run('exec-out', 'run-as', PKG, 'tar', '-cf', '-', 'shared_prefs')
(OUT / 'original-preferences.tar').write_bytes(prefs)
results = []
try:
    run('shell', 'run-as', PKG, 'rm', '-f', 'shared_prefs/interface.xml')
    sim = ET.Element('map')
    ET.SubElement(sim, 'boolean', {'name': 'simulation_enabled', 'value': 'true'})
    write_prefs('obd_connection', sim)
    for scene in args.scenes:
        w, h, dpi, font = SCENES[scene]
        run('shell', 'am', 'force-stop', PKG)
        run('shell', 'wm', 'size', f'{w}x{h}')
        run('shell', 'wm', 'density', str(dpi))
        run('shell', 'settings', 'put', 'system', 'font_scale', str(font))
        run('shell', 'settings', 'put', 'system', 'accelerometer_rotation', '0')
        run('shell', 'wm', 'user-rotation', 'lock', '0')
        dashboard_fixture('2x2', 4)
        launch()
        home = capture(scene + '-inicio')
        same_after_vertical_swipe(home, scene)
        # Compare equal-size text in this frame; Android may scale sp non-linearly.
        reference = next(n for n in home.iter('node') if n.get('text') == 'Velocidad')
        _, ry1, _, ry2 = bounds(reference)
        for label in ['km/h', 'RPM', '°C', 'V']:
            node = next((n for n in home.iter('node') if n.get('text') == label), None)
            assert node is not None, f'{scene}: falta {label}'
            _, y1, _, y2 = bounds(node)
            assert y2 - y1 >= ry2 - ry1 - 1, f'{scene}: unidad cortada: {label}'
        click('Paneles')
        panels = capture(scene + '-paneles')
        same_after_vertical_swipe(panels, scene, slots=True)
        click('Editar dashboard'); capture(scene + '-editor'); click('Listo')
        if args.quick:
            results.append({'scene': scene, 'home_no_vertical_scroll': True, 'panels_no_vertical_scroll': True, 'editor': True})
            (OUT / 'quick-report.json').write_text(json.dumps(results, ensure_ascii=False, indent=2))
            print(scene + ': Inicio, Paneles y editor comprobados', flush=True)
            continue
        combinations=[('1x1',1,1),('1x2',2,1),('1x3',3,1),('1x4',4,1),
                      ('2x1',1,2),('2x2',2,2),('2x3',3,2),('2x4',4,2)]
        for layout,columns,rows in combinations:
            run('shell','am','force-stop',PKG)
            dashboard_fixture(layout,columns*rows)
            launch();click('Paneles')
            tree=capture(scene+'-grid-'+layout)
            slots=slot_bounds(tree)
            assert set(slots)==set(range(1,columns*rows+1)),(scene,layout,slots)
            xs=sorted(set(b[0] for b in slots.values()));ys=sorted(set(b[1] for b in slots.values()))
            assert len(xs)==columns and len(ys)==rows,(scene,layout,xs,ys)
            for index,b in slots.items():assert b[0]==xs[(index-1)%columns] and b[1]==ys[(index-1)//columns]
            assert not any(n.get('content-desc') in ['Página anterior','Página siguiente'] for n in tree.iter('node'))
            same_after_vertical_swipe(tree,scene,slots=True)
        results.append({'scene':scene,'home_no_vertical_scroll':True,'panels_no_vertical_scroll':True,
                        'exact_combinations':len(combinations),'no_pagination':True})
        (OUT/'report.json').write_text(json.dumps(results,ensure_ascii=False,indent=2))
        print(scene+': ocho rejillas exactas sin scroll ni paginación',flush=True)

finally:
    run('shell', 'am', 'force-stop', PKG)
    run('shell', 'run-as', PKG, 'rm', '-f', 'shared_prefs/interface.xml')
    run('shell', 'run-as', PKG, 'tar', '-xf', '-', data=prefs)
    for kind, original in [('size', original_size), ('density', original_density)]:
        override = re.search(r'Override .*?: (\S+)', original)
        run('shell', 'wm', kind, override.group(1) if override else 'reset')
    for key, value in settings.items():
        run('shell', 'settings', 'put', 'system', key, value)
    launch()
