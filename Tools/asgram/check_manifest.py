"""Check the merged application manifest before distributing ASGRAM."""
from pathlib import Path
import argparse
import xml.etree.ElementTree as ET

ANDROID = "{http://schemas.android.com/apk/res/android}"


def check(path: Path, release: bool) -> None:
    root = ET.parse(path).getroot()
    permissions = {item.get(ANDROID + "name") for item in root.findall("uses-permission")}
    required = {
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.FOREGROUND_SERVICE_LOCATION",
    }
    missing = required - permissions
    if missing:
        raise ValueError("Missing location permissions: " + ", ".join(sorted(missing)))
    app = root.find("application")
    if app is None:
        raise ValueError("Missing application")
    maps = [item for item in app.findall("meta-data")
            if item.get(ANDROID + "name") == "com.google.android.geo.API_KEY"]
    if len(maps) != 1 or not maps[0].get(ANDROID + "value", "").startswith("AIza"):
        raise ValueError("Missing Maps SDK configuration")
    if app.get(ANDROID + "allowBackup") != "false":
        raise ValueError("ASGRAM account data must not be included in Android backups")
    services = {item.get(ANDROID + "name"): item for item in app.findall("service")}
    location_service = services.get("org.telegram.messenger.LocationSharingService")
    if location_service is None or "location" not in location_service.get(ANDROID + "foregroundServiceType", "").split("|"):
        raise ValueError("Missing foreground location service")
    if release:
        if "android.permission.REQUEST_INSTALL_PACKAGES" in permissions:
            raise ValueError("The Play release must use Play updates")
        for item in app.findall("activity"):
            if "StorePreview" in item.get(ANDROID + "name", ""):
                raise ValueError("Store preview activity leaked into release")
    print(f"PASS: {path.name}: location, Maps, backup and release boundaries")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("manifest", type=Path)
    parser.add_argument("--release", action="store_true")
    args = parser.parse_args()
    check(args.manifest, args.release)
