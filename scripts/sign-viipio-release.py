#!/usr/bin/env python3
"""Sign Viipio's release APK with its private, long-lived key outside Git.

Create/back up ~/.config/viipio-keyboard/{release.jks,keystore.pass} securely
before first use. Do not print credentials or check them into the repository.
"""
from pathlib import Path
import os
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
KEY_DIR = Path.home() / '.config/viipio-keyboard'
KEY = KEY_DIR / 'release.jks'
PASSWORD_FILE = KEY_DIR / 'keystore.pass'

for path in (KEY_DIR, KEY, PASSWORD_FILE):
    if path.is_symlink() or not path.exists() or (path.stat().st_mode & 0o077):
        raise SystemExit(f'Missing or insecure Viipio signer path: {path}')

password = PASSWORD_FILE.read_text(encoding='utf-8').strip()
if not password:
    raise SystemExit('Viipio signer password is empty')
if not os.environ.get('ANDROID_HOME') and not os.environ.get('ANDROID_SDK_ROOT'):
    raise SystemExit('Set ANDROID_HOME or ANDROID_SDK_ROOT to a local Android SDK')

env = os.environ.copy()
env.update({
    'GRADLE_GUARD_XMX': '1536m',
    'GRADLE_GUARD_METASPACE': '512m',
    'GRADLE_GUARD_RETRIES': '0',
    'RELEASE_KEYSTORE': str(KEY),
    'RELEASE_KEYSTORE_PASSWORD': password,
    'RELEASE_KEY_ALIAS': 'viipio',
    'RELEASE_KEY_PASSWORD': password,
})
logfile = ROOT / 'build/tooling/viipio-release-build.log'
logfile.parent.mkdir(parents=True, exist_ok=True)
with logfile.open('wb') as output:
    result = subprocess.run(
        ['bash', 'scripts/gradle-guard.sh', 'assembleRelease',
         '-x', 'normalizeApkPaths', '--offline'],
        cwd=ROOT, env=env, stdout=output, stderr=subprocess.STDOUT,
        timeout=1200, check=False,
    )
print(f'Viipio release build exit={result.returncode}; log={logfile}')
sys.exit(result.returncode)
