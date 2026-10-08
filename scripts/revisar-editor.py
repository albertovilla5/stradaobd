#!/usr/bin/env python3
"""Comprueba la edición en varias rejillas y resoluciones sobre un emulador, restaurando sus datos al finalizar."""
import argparse
import json
import os
import pathlib
import re
import subprocess
import time
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parents[1]
SCENES = {
    'radio-800': (800, 480, 160, 0, 1.0),
    'radio-1024': (1024, 600, 240, 0, 1.0),
    'radio-1280': (1280, 720, 240, 0, 1.0),
    'compact-landscape': (640, 360, 160, 0, 1.0),
    'small-landscape': (480, 272, 160, 0, 1.0),
    'portrait-360': (360, 640, 160, 0, 1.0),
    'portrait-400': (400, 800, 160, 0, 1.0),
    'large-text': (1080, 2400, 420, 1, 1.5),
    'portrait-large-text': (400, 800, 160, 0, 1.5),
    'radio-text-200': (1024, 600, 240, 0, 2.0),
}
LAYOUTS = {'1x1':1,'1x2': 2, '1x3': 3, '1x4': 4, '2x2': 4, '2x3': 6,'2x4':8,'2x5':10,'2x6':12,'3x3':9,'3x4':12,'3x5':15,'3x6':18,'4x4':16,'4x5':20,'4x6':24, '1+2': 3, '2+1': 3}
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--serial', default='emulator-5554')
parser.add_argument('--scenes', nargs='+', choices=SCENES, default=list(SCENES))
parser.add_argument('--layouts', nargs='+', choices=LAYOUTS, default=list(LAYOUTS))
parser.add_argument('--output', type=pathlib.Path, default=ROOT / 'build/editor-review')
parser.add_argument('--density', choices=['normal','compact'], default='normal')
parser.add_argument('--fullscreen', action='store_true')
parser.add_argument('--skip-dialogs', action='store_true')
parser.add_argument('--extended-dialogs', action='store_true', help='Incluye edición, escalas, destinos y archivos')
parser.add_argument('--apk', type=pathlib.Path, default=ROOT / 'app/build/outputs/apk/debug/app-debug.apk')
args = parser.parse_args()
if not args.serial.startswith('emulator-'):
    raise SystemExit('Estas pruebas solo se ejecutan en un emulador.')
adb = [str(pathlib.Path(os.environ.get('ANDROID_HOME', pathlib.Path.home() / 'Library/Android/sdk')) / 'platform-tools/adb'), '-s', args.serial]
PACKAGE = 'com.torqueobd2.dash'
PREFS = 'shared_prefs/torque_obd_prefs.xml'
args.output.mkdir(parents=True, exist_ok=True)

def run(*command, data=None):
    return subprocess.run(adb + list(command), input=data, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=35, check=True).stdout

def state():
    run('shell', 'uiautomator', 'dump', '/sdcard/torque-editor-review.xml')
    return ET.fromstring(run('shell', 'cat', '/sdcard/torque-editor-review.xml'))

def rect(node):
    return tuple(map(int, re.findall(r'\d+', node.get('bounds', ''))))

def visible(node):
    box = rect(node)
    return len(box) == 4 and box[2] > box[0] and box[3] > box[1]

def nodes_named(tree, name):
    return [n for n in tree.iter('node') if name in (n.get('text'), n.get('content-desc')) and visible(n)]

def tap(name, tree=None):
    tree = tree if tree is not None else state()
    matches = nodes_named(tree, name)
    if not matches:
        raise AssertionError('Control no visible: ' + name)
    left, top, right, bottom = rect(matches[-1])
    run('shell', 'input', 'tap', str((left + right) // 2), str((top + bottom) // 2))
    time.sleep(.3)

def reach(name, click=True, right_column=False, minimum_height=0):
    direction=-1
    previous=None
    unchanged=0
    for _ in range(25):
        tree = state()
        matches = nodes_named(tree, name)
        if matches:
            target = matches[-1]
            parents = {child: parent for parent in tree.iter('node') for child in parent}
            while target.get('clickable') != 'true' and target in parents:
                target = parents[target]
            bounds = rect(target)
            if bounds[3] - bounds[1] >= minimum_height:
                if click: tap(name, tree)
                return tree
        scrollers = [n for n in tree.iter('node') if n.get('scrollable')=='true' and visible(n)]
        assert scrollers, 'Control no accesible: ' + name
        panels = nodes_named(tree, 'Herramientas del dashboard')
        if panels:
            px1, py1, px2, py2 = rect(panels[0])
            inside = [n for n in scrollers if px1 <= rect(n)[0] < rect(n)[2] <= px2
                      and py1 <= rect(n)[1] < rect(n)[3] <= py2]
            if inside: scrollers = inside
        vertical = [n for n in scrollers if n.get('class') == 'android.widget.ScrollView']
        target = max(vertical or scrollers, key=lambda n: rect(n)[0]) if right_column else max(
            scrollers, key=lambda n: (rect(n)[2]-rect(n)[0])*(rect(n)[3]-rect(n)[1]))
        left, top, right, bottom = rect(target)
        nested = [n for n in scrollers if n is not target and rect(n)[1]>top+50 and rect(n)[3]<=bottom
                  and left <= rect(n)[0] < rect(n)[2] <= right]
        start = min(rect(n)[1] for n in nested)-12 if nested else int(top+(bottom-top)*.50)
        end=max(top+4,min(bottom-4,start+direction*int((bottom-top)*.4)))
        run('shell','input','swipe',str((left+right)//2),str(start),str((left+right)//2),str(end),'500')
        signature=tuple((n.get('text'),n.get('content-desc'),n.get('bounds')) for n in tree.iter('node'))
        unchanged=unchanged+1 if signature==previous else 0
        previous=signature
        if unchanged>=2:direction=1;unchanged=0
    capture('failed', 'dialog', name)
    raise AssertionError('Control no alcanzable: ' + name)

def capture(scene, layout, mode):
    tree = state()
    folder = args.output / scene / layout.replace('+', '-')
    folder.mkdir(parents=True, exist_ok=True)
    (folder / (mode + '.xml')).write_bytes(ET.tostring(tree))
    (folder / (mode + '.png')).write_bytes(run('exec-out', 'screencap', '-p'))
    return tree

def write_preferences(data):
    run('shell', f"run-as {PACKAGE} sh -c 'cat > {PREFS}'", data=data)

def fixture(layout):
    tree = ET.Element('map')
    def put(key, value):
        if isinstance(value, int):
            ET.SubElement(tree, 'int', name=key, value=str(value))
        else:
            ET.SubElement(tree, 'string', name=key).text = str(value)
    put('dash_tabs_count', 1)
    put('dash_tabs_selected', 'resolution-review')
    put('dash_tab_0_id', 'resolution-review')
    put('dash_tab_0_name', 'Prueba de resolución')
    put('dash_tab_0_grid', layout)
    put('dash_tab_0_density', 'COMPACT' if args.density == 'compact' else 'COMFORTABLE')
    count = LAYOUTS[layout]
    put('dash_tab_0_slot_count', count if args.fullscreen else count - 1)
    examples = [('RPM', 'ANALOG_GAUGE', 'Tacómetro'), ('Velocidad', 'DIGITAL_HUD', 'Velocidad'),
                ('MAP', 'HORIZONTAL_BAR', 'Turbo'), ('EGT', 'SPARKLINE_CHART', 'Escape'),
                ('Temp Liq', 'COMPACT_TILE', 'Temperatura')]
    for index in range(count if args.fullscreen else count - 1):
        pid, kind, title = examples[index % len(examples)]
        title = f'{index+1} · {title}'
        for key, value in dict(index=index, id='test-' + str(index), pid=pid, type=kind, title=title, color='#FF6D00', size='STANDARD').items():
            put(f'dash_tab_0_slot_{index}_' + key, value)
    return ET.tostring(tree, encoding='utf-8', xml_declaration=True)

def grid_bounds(tree):
    grid = nodes_named(tree, 'Indicadores del dashboard')
    assert grid, 'Rejilla no visible'
    return rect(grid[0]), grid[0].get('scrollable') == 'true'

def cells(tree):
    return {int(n.get('content-desc').split()[-1]): rect(n) for n in tree.iter('node')
            if re.fullmatch(r'Ranura \d+', n.get('content-desc', '')) and visible(n)}

def validate_cells(tree, layout):
    box, scrollable = grid_bounds(tree)
    found = cells(tree)
    for index, cell in found.items():
        assert box[0] <= cell[0] < cell[2] <= box[2], ('Ranura fuera de ancho', index, box, cell)
        assert box[1] <= cell[1] < cell[3] <= box[3], ('Ranura fuera de alto', index, box, cell)
    if not scrollable:
        assert set(found) == set(range(1, LAYOUTS[layout] + 1)), ('Ranuras ocultas sin desplazamiento', layout, found)
    return found, scrollable

def validate_readings(tree, density, font):
    # Las cifras del tacómetro, velocímetro y tarjeta deben mantener altura legible.
    for slot in tree.iter('node'):
        if not re.fullmatch(r'Ranura \d+', slot.get('content-desc', '')):
            continue
        box = rect(slot)
        if len(box) != 4 or box[3] - box[1] < ((30 + 40 * font) if args.density == "compact" else (34 + 44 * font)) * density / 160 - 2:
            continue  # Una celda que cruza el borde se valida al desplazarla completa.
        index = int(slot.get('content-desc').split()[-1])
        minimum_sp = {1: 20, 2: 20, 3: 11, 4: 12, 5: 20}.get(index)
        for reading in slot.iter('node'):
            if minimum_sp and reading.get('text', '').startswith('—') and visible(reading):
                bounds = rect(reading)
                assert bounds[3] - bounds[1] >= minimum_sp * font * density / 160 * .85, ('Cifra recortada', index, reading.attrib)

def validate_edit_targets(tree):
    parents = {child: parent for parent in tree.iter('node') for child in parent}
    viewport, _ = grid_bounds(tree)
    for index, box in cells(tree).items():
        targets = nodes_named(tree, f'Editar indicador {index}')
        for target in targets:
            while target.get('clickable') != 'true' and target in parents:
                target = parents[target]
            assert target.get('clickable') == 'true', ('Indicador no táctil', index)
            target_box = rect(target)
            visible_box = (max(target_box[0], viewport[0]), max(target_box[1], viewport[1]),
                           min(target_box[2], viewport[2]), min(target_box[3], viewport[3]))
            assert visible_box == box, ('La zona táctil no ocupa toda la tarjeta visible', index, box, target_box)

def validate_action_height(tree, label, density):
    parents = {child: parent for parent in tree.iter('node') for child in parent}
    target = nodes_named(tree, label)[-1]
    while target.get('clickable') != 'true':
        target = parents[target]
    box = rect(target)
    assert box[3] - box[1] >= 56 * density / 160 - 1, ('Acción demasiado pequeña', label, box)

run('install', '-r', str(args.apk))
run('shell', 'am', 'force-stop', PACKAGE)
original_prefs = run('shell', 'run-as', PACKAGE, 'cat', PREFS)
original_size = run('shell', 'wm', 'size').decode()
original_density = run('shell', 'wm', 'density').decode()
original_settings = {key: run('shell', 'settings', 'get', 'system', key).decode().strip()
                     for key in ['font_scale', 'accelerometer_rotation', 'user_rotation']}
report = []
try:
    for scene in args.scenes:
        width, height, density, rotation, font = SCENES[scene]
        run('shell', 'am', 'force-stop', PACKAGE)
        run('shell', 'wm', 'size', f'{width}x{height}')
        run('shell', 'wm', 'density', str(density))
        run('shell', 'settings', 'put', 'system', 'font_scale', str(font))
        run('shell', 'settings', 'put', 'system', 'accelerometer_rotation', '0')
        run('shell', 'settings', 'put', 'system', 'user_rotation', str(rotation))
        run('shell', 'wm', 'user-rotation', 'lock', str(rotation))
        time.sleep(.5)
        for layout in args.layouts:
            run('shell', 'am', 'force-stop', PACKAGE)
            write_preferences(fixture(layout))
            run('shell', 'am', 'start', '-W', '-f', '0x10008000', '-n', PACKAGE + '/.MainActivity')
            time.sleep(.6)
            tap('A Medida')
            normal = capture(scene, layout, 'normal')
            normal_cells, _ = validate_cells(normal, layout)
            validate_readings(normal, density, font)
            normal_box, _ = grid_bounds(normal)
            assert nodes_named(normal, 'Editar dashboard'), 'Botón flotante ausente'
            assert not nodes_named(normal, 'Herramientas del dashboard'), 'Herramientas ocupan espacio al visualizar'
            tap('Editar dashboard', normal)
            edit = capture(scene, layout, 'edit')
            edit_cells, scrollable = validate_cells(edit, layout)
            validate_readings(edit, density, font)
            validate_edit_targets(edit)
            edit_box, _ = grid_bounds(edit)
            panel = rect(nodes_named(edit, 'Herramientas del dashboard')[0])
            assert edit_box[2] <= panel[0] or edit_box[3] <= panel[1], ('El panel tapa indicadores', edit_box, panel)
            assert (edit_box[2]-edit_box[0])*(edit_box[3]-edit_box[1]) < (normal_box[2]-normal_box[0])*(normal_box[3]-normal_box[1]), 'El editor no reserva espacio para las herramientas'
            assert nodes_named(edit, 'Terminar edición'), 'No se puede finalizar la edición'
            assert nodes_named(edit, 'Herramientas del dashboard'), 'Herramientas no visibles'
            if scrollable:
                minimum_cell = ((30 + 40 * font) if args.density == "compact" else (34 + 44 * font)) * density / 160 - 2
                seen = {index for index, box in edit_cells.items() if box[3] - box[1] >= minimum_cell}
                left, top, right, bottom = grid_bounds(edit)[0]
                unchanged = 0
                previous_cells = edit_cells
                for _ in range(LAYOUTS[layout] * 3):
                    if seen == set(range(1, LAYOUTS[layout]+1)):
                        break
                    # Arrastres cortos y lentos evitan saltarse una fila intermedia por inercia.
                    run('shell', 'input', 'swipe', str((left+right)//2), str(int(top+(bottom-top)*.50)),
                        str((left+right)//2), str(int(top+(bottom-top)*.15)), '700')
                    scrolled = state()
                    validate_readings(scrolled, density, font)
                    current_cells = cells(scrolled)
                    seen.update(index for index, box in current_cells.items() if box[3] - box[1] >= minimum_cell)
                    unchanged = unchanged + 1 if current_cells == previous_cells else 0
                    previous_cells = current_cells
                    if unchanged >= 3: break
                capture(scene,layout,'scroll-end')
                assert seen == set(range(1, LAYOUTS[layout]+1)), ('Ranuras inaccesibles al desplazar', scene, layout, seen)
            tap('Terminar edición')
            restored = capture(scene, layout, 'restored')
            assert grid_bounds(restored)[0] == normal_box, 'No recupera el espacio al terminar'
            assert not nodes_named(restored, 'Herramientas del dashboard'), 'Panel visible después de terminar'
            tap('Editar dashboard', restored)
            if args.fullscreen:
                reach('Pantalla completa')
                full = capture(scene, layout, 'fullscreen')
                full_cells, full_scrollable = validate_cells(full, layout)
                assert nodes_named(full, 'Editar dashboard'), 'Botón flotante oculto en pantalla completa'
                if scene == 'radio-1024' and layout == '4x6' and args.density == 'compact':
                    assert not full_scrollable and len(full_cells) == 24, ('No caben 24 en pantalla completa',full_cells)
                tap('Editar dashboard',full)
                reach('Salir de pantalla completa')
                tap('Editar dashboard')
            report.append(dict(scene=scene, layout=layout, density=args.density, scrollable=scrollable))
            (args.output / 'report.json').write_text(json.dumps(report, ensure_ascii=False, indent=2))
            print(f'{scene}/{layout}: lectura y edición OK' + (' (desplazamiento comprobado)' if scrollable else ' (todas las ranuras visibles)'), flush=True)
        if args.skip_dialogs:
            continue
        # Los menús y diálogos se prueban una vez en cada resolución.
        tree = state()
        button = next(n.get('content-desc') for n in tree.iter('node')
                      if re.fullmatch(r'Editar indicador \d+', n.get('content-desc', '')) and visible(n))
        tap(button, tree)  # Toca el centro del indicador, lejos de la marca de la esquina.
        menu = capture(scene, layout, 'indicator-actions')
        assert nodes_named(menu, 'Herramientas del dashboard'), 'Acciones fuera del panel'
        for label in ['Editar indicador', 'Mover / intercambiar', 'Duplicar indicador', 'Vaciar ranura']:
            menu = reach(label, click=False, minimum_height=56 * density / 160 - 1)
            validate_action_height(menu, label, density)
        reach('Editar indicador')
        editor = capture(scene, layout, 'widget-editor')
        index = int(button.split()[-1])
        titles = ['Tacómetro', 'Velocidad', 'Turbo', 'Escape', 'Temperatura']
        expected_title = f'{index} · {titles[(index-1) % len(titles)]}'
        assert any(n.get('class') == 'android.widget.EditText' and n.get('text') == expected_title
                   for n in editor.iter('node')), ('Edita un indicador distinto', expected_title)
        assert nodes_named(editor, 'Guardar indicador'), 'No se puede guardar desde el panel'
        tap('Cancelar', editor)
        reach('Mover / intercambiar')
        transfer = capture(scene, layout, 'transfer-dialog')
        assert any(n.get('content-desc', '').startswith('Destino ranura ') for n in transfer.iter('node')), 'Destinos ausentes'
        tap('Cancelar', transfer)
        tap('Ver opciones del dash')
        reach('Disposición de rejilla', click=False)
        tree = capture(scene, layout, 'tools')
        validate_action_height(tree, 'Disposición de rejilla', density)
        tap('Disposición de rejilla', tree)
        tree = capture(scene, layout, 'grid-dialog')
        tap('Cerrar', tree)
        reach('Renombrar pestaña')
        tree = capture(scene, layout, 'name-dialog')
        field = next(n for n in tree.iter('node') if n.get('class') == 'android.widget.EditText' and visible(n))
        left, top, right, bottom = rect(field)
        run('shell', 'input', 'tap', str((left+right)//2), str((top+bottom)//2))
        time.sleep(.5)
        tree = capture(scene, layout, 'keyboard')
        assert any(n.get('class')=='android.widget.EditText' and visible(n) for n in tree.iter('node')), 'Campo oculto por teclado'
        assert nodes_named(tree, 'Guardar'), 'Guardar oculto por teclado'
        tap('Cancelar', tree)
        time.sleep(.5)
        if args.extended_dialogs:
            reach('Importar / exportar diseños')
            capture(scene,layout,'design-files');reach('Exportar todas las pestañas',click=False)
            reach('Abrir archivo de diseños',click=False);tap('Cerrar')
            tree=state()
            button=next(n.get('content-desc') for n in tree.iter('node') if n.get('content-desc','').startswith('Editar indicador ') and visible(n))
            tap(button);tap('Mover / intercambiar')
            capture(scene,layout,'transfer-dialog')
            reach('Destino ranura '+str(LAYOUTS[layout]),click=False);tap('Cancelar')
            tap(button);tap('Editar indicador')
            reach('Título del indicador',click=False);capture(scene,layout,'edit-title')
            reach('Decimales',click=False,right_column=True);reach('Escala personalizada',click=False,right_column=True)
            capture(scene,layout,'edit-options');tap('Cancelar')
            tap(button);tap('Vaciar ranura')
            reach('Añadir varios indicadores')
            capture(scene,layout,'bulk-dialog');reach('Seleccionar hasta '+str(1 if args.fullscreen else 2),click=False)
            tree=state();assert nodes_named(tree,'Cancelar') and nodes_named(tree,'Añadir seleccionados')
            tap('Cancelar')
        tap('Terminar edición')
        print(f'{scene}: herramientas, rejilla, nombre y teclado OK', flush=True)
finally:
    run('shell', 'am', 'force-stop', PACKAGE)
    write_preferences(original_prefs)
    size = re.search(r'Override size: (\S+)', original_size)
    density = re.search(r'Override density: (\d+)', original_density)
    run('shell', 'wm', 'size', size.group(1) if size else 'reset')
    run('shell', 'wm', 'density', density.group(1) if density else 'reset')
    for key, value in original_settings.items():
        run('shell', 'settings', 'put', 'system', key, value)
    run('shell', 'wm', 'user-rotation', 'free' if original_settings['accelerometer_rotation']=='1' else 'lock', *([] if original_settings['accelerometer_rotation']=='1' else [original_settings['user_rotation']]))
    run('shell', 'am', 'start', '-W', '-f', '0x10008000', '-n', PACKAGE + '/.MainActivity')
(args.output / 'report.json').write_text(json.dumps(report, ensure_ascii=False, indent=2))
print(f'{len(report)} combinaciones verificadas. Datos y pantalla originales restaurados.', flush=True)
