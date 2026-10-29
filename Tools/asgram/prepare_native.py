#!/usr/bin/env python3
"""Verify and prepare the matching vendor native libraries for ASGRAM."""
import argparse
import hashlib
import json
from pathlib import Path
import sys
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[2]
PROVENANCE = json.loads((Path(__file__).with_name("native-provenance.json")).read_text())

def sha256(path):
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for block in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", type=Path, help="An already downloaded official matching Telegram APK")
    args = parser.parse_args()
    cache = ROOT / ".asgram"
    cache.mkdir(exist_ok=True)
    apk = args.apk or cache / "telegram-base.apk"
    if not apk.exists():
        temporary = cache / "telegram-base.apk.download"
        try:
            urllib.request.urlretrieve(PROVENANCE["source_url"], temporary)
            if sha256(temporary) != PROVENANCE["apk_sha256"]:
                raise ValueError("The official download has changed. Use the exact matching version; do not mix source and native binaries.")
            temporary.replace(apk)
        finally:
            temporary.unlink(missing_ok=True)
    if sha256(apk) != PROVENANCE["apk_sha256"]:
        raise ValueError("APK checksum does not match the pinned release.")
    with zipfile.ZipFile(apk) as archive:
        verified = {}
        for abi, expected in PROVENANCE['native_libraries'].items():
            data = archive.read(f'lib/{abi}/libtmessages.49.so')
            if hashlib.sha256(data).hexdigest() != expected:
                raise ValueError(f'Native library checksum mismatch for {abi}.')
            verified[abi] = data
    for abi, data in verified.items():
        destination = cache / 'nativeLibraries' / abi / 'libtmessages.49.so'
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_bytes(data)
        print(f"Prepared {abi} native library for source {PROVENANCE['source_commit']}.")

if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, KeyError, zipfile.BadZipFile) as error:
        print(f"ASGRAM setup stopped: {error}", file=sys.stderr)
        sys.exit(1)
