#!/usr/bin/env python3
"""Asistente, perfiles y averías en radios habituales. Solo emulador; restaura datos y pantalla."""
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
parser.add_argument('--skip-setup', action='store_true', help='Usar una ficha de prueba para repetir solo la matriz de pantallas')
args = parser.parse_args()
if not args.serial.startswith('emulator-'): raise SystemExit('Solo se permite un emulador.')
ADB = [str(pathlib.Path.home() / 'Library/Android/sdk/platform-tools/adb'), '-s', args.serial]
PKG = 'com.stradaobd.dash'
OUT = ROOT / 'build/vehicle-review'
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

original_size=run('shell','wm','size').decode();original_density=run('shell','wm','density').decode()
settings={k:run('shell','settings','get','system',k).decode().strip() for k in ['font_scale','accelerometer_rotation','user_rotation']}
stop();backup=run('exec-out','run-as',PKG,'tar','-cf','-','shared_prefs')
(OUT/'original-preferences.tar').write_bytes(backup)
checks=[]
try:
    if not args.skip_setup:
        run('shell','run-as',PKG,'rm','-rf','shared_prefs');run('shell','run-as',PKG,'mkdir','shared_prefs')
        sim=ET.Element('map');ET.SubElement(sim,'boolean',name='simulation_enabled',value='true');write('obd_connection',sim)
        configure('radio-800');launch();tree=capture('first-launch')
        assert find('Conducción',tree) is None and any(n.get('text')=='Configura tu vehículo' for n in tree.iter('node'))
        assert not any(n.get('name')=='saved_vehicles_json' for n in prefs('torque_obd_prefs')), 'Se ha creado un perfil ficticio'
        click('Siguiente');tree=capture('required-name')
        assert find('Dato: Nombre del vehículo',tree) is not None
        click('Dato: Nombre del vehículo');capture('name-keyboard')
        run('shell','input','text','Seat%sLeon');click('Siguiente')
        number('Año','2020','large-number-pad');click('Siguiente')
        click('Gasolina');click('Siguiente')
        for field,value in [('Cilindrada','1.5'),('Capacidad del depósito','50'),('Peso del vehículo','1300'),('Potencia máxima','150'),
                            ('Par máximo','250'),('Régimen máximo','6500'),('Eficiencia volumétrica','85'),('Ajuste de presión','-0.2')]:
            number(field,value);click('Siguiente')
        click('Siguiente') # automatic adapter commands, explicitly optional
        tree=state();switch=next(n for n in tree.iter('node') if n.get('checkable')=='true');tap(switch)
        click('Siguiente');capture('review-profile');click('Guardar')
        time.sleep(1);tree=capture('saved-first-profile')
        stored=prefs('torque_obd_prefs')
        payload=next(n.text for n in stored if n.get('name')=='saved_vehicles_json');vehicles=json.loads(payload)
        assert len(vehicles)==1 and vehicles[0]['name']=='Seat Leon', vehicles
        expected={'modelYear':'2020','engineDisplacementLiters':1.5,'fuelTankCapacityLiters':50,'vehicleWeightKg':1300,
                  'maxPowerHp':150,'maxTorqueNm':250,'maxRpm':6500,'fuelType':'GASOLINE','volumetricEfficiencyPercent':85,
                  'boostPressureAdjustmentBar':-.2,'customInitCommands':'','hideUnsupportedSensors':False}
        for key,value in expected.items():
            actual=vehicles[0][key]
            assert abs(actual-value)<.0001 if isinstance(value,float) else actual==value, (key,actual,value)
        assert next(n.text for n in stored if n.get('name')=='active_vehicle_id')==vehicles[0]['id']
        stop();launch();tree=capture('restart-saved-profile');assert find('Conducción',tree) is not None
        # Explicitly empty saved list must not resurrect the old legacy fields.
        empty=copy.deepcopy(stored);next(n for n in empty if n.get('name')=='saved_vehicles_json').text='[]'
        stop();write('torque_obd_prefs',empty);launch();tree=capture('empty-list-with-legacy')
        assert find('Conducción',tree) is None
        stop();write('torque_obd_prefs',stored)
        checks.append({'first_launch_setup':True,'no_sample_profile':True,'all_fields_saved':True,'restart_skips_setup':True,'empty_list_not_resurrected':True})
    else:
        vehicles=[{'id':'profile-review','name':'Seat Leon','modelYear':'2020','engineDisplacementLiters':1.5,
                   'fuelTankCapacityLiters':50,'vehicleWeightKg':1300,'maxPowerHp':150,'maxTorqueNm':250,'maxRpm':6500,
                   'fuelType':'GASOLINE','volumetricEfficiencyPercent':85,'boostPressureAdjustmentBar':-.2,
                   'customInitCommands':'','hideUnsupportedSensors':False}]
        stored=ET.Element('map')
        ET.SubElement(stored,'string',name='saved_vehicles_json').text=json.dumps(vehicles)
        ET.SubElement(stored,'string',name='active_vehicle_id').text='profile-review'
        write('torque_obd_prefs',stored)
        sim=ET.Element('map');ET.SubElement(sim,'boolean',name='simulation_enabled',value='true');write('obd_connection',sim)
        interface=ET.Element('map');ET.SubElement(interface,'boolean',name='car_mode',value='true');write('interface',interface)
    if args.release_apk: run('install','-r',str(args.release_apk))
    for scene in args.scenes:
        stop();configure(scene);launch();tree=capture(scene+'-inicio')
        dpi=SCENES[scene][2]
        for label in ['Conducción','Paneles','Averías','Menú']: assert_target(label,tree,dpi)
        click('Averías');tree=capture(scene+'-averias')
        assert_target('Leer averías',tree,dpi)
        click('Leer averías')
        for _ in range(8):
            tree=state()
            if any('P0300' in n.get('text','') for n in tree.iter('node')): break
            time.sleep(.5)
        assert any('P0300' in n.get('text','') for n in tree.iter('node')), 'No se ha detectado el DTC simulado'
        capture(scene+'-codigos')
        code = next(n.get('text') for n in tree.iter('node') if n.get('text','').startswith('P0300'))
        click(code);detail = capture(scene+'-dtc-detalle')
        assert_target('Cerrar detalle',detail,dpi);click('Cerrar detalle')
        click('Opciones de averías');click('Borrar averías',True)
        confirmation = capture(scene+'-dtc-confirmacion')
        for label in ['Cancelar','Confirmar']: assert_target(label,confirmation,dpi)
        assert find('Confirmar',confirmation).get('enabled')=='false', 'Borrado sin confirmar motor parado'
        click('Cancelar')
        click('Opciones de averías');capture(scene+'-dtc-opciones')
        click('Monitores y testigo',True);capture(scene+'-monitores')
        click('Menú');click('Vehículo',True);capture(scene+'-vehiculo')
        click('Garaje',True);capture(scene+'-garaje')
        click('Perfil: Seat Leon');click('Editar ficha',True);tree=capture(scene+'-editar-perfil')
        assert_target('Siguiente',tree,dpi)
        click('Siguiente');click('Dato: Año');tree=capture(scene+'-teclado-grande')
        for digit in '0123456789':assert_target('Tecla '+digit,tree,dpi)
        click('Cancelar número' if find('Cancelar número') is not None else 'Cancelar');click('Cerrar ficha')
        if args.release_apk:
            stop();run('install','-r',str(ROOT/'app/build/outputs/apk/debug/app-debug.apk'))
        assert json.loads(next(n.text for n in prefs('torque_obd_prefs') if n.get('name')=='saved_vehicles_json'))==vehicles
        if args.release_apk: run('install','-r',str(args.release_apk))
        checks.append({'scene':scene,'release_build':bool(args.release_apk),'navigation_76dp':True,'dtc_direct_access':True,'read_action_visible':True,
                       'simulated_code_detected':True,'detail_close_76dp':True,'clear_confirmation_76dp':True,'clear_disabled_until_confirmed':True,'profile_editor':True,'number_keys_76dp':True,'cancel_keeps_profile':True})
        (OUT/'report.json').write_text(json.dumps(checks,ensure_ascii=False,indent=2))
finally:
    stop()
    if args.release_apk: run('install','-r',str(ROOT/'app/build/outputs/apk/debug/app-debug.apk'))
    run('shell','run-as',PKG,'rm','-rf','shared_prefs');run('shell','run-as',PKG,'tar','-xf','-',data=backup)
    for kind,original in [('size',original_size),('density',original_density)]:
        override=re.search(r'Override .*?: (\S+)',original);run('shell','wm',kind,override.group(1) if override else 'reset')
    for key,value in settings.items():run('shell','settings','put','system',key,value)
    if args.release_apk: run('install','-r',str(args.release_apk))
    launch()
print('Asistente, persistencia, perfiles y averías verificados; datos restaurados.',flush=True)
