#!/usr/bin/env python3
"""ADB visual checks on an emulator. Captures navigation screens at different display settings."""
import argparse
import json
import os
import pathlib
import re
import struct
import time
import subprocess
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--serial', default='emulator-5556')
parser.add_argument('--output', type=pathlib.Path, default=ROOT / 'build/layout-review')
parser.add_argument('--scene', default='pixel-landscape')
parser.add_argument('--screen')
parser.add_argument('--scenes', nargs='+', choices=list(['pixel-landscape', 'radio-800', 'radio-1024', 'radio-1280', 'compact-landscape', 'portrait-400', 'portrait-360', 'large-text', 'portrait-large-text']))
parser.add_argument('--action', choices=['capture', 'nav', 'text', 'back', 'keyboard', 'scroll', 'configure', 'matrix'], default='capture')
parser.add_argument('--value')
args = parser.parse_args()
if not args.serial.startswith('emulator-'):
    raise SystemExit('Use an emulator: these checks temporarily change its display and font settings.')
sdk = pathlib.Path(os.environ.get('ANDROID_HOME', pathlib.Path.home() / 'Library/Android/sdk'))
adb = [str(sdk / 'platform-tools/adb'), '-s', args.serial]
OUT = args.output
OUT.mkdir(parents=True, exist_ok=True)
TABS = [('Deportivo', 'sport'), ('A Medida', 'custom'),
        ('Vehículo', 'vehicle'), ('Sensores', 'sensors'), ('Ajustes', 'settings')]
SETTINGS_TABS = [('General', 'settings-general'), ('Conexión', 'connection'), ('Terminal', 'terminal')]
SCENES = {
    'pixel-landscape': (1080, 2400, 420, 1, 1.0),
    'radio-800': (800, 480, 160, 0, 1.0),
    'radio-1024': (1024, 600, 240, 0, 1.0),
    'radio-1280': (1280, 720, 240, 0, 1.0),
    'compact-landscape': (640, 360, 160, 0, 1.0),
    'portrait-400': (400, 800, 160, 0, 1.0),
    'portrait-360': (360, 640, 160, 0, 1.0),
    'large-text': (1080, 2400, 420, 1, 1.5),
    'portrait-large-text': (400, 800, 160, 0, 1.5),
}

def run(*command):
    return subprocess.check_output(adb + list(command), stderr=subprocess.STDOUT, timeout=35)

def screenshot():
    return run('exec-out', 'screencap', '-p')

def screen_size():
    return struct.unpack('>II', screenshot()[16:24])

def state():
    run('shell', 'uiautomator', 'dump', '/sdcard/torque-layout-review.xml')
    return run('shell', 'cat', '/sdcard/torque-layout-review.xml').decode()

def rect(node):
    return tuple(map(int, re.findall(r'\d+', node.get('bounds', ''))))

def positive(box):
    return len(box) == 4 and box[2] > box[0] and box[3] > box[1]

def tap(node):
    box = rect(node)
    if not positive(box):
        raise RuntimeError('Element has no visible bounds')
    run('shell', 'input', 'tap', str((box[0] + box[2]) // 2), str((box[1] + box[3]) // 2))

def tap_text(value):
    nodes = list(ET.fromstring(state()).iter('node'))
    matches = [n for n in nodes if n.get('text') == value or n.get('content-desc') == value]
    matches = [n for n in matches if positive(rect(n))]
    if not matches:
        raise RuntimeError('Element not visible: ' + value)
    tap(matches[-1])

def navigate(title):
    if title in dict(SETTINGS_TABS):
        navigate('Ajustes')
        tap_text(title)
        return
    desired = next(i for i, (name, _) in enumerate(TABS) if name == title)
    width, height = screen_size()
    for attempt in range(32):
        nodes = list(ET.fromstring(state()).iter('node'))
        for node in nodes:
            if node.get('content-desc') == title and positive(rect(node)):
                tap(node)
                return
        visible = [next((i for i, (name, _) in enumerate(TABS) if name == n.get('content-desc')), -1)
                   for n in nodes if positive(rect(n))]
        visible = [i for i in visible if i >= 0]
        backwards = bool(visible and desired < min(visible))
        scrollers = [rect(n) for n in nodes if n.get('scrollable') == 'true' and positive(rect(n))]
        if width > height:
            scrollers = [b for b in scrollers if b[0] < width * .2 and b[2] < width * .35]
        else:
            scrollers = [b for b in scrollers if b[1] > height * .7]
        if not scrollers:
            for n in nodes:
                if n.get('text') == 'Wait' and positive(rect(n)):
                    tap(n)
            if attempt < 31:
                continue
            raise RuntimeError('Navigation scroller not found: ' + title)
        left, top, right, bottom = scrollers[0]
        if width > height:
            x = (left + right) // 2
            y1, y2 = int(top + (bottom-top)*.25), int(top + (bottom-top)*.75)
            command = (x, y1, x, y2) if backwards else (x, y2, x, y1)
        else:
            y = (top + bottom) // 2
            x1, x2 = int(left + (right-left)*.2), int(left + (right-left)*.8)
            command = (x1, y, x2, y) if backwards else (x2, y, x1, y)
        run('shell', 'input', 'swipe', *(str(v) for v in command), '500')
    raise RuntimeError('Unable to reach ' + title)

def configure(name):
    w, h, density, rotation, font = SCENES[name]
    run('shell', 'am', 'force-stop', 'com.stradaobd.dash')
    run('shell', 'wm', 'size', f'{w}x{h}')
    run('shell', 'wm', 'density', str(density))
    run('shell', 'settings', 'put', 'system', 'font_scale', str(font))
    run('shell', 'settings', 'put', 'system', 'accelerometer_rotation', '0')
    run('shell', 'settings', 'put', 'system', 'user_rotation', str(rotation))
    run('shell', 'wm', 'user-rotation', 'lock', str(rotation))
    run('shell', 'am', 'start', '-W', '-n', 'com.stradaobd.dash/.MainActivity')
    expected = (h, w) if rotation % 2 else (w, h)
    for attempt in range(4):
        run('shell', 'wm', 'user-rotation', 'lock', str(rotation))
        time.sleep(.8)
        if screen_size() == expected:
            break
    else:
        raise AssertionError(f'{name}: expected display {expected}, got {screen_size()}')


def capture(scene, name):
    xml = state()
    nodes = list(ET.fromstring(xml).iter('node'))
    # A resize can briefly make System UI busy on the emulator.
    for node in nodes:
        if node.get('text') == 'Wait':
            tap(node)
            xml = state()
            nodes = list(ET.fromstring(xml).iter('node'))
            break
    pixels = screenshot()
    width, height = struct.unpack('>II', pixels[16:24])
    target = OUT / scene
    target.mkdir(parents=True, exist_ok=True)
    (target / (name + '.xml')).write_text(xml)
    (target / (name + '.png')).write_bytes(pixels)
    readings = [{'text': n.get('text'), 'bounds': rect(n)} for n in nodes if n.get('text') and positive(rect(n))]
    if not run('shell', 'pidof', 'com.stradaobd.dash').strip():
        raise AssertionError('App process stopped')
    result = {'scene': scene, 'screen': name, 'width': width, 'height': height, 'readings': readings}
    (target / (name + '.json')).write_text(json.dumps(result, indent=2, ensure_ascii=False))
    print(f'{scene}/{name}: {width}×{height} OK', flush=True)
    return result

if args.action == 'matrix':
    run('shell', 'am', 'force-stop', 'com.stradaobd.dash')
    results = []
    for scene in args.scenes or SCENES:
        configure(scene)
        titles = TABS if scene in ['pixel-landscape', 'radio-1024', 'portrait-400', 'large-text'] else TABS[:4]
        for title, name in titles:
            navigate(title)
            results.append(capture(scene, name))
            if title == 'Ajustes':
                for section, section_name in SETTINGS_TABS:
                    navigate(section)
                    results.append(capture(scene, section_name))
    (OUT / 'report.json').write_text(json.dumps(results, indent=2, ensure_ascii=False))
    print(f'{len(results)} screen checks completed', flush=True)
elif args.action == 'configure':
    configure(args.scene)
    capture(args.scene, args.screen or 'configured')
elif args.action == 'nav':
    navigate(args.value)
    capture(args.scene, args.screen or next(name for title, name in TABS + SETTINGS_TABS if title == args.value))
elif args.action == 'text':
    tap_text(args.value)
    capture(args.scene, args.screen or 'dialog')
elif args.action == 'back':
    run('shell', 'input', 'keyevent', '4')
    capture(args.scene, args.screen or 'back')
elif args.action == 'keyboard':
    edit = next(n for n in ET.fromstring(state()).iter('node') if n.get('class') == 'android.widget.EditText')
    tap(edit)
    time.sleep(.6)
    if args.value:
        run('shell', 'input', 'text', args.value)
    capture(args.scene, args.screen or 'keyboard')
elif args.action == 'scroll':
    w, h = screen_size()
    run('shell', 'input', 'swipe', str(int(w*.7)), str(int(h*.75)), str(int(w*.7)), str(int(h*.3)), '250')
    capture(args.scene, args.screen or 'scrolled')
else:
    capture(args.scene, args.screen or 'current')
