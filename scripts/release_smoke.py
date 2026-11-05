#!/usr/bin/env python3
"""Black-box checks of the unmodified R8 release APK on an unlocked test device.

Set ANDROID_SERIAL if more than one device is connected. Requires adb on PATH.
No app-private test classes or keep rules are used. Artifacts stay under artifacts/.
"""
import os
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET

OUT = Path('artifacts/release-smoke')
OUT.mkdir(parents=True, exist_ok=True)
ADB = [os.environ.get('ADB', 'adb')]
if os.environ.get('ANDROID_SERIAL'):
    ADB += ['-s', os.environ['ANDROID_SERIAL']]


def adb(*args):
    return subprocess.check_output(ADB + list(args), timeout=30)


def snapshot():
    adb('shell', 'uiautomator', 'dump', '/sdcard/layout-pdf-smoke.xml')
    xml = adb('shell', 'cat', '/sdcard/layout-pdf-smoke.xml')
    (OUT / 'last-ui.xml').write_bytes(xml)
    return list(ET.fromstring(xml).iter('node'))


def find(predicate, timeout=20):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        for node in snapshot():
            if predicate(node.attrib):
                return node.attrib
        time.sleep(0.5)
    raise AssertionError('Expected UI state was not visible; inspect artifacts/release-smoke/last-ui.xml')


def tap(predicate):
    node = find(predicate)
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', node['bounds']))
    assert x2 > x1 and y2 > y1, node
    adb('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2))


def screen(name):
    (OUT / f'{name}.png').write_bytes(adb('exec-out', 'screencap', '-p'))


def resource(name):
    return lambda n: n.get('resource-id', '').endswith('/' + name)


adb('install', '-r', 'sample/build/outputs/apk/release/sample-release.apk')
adb('shell', 'am', 'force-stop', 'com.emon.XMLtoPDF')
adb('shell', 'am', 'start', '-W', '-n', 'com.emon.XMLtoPDF/com.emon.exampleXMLtoPDF.MainActivity')
find(resource('export_invoice'))
screen('release-home')
tap(resource('export_invoice'))
find(lambda n: n.get('text') == 'PDF ready · 1 page(s)')
# PdfRenderer must also successfully open the file, not just the writer callback.
find(lambda n: n.get('text') == 'PAGE 1 / 1')
screen('release-invoice')
tap(resource('export_report'))
find(lambda n: re.fullmatch(r'PDF ready · ([2-9]|[1-9][0-9]+) page\(s\)', n.get('text', '')) is not None)
tap(resource('open_compose'))
tap(lambda n: n.get('text') == 'Export Compose PDF')
find(lambda n: n.get('text') == 'PDF ready · 1 page(s)')
screen('release-compose')
print('PASS: minified XML invoice, PdfRenderer preview, multipage report and Compose export')
