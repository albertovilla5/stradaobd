#!/usr/bin/env python3
"""Comprueba las operaciones y el intercambio JSON del editor en un emulador. Restaura sus preferencias."""
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
p.add_argument('--apk', type=pathlib.Path, default=ROOT/'app/build/outputs/apk/debug/app-debug.apk')
args = p.parse_args()
if not args.serial.startswith('emulator-'): raise SystemExit('Solo emuladores de desarrollo')
ADB = [str(pathlib.Path.home()/'Library/Android/sdk/platform-tools/adb'), '-s', args.serial]
PACKAGE = 'com.torqueobd2.dash'
OUT = ROOT/'build/editor-expanded/functions'
OUT.mkdir(parents=True,exist_ok=True)


def adb(*cmd, data=None):
    return subprocess.check_output(ADB+list(cmd),input=data,stderr=subprocess.STDOUT,timeout=40)


def stop(): adb('shell','am','force-stop',PACKAGE)


def start():
    adb('shell','am','start','-W','-f','0x10008000','-n',PACKAGE+'/.MainActivity');time.sleep(.8)


def prefs(name,values):
    root=ET.Element('map')
    for key,value in values.items():
        if isinstance(value,bool): ET.SubElement(root,'boolean',name=key,value=str(value).lower())
        elif isinstance(value,int): ET.SubElement(root,'int',name=key,value=str(value))
        else: ET.SubElement(root,'string',name=key).text=str(value)
    adb('shell','run-as',PACKAGE,'sh','-c',"'cat > shared_prefs/"+name+".xml'",data=ET.tostring(root))


def state():
    adb('shell','uiautomator','dump','/sdcard/torque-editor-functions.xml')
    return ET.fromstring(adb('shell','cat','/sdcard/torque-editor-functions.xml'))


def rect(n):return tuple(map(int,re.findall(r'\d+',n.get('bounds',''))))


def visible(n):
    b=rect(n);return len(b)==4 and b[2]>b[0] and b[3]>b[1]


def node(label,tree=None):
    root=tree if tree is not None else state()
    return next((n for n in root.iter('node') if label in (n.get('text'),n.get('content-desc')) and visible(n)),None)


def tap_node(n):
    x1,y1,x2,y2=rect(n);adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));time.sleep(.25)


def tap(label):
    n=node(label);assert n is not None, 'No visible: '+label;tap_node(n)


def reach(label):
    for _ in range(20):
        tree=state();n=node(label,tree)
        if n is not None:tap_node(n);return
        scrollers=[n for n in tree.iter('node') if n.get('scrollable')=='true' and visible(n)]
        assert scrollers,'No hay desplazamiento para '+label
        # El scroll exterior del formulario tiene más área que la lista de sensores.
        n=max(scrollers,key=lambda n:(rect(n)[2]-rect(n)[0])*(rect(n)[3]-rect(n)[1]))
        x1,y1,x2,y2=rect(n);x=(x1+x2)//2
        nested=[s for s in scrollers if s is not n and rect(s)[1]>y1+50 and rect(s)[3]<=y2]
        start=min(rect(s)[1] for s in nested)-12 if nested else int(y1+(y2-y1)*.50)
        adb('shell','input','swipe',str(x),str(start),str(x),str(max(y1+4,start-int((y2-y1)*.40))),'500')
    capture('unreachable-'+re.sub(r'[^a-zA-Z0-9]','-',label))
    raise AssertionError('No alcanzable: '+label)


def capture(name):
    tree=state();(OUT/(name+'.xml')).write_bytes(ET.tostring(tree));(OUT/(name+'.png')).write_bytes(adb('exec-out','screencap','-p'))
    print(name+': OK',flush=True);return tree


def stored():
    root=ET.fromstring(adb('shell','run-as',PACKAGE,'cat','shared_prefs/torque_obd_prefs.xml'))
    return {n.get('name'):n.get('value') if n.tag!='string' else n.text for n in root}


def field(label, value):
    tree=state(); parent={child:parent for parent in tree.iter('node') for child in parent}
    lab=node(label,tree);assert lab is not None,label
    container=lab
    while container is not None:
        fields=[n for n in container.iter('node') if n.get('class')=='android.widget.EditText' and visible(n)]
        if fields:break
        container=parent.get(container)
    assert len(fields)==1,(label,fields)
    tap_node(fields[0]);adb('shell','input','keyevent','KEYCODE_MOVE_END',*(['KEYCODE_DEL']*30))
    adb('shell','input','text',value)
    time.sleep(.2)


def tool(label):tap('Herramientas del dashboard');reach(label)


def widget_menu(slot,action):tap('Editar indicador '+str(slot));tap(action)


def choose_file(filename):
    tree=state()
    n=next((n for n in tree.iter('node') if filename in n.get('text','') and visible(n)),None)
    if n is None:
        roots=node('Show roots',tree)
        if roots is not None:tap_node(roots);tap('Downloads')
        tree=state();n=next((n for n in tree.iter('node') if filename in n.get('text','') and visible(n)),None)
    assert n is not None,'Archivo no visible: '+filename
    tap_node(n);time.sleep(.6)


adb('install','-r',str(args.apk));stop()
backup=adb('exec-out','run-as',PACKAGE,'tar','-cf','-','shared_prefs')
(OUT/'original-preferences.tar').write_bytes(backup)
created_files=[]
try:
    adb('shell','run-as',PACKAGE,'rm','-rf','shared_prefs');adb('shell','run-as',PACKAGE,'mkdir','shared_prefs')
    prefs('obd_connection',{'simulation_enabled':True,'polling_mode':'VISIBLE'})
    dashboard={'dash_tabs_count':1,'dash_tabs_selected':'editor-test','dash_tab_0_id':'editor-test','dash_tab_0_name':'Motor',
               'dash_tab_0_grid':'2x2','dash_tab_0_slot_count':3,'dash_tab_0_density':'COMFORTABLE'}
    for i,(pid,pid_id,kind,title) in enumerate([('RPM','SAE_010C_RPM','ANALOG_GAUGE','Tacometro propio'),
        ('Velocidad','SAE_010D_Velocidad','DIGITAL_HUD','Velocidad'),('Voltaje','SAE_0142_Voltaje','HORIZONTAL_BAR','Bateria')]):
        prefix=f'dash_tab_0_slot_{i}_'
        for key,val in {'index':i,'id':'widget-'+str(i),'pid':pid,'pid_id':pid_id,'type':kind,'title':title,'color':'#00E5FF','size':'STANDARD'}.items():dashboard[prefix+key]=val
    prefs('torque_obd_prefs',dashboard);start();tap('A Medida');tap('Editar dashboard');capture('initial')
    # Edición conserva el vínculo y el ID. Decimales y escala deben persistir.
    widget_menu(1,'Editar indicador')
    capture('edit-existing')
    reach('Título del indicador');assert node('Tacometro propio') is not None
    field('Título del indicador','RPM%sprecisas')
    reach('Escala personalizada');tap('3')
    tree=capture('scale-toggle');label=node('Escala personalizada',tree);ly=(rect(label)[1]+rect(label)[3])/2
    switch=min((n for n in tree.iter('node') if n.get('checkable')=='true' and visible(n)),key=lambda n:abs((rect(n)[1]+rect(n)[3])/2-ly))
    if switch.get('checked')=='false':tap_node(switch)
    reach('Mínimo');field('Mínimo','0');reach('Máximo');field('Máximo','9000')
    capture('scale-and-decimals');tap('Guardar indicador');time.sleep(.4)
    saved=stored();assert saved['dash_tab_0_slot_0_id']=='widget-0';assert saved['dash_tab_0_slot_0_pid_id']=='SAE_010C_RPM'
    assert saved['dash_tab_0_slot_0_decimals']=='3',saved['dash_tab_0_slot_0_decimals']
    assert float(saved['dash_tab_0_slot_0_scale_max'])==9000,saved['dash_tab_0_slot_0_scale_max']
    # Movimiento vacío, intercambio ocupado y copia.
    widget_menu(1,'Mover / intercambiar');tap('Destino ranura 4')
    assert stored()['dash_tab_0_slot_2_index']=='3'
    widget_menu(4,'Mover / intercambiar');tap('Destino ranura 2');capture('swapped')
    widget_menu(2,'Duplicar indicador');tap('Destino ranura 1');capture('duplicated-widget')
    # Copiar pestaña conserva configuración sin compartir identidades.
    tool('Duplicar pestaña');capture('duplicated-tab')
    saved=stored();assert saved['dash_tabs_count']=='2';assert saved['dash_tab_0_id']!=saved['dash_tab_1_id']
    assert saved['dash_tab_0_slot_0_id']!=saved['dash_tab_1_slot_0_id']
    tool('Presentación compacta');assert stored()['dash_tab_1_density']=='COMPACT'
    # Archivo exportado se lee y valida fuera de la app.
    tool('Importar / exportar diseños');tap('Exportar todas las pestañas')
    tree=state();filename=next(n.get('text') for n in tree.iter('node') if n.get('class')=='android.widget.EditText')
    created_files.append(filename);tap('SAVE');time.sleep(.5);capture('exported')
    exported=adb('shell','cat','/sdcard/Download/'+filename)
    (OUT/'designs.json').write_bytes(exported);data=json.loads(exported)
    assert data['version']==1 and len(data['dashboards'])==2
    assert data['dashboards'][1]['density']=='COMPACT';assert any(w['decimals']==3 and w['scaleMin']==0 and w['scaleMax']==9000 for w in data['dashboards'][1]['widgets'])
    # Reimportar añade pestañas nuevas tras vista previa; no reemplaza las existentes.
    tap('Abrir archivo de diseños');choose_file(filename);capture('import-preview')
    assert stored()['dash_tabs_count']=='2'
    reach('Importar como pestañas nuevas');time.sleep(.4);tap('Cerrar')
    assert stored()['dash_tabs_count']=='4'
    assert stored()['dash_tab_2_slot_0_decimals']=='3' and float(stored()['dash_tab_2_slot_0_scale_max'])==9000
    capture('imported')
    # Sintaxis, escala inválida y decimales fraccionarios no modifican las pestañas.
    bad_scale=json.loads(exported);bad_scale['dashboards'][0]['widgets'][0]['scaleMax']=0
    bad_decimals=json.loads(exported);bad_decimals['dashboards'][0]['widgets'][0]['decimals']=1.5
    for case,content in [('syntax','bad-json'),('scale',json.dumps(bad_scale)),('decimals',json.dumps(bad_decimals))]:
        # Reutiliza el documento registrado por SAF; archivos escritos por shell
        # pueden tardar en aparecer en el proveedor de Descargas.
        invalid=filename
        adb('shell','sh','-c',"'cat > /sdcard/Download/"+invalid+"'",data=content.encode())
        tool('Importar / exportar diseños');tap('Abrir archivo de diseños');choose_file(invalid)
        tree=capture('invalid-import-'+case);assert any('No se pudo importar' in n.get('text','') for n in tree.iter('node'))
        assert stored()['dash_tabs_count']=='4';tap('Cerrar')
    # Cancelar eliminación conserva pestañas. Confirmar elimina solo la seleccionada.
    tool('Eliminar pestaña');capture('delete-confirm');tap('Cancelar');assert stored()['dash_tabs_count']=='4'
    tool('Eliminar pestaña');tap('Eliminar');assert stored()['dash_tabs_count']=='3'
    # Reiniciar prueba la persistencia. La rejilla pequeña conserva las ranuras ocultas.
    saved=stored();stop();start();tap('A Medida');capture('after-restart')
    assert stored()['dash_tabs_count']=='3'
    assert stored()['dash_tabs_selected']==saved['dash_tabs_selected']
    tap('Editar dashboard');tool('Disposición de rejilla');reach('Matriz 4x6');capture('grid-24')
    tool('Añadir varios indicadores');capture('bulk-dialog');reach('Seleccionar hasta 20');tap('Añadir seleccionados')
    assert stored()[f"dash_tab_{int(stored()['dash_tabs_count'])-1}_slot_count"]=='24'
    capture('bulk-added')
    tap('Terminar edición');tap('Pantalla completa');capture('fullscreen');tap('Salir de pantalla completa')
    (OUT/'result.json').write_text(json.dumps({'passed':True,'checks':['edit-preserves-identity','scale','decimals','move-empty','swap-occupied',
        'copy-widget','copy-tab','density-persistence','json-export','json-import-preview-and-append','invalid-import-keeps-layouts','delete-confirmation',
        'restart-persistence','new-grid-selection','bulk-add-24','fullscreen-exit']},indent=2))
finally:
    stop();adb('shell','run-as',PACKAGE,'rm','-rf','shared_prefs');adb('shell','run-as',PACKAGE,'tar','-xf','-',data=backup)
    for filename in created_files:adb('shell','rm','-f','/sdcard/Download/'+filename)
    start();print('Preferencias originales restauradas.',flush=True)
