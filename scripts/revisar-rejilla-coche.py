#!/usr/bin/env python3
"""Comprueba selección, persistencia y geometría fija de la rejilla del coche. Solo emulador; restaura datos."""
import argparse
import copy
import json
import pathlib
import re
import subprocess
import time
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--serial', default='emulator-5554')
parser.add_argument('--scenes', nargs='+', default=['radio-800', 'radio-1024', 'radio-1280', 'radio-1280-dense', 'radio-wide', 'radio-fullhd', 'portrait', 'large-text'])
parser.add_argument('--release-apk', type=pathlib.Path, help='Comprueba los menús con la APK release y la deja instalada; debug respalda/restaura los datos')
args = parser.parse_args()
if not args.serial.startswith('emulator-'): raise SystemExit('Solo se permite un emulador.')
ADB = [str(pathlib.Path.home() / 'Library/Android/sdk/platform-tools/adb'), '-s', args.serial]
PKG = 'com.stradaobd.dash'
OUT = ROOT / 'build/fixed-grid-review'
OUT.mkdir(parents=True, exist_ok=True)
SCENES = {'radio-800': (800,480,160,1), 'radio-1024': (1024,600,160,1), 'radio-1280': (1280,720,160,1),
          'radio-1280-dense': (1280,720,240,1), 'radio-wide': (1280,480,160,1), 'radio-fullhd': (1920,720,240,1),
          'portrait': (400,800,160,1), 'large-text': (1024,600,160,1.5)}

def run(*args, data=None):
    return subprocess.check_output(ADB + list(args), input=data, stderr=subprocess.STDOUT, timeout=40)

def stop(): run('shell','am','force-stop',PKG)
def launch():
    run('shell','am','start','-W','-n',PKG+'/.MainActivity')
    time.sleep(.7)

def state():
    run('shell','uiautomator','dump','/sdcard/profile-review.xml')
    root = ET.fromstring(run('shell','cat','/sdcard/profile-review.xml'))
    permission = next((n for n in root.iter('node') if n.get('package','').endswith('permissioncontroller') and n.get('text') in ['Allow','Permitir']),None)
    if permission is not None:
        tap(permission);return state()
    deny = next((n for n in root.iter('node') if n.get('package')=='com.android.settings' and n.get('text')=='Deny'), None)
    if deny is not None:
        tap(deny); return state()
    return root

def bounds(n): return tuple(map(int,re.findall(r'\d+',n.get('bounds',''))))
def tap(n):
    x1,y1,x2,y2=bounds(n)
    run('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
    time.sleep(.15)

def find(label, tree=None):
    tree = tree if tree is not None else state()
    parents = {c:n for n in tree.iter() for c in n}
    for n in tree.iter('node'):
        if label in (n.get('text'), n.get('content-desc')):
            while n.get('clickable')!='true' and n in parents: n=parents[n]
            if n.get('clickable')!='true': continue
            x1,y1,x2,y2=bounds(n); ancestor=n
            while ancestor in parents:
                ancestor=parents[ancestor]
                if ancestor.get('bounds'):
                    a,b,c,d=bounds(ancestor)
                    x1,y1,x2,y2=max(x1,a),max(y1,b),min(x2,c),min(y2,d)
            if x2>x1 and y2>y1:
                result=ET.Element('node',dict(n.attrib));result.set('bounds',f'[{x1},{y1}][{x2},{y2}]');return result
    return None

def click(label, scroll=False):
    for _ in range(15 if scroll else 2):
        tree=state();n=find(label,tree)
        if n is not None:
            assert n.get('enabled')=='true', 'Desactivado: '+label
            tap(n);return
        if scroll:
            scrollers=[n for n in tree.iter('node') if n.get('scrollable')=='true']
            if not scrollers: break
            x1,y1,x2,y2=bounds(scrollers[-1])
            run('shell','input','swipe',str((x1+x2)//2),str(int(y1+(y2-y1)*.8)),str((x1+x2)//2),str(int(y1+(y2-y1)*.2)),'250')
    raise AssertionError('Inaccesible: '+label)

def capture(name):
    tree=state()
    (OUT/(name+'.xml')).write_bytes(ET.tostring(tree))
    (OUT/(name+'.png')).write_bytes(run('exec-out','screencap','-p'))
    print(name,flush=True);return tree

def assert_target(label, tree, dpi=160):
    n=find(label,tree);assert n is not None,'Falta: '+label
    x1,y1,x2,y2=bounds(n)
    assert (x2-x1)*160/dpi>=75 and (y2-y1)*160/dpi>=75, (label,bounds(n),dpi)

def configure(scene):
    w,h,dpi,font=SCENES[scene]
    run('shell','wm','size',f'{w}x{h}');run('shell','wm','density',str(dpi))
    run('shell','settings','put','system','font_scale',str(font))
    run('shell','settings','put','system','accelerometer_rotation','0');run('shell','wm','user-rotation','lock','0')

def prefs(name): return ET.fromstring(run('exec-out','run-as',PKG,'cat',f'shared_prefs/{name}.xml'))
def write(name, tree):
    run('shell',f"run-as {PKG} sh -c 'cat > shared_prefs/{name}.xml'",data=ET.tostring(tree,encoding='utf-8',xml_declaration=True))

def number(field, value, screenshot=None, dpi=160):
    click('Dato: '+field)
    tree=state()
    for digit in '0123456789': assert_target('Tecla '+digit,tree,dpi)
    if screenshot: capture(screenshot)
    for char in value.lstrip('-'):
        label='Separador decimal' if char=='.' else 'Tecla '+char
        node=find(label,tree)
        if node is None and char=='.': node=find('Tecla .',tree)
        assert node is not None,label
        tap(node)
    if value.startswith('-'): tap(find('Cambiar signo',tree))
    click('Aceptar número')


def validate_grid(tree, columns, rows):
    slots={int(n.get('content-desc').split()[-1]):bounds(n) for n in tree.iter('node') if re.fullmatch(r'Ranura \d+',n.get('content-desc',''))}
    assert set(slots)==set(range(1,columns*rows+1)),slots
    xs=sorted(set(b[0] for b in slots.values()));ys=sorted(set(b[1] for b in slots.values()))
    assert len(xs)==columns and len(ys)==rows,(columns,rows,slots)
    for i,b in slots.items():
        assert b[0]==xs[(i-1)%columns] and b[1]==ys[(i-1)//columns],(i,b)
        assert b[2]>b[0] and b[3]>b[1]
    assert not any(n.get('content-desc') in ['Página anterior','Página siguiente'] for n in tree.iter('node'))
    return slots

def choose(label):
    click('Editar dashboard');click('Herramientas');click('Disposición de rejilla',True)
    capture('selector-'+label.replace(' ','-'))
    click(label,True);click('Listo')

original_size=run('shell','wm','size').decode();original_density=run('shell','wm','density').decode()
settings={k:run('shell','settings','get','system',k).decode().strip() for k in ['font_scale','accelerometer_rotation','user_rotation']}
stop();backup=run('exec-out','run-as',PKG,'tar','-cf','-','shared_prefs')
(OUT/'original-preferences.tar').write_bytes(backup)
checks=[]
try:
    configure('radio-1024')
    profile={'id':'grid-vehicle','name':'Coche de prueba','modelYear':'2020','engineDisplacementLiters':1.5,
             'fuelTankCapacityLiters':50,'vehicleWeightKg':1300,'maxPowerHp':150,'maxTorqueNm':250,'maxRpm':6500,
             'fuelType':'GASOLINE','volumetricEfficiencyPercent':85,'boostPressureAdjustmentBar':0,
             'customInitCommands':'','hideUnsupportedSensors':True}
    tree=ET.Element('map')
    def pref(kind,name,value):
        n=ET.SubElement(tree,kind,name=name)
        if kind=='string':n.text=str(value)
        else:n.set('value',str(value))
    pref('string','saved_vehicles_json',json.dumps([profile]));pref('string','active_vehicle_id','grid-vehicle')
    pref('int','dash_tabs_count',1);pref('string','dash_tabs_selected','grid-review')
    for key,value in [('id','grid-review'),('name','Telemetría'),('grid','4x6'),('density','COMFORTABLE')]:pref('string','dash_tab_0_'+key,value)
    pref('int','dash_tab_0_slot_count',24)
    sensors=[('SAE_010C_RPM','RPM','Motor','#9DBAFF'),('SAE_010D_Velocidad','Velocidad','Velocidad','#8FCDFF'),
             ('SAE_0105_Temp Liq','Temp Liq','Agua','#8AE6D2'),('SAE_0142_Voltaje','Voltaje','Voltaje','#E9D5A1')]
    for i in range(24):
        key=f'dash_tab_0_slot_{i}_';pid,short,title,color=sensors[i%4]
        for suffix,value in [('id',f'widget-{i}'),('pid_id',pid),('pid',short),('title',title),('color',color),
                             ('type','DIGITAL_HUD'),('size','STANDARD')]:pref('string',key+suffix,value)
        pref('int',key+'index',i)
    write('torque_obd_prefs',tree)
    sim=ET.Element('map');ET.SubElement(sim,'boolean',name='simulation_enabled',value='true');write('obd_connection',sim)
    interface=ET.Element('map');ET.SubElement(interface,'boolean',name='car_mode',value='true');write('interface',interface)
    if args.release_apk:run('install','-r',str(args.release_apk))
    launch();click('Paneles')
    validate_grid(capture('legacy-bounded'),4,2)
    choose('1 columna × 2 filas');validate_grid(capture('chosen-1-column-2-rows'),1,2)
    stop();launch();click('Paneles');validate_grid(capture('restart-1-column-2-rows'),1,2)
    choose('4 columnas × 2 filas');validate_grid(capture('chosen-4-columns-2-rows'),4,2)
    for scene in args.scenes:
        stop();configure(scene);launch();click('Paneles')
        actual=capture(scene+'-fixed-4x2');before=validate_grid(actual,4,2)
        w,h,_,_=SCENES[scene]
        run('shell','input','swipe',str(int(w*.56)),str(int(h*.65)),str(int(w*.56)),str(int(h*.25)),'350')
        assert before==validate_grid(state(),4,2),'Scroll vertical en '+scene
        checks.append({'scene':scene,'columns':4,'rows':2,'slots':8,'no_reflow':True,'no_pagination':True,'no_vertical_scroll':True})
        (OUT/'report.json').write_text(json.dumps(checks,ensure_ascii=False,indent=2))
    if args.release_apk:
        stop();run('install','-r',str(ROOT/'app/build/outputs/apk/debug/app-debug.apk'))
    stored=prefs('torque_obd_prefs')
    value=lambda name:next(n.text for n in stored if n.get('name')==name)
    assert value('dash_tab_0_grid')=='4x6' and value('dash_tab_0_car_grid')=='2x4'
    assert value('dash_tab_0_slot_23_id')=='widget-23','Se ha perdido una ranura móvil'
    checks.append({'ui_selected_and_restarted':True,'mobile_grid_preserved':True,'hidden_slots_preserved':True,'release':bool(args.release_apk)})
    (OUT/'report.json').write_text(json.dumps(checks,ensure_ascii=False,indent=2))
finally:
    stop()
    if args.release_apk:run('install','-r',str(ROOT/'app/build/outputs/apk/debug/app-debug.apk'))
    run('shell','run-as',PKG,'rm','-rf','shared_prefs');run('shell','run-as',PKG,'tar','-xf','-',data=backup)
    for kind,original in [('size',original_size),('density',original_density)]:
        override=re.search(r'Override .*?: (\S+)',original);run('shell','wm',kind,override.group(1) if override else 'reset')
    for key,value in settings.items():run('shell','settings','put','system',key,value)
    if args.release_apk:run('install','-r',str(args.release_apk))
    launch()
print('Rejilla elegida, reinicio, ocho tamaños y datos móviles verificados; preferencias restauradas.',flush=True)
