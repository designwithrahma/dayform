#!/usr/bin/env python3
"""Offline, non-build checks; not a substitute for Android compilation/tests."""
from pathlib import Path
from xml.etree import ElementTree
import re, sys, tomllib
root = Path(__file__).resolve().parents[1]
errors = []
required = [
    'README.md','LICENSE','CHANGELOG.md','CONTRIBUTING.md','SECURITY.md',
    'CODE_OF_CONDUCT.md','build.gradle.kts','settings.gradle.kts','gradlew',
    'gradlew.bat','gradle/wrapper/gradle-wrapper.jar',
    'gradle/wrapper/gradle-wrapper.properties','gradle/libs.versions.toml',
    'app/build.gradle.kts','app/src/main/AndroidManifest.xml',
    'app/schemas/com.example.data.local.DayformDatabase/1.json',
    'app/schemas/com.example.data.local.DayformDatabase/2.json',
    '.github/workflows/android-ci.yml','.github/workflows/release.yml',
]
for path in required:
    if not (root / path).is_file(): errors.append(f'Missing: {path}')
for p in root.rglob('*'):
    rel = p.relative_to(root)
    if 'public' in rel.parts or p.name == 'metadata.json':
        errors.append(f'Unwanted export asset: {rel}')
    if p.is_file() and p.suffix.lower() in {'.apk','.aab','.jks','.keystore','.zip'}:
        errors.append(f'Binary/secret should not be in repository: {rel}')
    if p.is_file() and p.stat().st_size == 0: errors.append(f'Empty file: {rel}')
try:
    gradle = (root/'app/build.gradle.kts').read_text()
    for token in ['applicationId = "com.designwithrahma.dayform"','versionName = "1.0.0"', 'versionCode = 1']:
        if token not in gradle:errors.append(f'Missing compatibility/version setting: {token}')
    tomllib.loads((root/'gradle/libs.versions.toml').read_text())
    manifest=ElementTree.parse(root/'app/src/main/AndroidManifest.xml').getroot()
    permissions=[x.attrib.get('{http://schemas.android.com/apk/res/android}name') for x in manifest.findall('uses-permission')]
    if 'android.permission.INTERNET' in permissions:errors.append('Unexpected INTERNET permission; update privacy docs')
except Exception as exc:errors.append(f'Configuration parse failed: {exc}')
for p in (root/'docs/screenshots').glob('*.png'):
    with p.open('rb') as f:
        if f.read(8) != b'\x89PNG\r\n\x1a\n':errors.append(f'Invalid screenshot PNG: {p.name}')
for md in root.rglob('*.md'):
    content=md.read_text()
    for link in re.findall(r'(?<!!)\[[^\]]+\]\(([^)]+)\)',content):
        if link.startswith(('http://','https://','#','mailto:','<')):continue
        if not (md.parent/link.split('#')[0]).exists():errors.append(f'Broken link: {md.relative_to(root)} -> {link}')
if errors:
    print('FAILED',len(errors))
    for error in errors:print(' -',error)
    sys.exit(1)
print(f'PASS: {len(required)} required files, XML/TOML, images and Markdown links')
print('NOTE: These checks do not compile Android source or verify APK release signing.')
