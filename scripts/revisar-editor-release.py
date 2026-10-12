#!/usr/bin/env python3
"""Verifica escalas pequeñas y 24 lecturas con el APK release. Restaura datos y deja release instalado."""
import argparse
import json
import pathlib
import re
import subprocess
import time
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--serial', default='emulator-5554')
args = parser.parse_args()
if not args.serial.startswith('emulator-'): raise SystemExit('Solo emuladores de desarrollo')
ADB = [str(pathlib.Path.home()/'Library/Android/sdk/platform-tools/adb'), '-s', args.serial]
PACKAGE = 'com.stradaobd.dash'
DEBUG = ROOT/'app/build/outputs/apk/debug/app-debug.apk'
RELEASE = ROOT/'app/build/outputs/apk/release/app-release.apk'
OUT = ROOT/'build/editor-expanded/release'
OUT.mkdir(parents=True, exist_ok=True)

def adb(*cmd, data=None):
    return subprocess.check_output(ADB+list(cmd), input=data, stderr=subprocess.STDOUT, timeout=40)

def stop(): adb('shell','am','force-stop',PACKAGE)

def start():
    adb('shell','am','start','-W','-f','0x10008000','-n',PACKAGE+'/.MainActivity');time.sleep(1)

def prefs(name, values):
    root=ET.Element('map')
    for key,value in values.items():
        if isinstance(value,bool): ET.SubElement(root,'boolean',name=key,value=str(value).lower())
        elif isinstance(value,int): ET.SubElement(root,'int',name=key,value=str(value))
        else: ET.SubElement(root,'string',name=key).text=str(value)
    adb('shell',"run-as "+PACKAGE+" sh -c 'cat > shared_prefs/"+name+".xml'",data=ET.tostring(root))

def state():
    adb('shell','uiautomator','dump','/sdcard/torque-editor-release.xml')
    return ET.fromstring(adb('shell','cat','/sdcard/torque-editor-release.xml'))

def tap(label):
    n=next(n for n in state().iter('node') if label in (n.get('text'),n.get('content-desc')))
    x1,y1,x2,y2=map(int,re.findall(r'\d+',n.get('bounds')))
    adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));time.sleep(.3)

def capture(name):
    tree=state();(OUT/(name+'.xml')).write_bytes(ET.tostring(tree))
    (OUT/(name+'.png')).write_bytes(adb('exec-out','screencap','-p'))
    print(name+': OK',flush=True);return tree

def fixture(dense):
    count=24 if dense else 1
    values={'dash_tabs_count':1,'dash_tabs_selected':'release-editor','dash_tab_0_id':'release-editor',
        'dash_tab_0_name':'24 métricas' if dense else 'Voltaje preciso','dash_tab_0_grid':'4x6' if dense else '1x1',
        'dash_tab_0_density':'COMPACT' if dense else 'COMFORTABLE','dash_tab_0_slot_count':count}
    examples=[('SAE_0142_Voltaje','Voltaje','Voltaje ECU'),('SAE_010C_RPM','RPM','Revoluciones'),
        ('SAE_010D_Velocidad','Velocidad','Velocidad'),('SAE_0105_Temp Liq','Temp Liq','Refrigerante')]
    for i in range(count):
        pid,short,title=examples[i%len(examples)]
        for key,value in {'index':i,'id':'release-widget-'+str(i),'pid_id':pid,'pid':short,'title':title,
            'type':'ANALOG_GAUGE','size':'STANDARD','color':'#00E5FF',
            'decimals':3 if i%4==0 else -1}.items():values[f'dash_tab_0_slot_{i}_'+key]=value
        if i%4==0:
            values[f'dash_tab_0_slot_{i}_scale_min']='13.6';values[f'dash_tab_0_slot_{i}_scale_max']='14.4'
    return values

adb('install','-r',str(DEBUG));stop()
backup=adb('exec-out','run-as',PACKAGE,'tar','-cf','-','shared_prefs')
(OUT/'original-preferences.tar').write_bytes(backup)
try:
    adb('shell','run-as',PACKAGE,'rm','-rf','shared_prefs');adb('shell','run-as',PACKAGE,'mkdir','shared_prefs')
    prefs('obd_connection',{'simulation_enabled':True,'polling_mode':'VISIBLE'})
    for dense in [False,True]:
        if dense:stop();adb('install','-r',str(DEBUG))
        prefs('torque_obd_prefs',fixture(dense));adb('install','-r',str(RELEASE));start();tap('A Medida')
        if dense:tap('Pantalla completa')
        time.sleep(2);tree=capture('24-readings' if dense else 'small-scale')
        assert any('SIMULACIÓN' in n.get('text','') for n in tree.iter('node'))
        assert any(re.fullmatch(r'13\.\d{3}',n.get('text','')) for n in tree.iter('node'))
        slots=[n for n in tree.iter('node') if re.fullmatch(r'Ranura \d+',n.get('content-desc',''))]
        assert len(slots)==(24 if dense else 1)
        if dense:
            assert next(n for n in tree.iter('node') if n.get('content-desc')=='Indicadores del dashboard').get('scrollable')!='true'
            tap('Salir de pantalla completa')
    # Un vínculo antiguo sin ID y sin sensor en la biblioteca debe seguir sin
    # resolver al guardar su apariencia; nunca se sustituye por el primer PID.
    stop();adb('install','-r',str(DEBUG))
    missing=fixture(False);missing['dash_tab_0_slot_0_pid_id']='';missing['dash_tab_0_slot_0_pid']='SENSOR_AUSENTE'
    missing['dash_tab_0_slot_0_title']='Sensor ausente'
    prefs('torque_obd_prefs',missing);adb('install','-r',str(RELEASE));start();tap('A Medida')
    tap('Editar dashboard');tap('Editar indicador 1');tap('Editar indicador')
    tree=capture('missing-sensor-editor')
    assert any('Sensor no encontrado' in n.get('text','') for n in tree.iter('node'))
    tap('Guardar indicador');tree=capture('missing-sensor-saved')
    assert any(n.get('text')=='Sensor no encontrado' for n in tree.iter('node'))
    stop();adb('install','-r',str(DEBUG))
    saved=ET.fromstring(adb('shell','run-as',PACKAGE,'cat','shared_prefs/torque_obd_prefs.xml'))
    values={n.get('name'):n.text if n.tag=='string' else n.get('value') for n in saved}
    assert not values['dash_tab_0_slot_0_pid_id'] and values['dash_tab_0_slot_0_pid']=='SENSOR_AUSENTE'
    assert values['dash_tab_0_slot_0_id']=='release-widget-0'
    (OUT/'result.json').write_text(json.dumps({'passed':True,'checks':['release-small-scale','release-custom-decimals','release-24-simulated-readings','release-fullscreen-exit','missing-legacy-binding-preserved-on-edit']},indent=2))
finally:
    stop();adb('install','-r',str(DEBUG));stop()
    adb('shell','run-as',PACKAGE,'rm','-rf','shared_prefs');adb('shell','run-as',PACKAGE,'tar','-xf','-',data=backup)
    adb('install','-r',str(RELEASE));start()
    print('Preferencias restauradas; release instalado.',flush=True)
