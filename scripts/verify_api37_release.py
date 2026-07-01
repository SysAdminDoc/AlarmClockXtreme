#!/usr/bin/env python3
"""Verify Android 17 / 16 KB release readiness for local APKs."""

from __future__ import annotations

import argparse
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path


PACKAGE = "com.sysadmindoc.alarmclock"
DEFAULT_APKS = (
    Path("app/build/outputs/apk/play/release/app-play-release.apk"),
    Path("app/build/outputs/apk/fdroid/release/app-fdroid-release.apk"),
    Path("wear/build/outputs/apk/release/wear-release.apk"),
)
RUNTIME_PERMISSIONS = (
    "android.permission.POST_NOTIFICATIONS",
    "android.permission.ACCESS_LOCAL_NETWORK",
)
READY_PERMISSIONS = (
    "android.permission.USE_EXACT_ALARM",
    "android.permission.POST_PROMOTED_NOTIFICATIONS",
    "android.permission.POST_NOTIFICATIONS",
    "android.permission.ACCESS_LOCAL_NETWORK",
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--device", help="adb serial for an API 37 16 KB emulator/device.")
    parser.add_argument(
        "--fresh-install",
        action="store_true",
        help="Uninstall the phone package before installing the Play release APK.",
    )
    parser.add_argument(
        "--run-test-alarm",
        action="store_true",
        help="Drive onboarding's built-in exact test alarm and dismiss it.",
    )
    parser.add_argument(
        "--apk",
        action="append",
        type=Path,
        help="APK to check with zipalign -P 16. Defaults to Play/F-Droid/Wear release APKs.",
    )
    parser.add_argument(
        "--play-apk",
        type=Path,
        default=DEFAULT_APKS[0],
        help="Phone Play release APK to install for device smoke.",
    )
    return parser.parse_args()


def run(command: list[str], *, check: bool = True) -> subprocess.CompletedProcess[str]:
    result = subprocess.run(
        command,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
    )
    if check and result.returncode != 0:
        raise RuntimeError(
            f"Command failed ({result.returncode}): {' '.join(command)}\n{result.stdout}"
        )
    return result


def android_home() -> Path:
    for env_name in ("ANDROID_HOME", "ANDROID_SDK_ROOT"):
        value = os.environ.get(env_name)
        if value:
            return Path(value)
    local_app_data = os.environ.get("LOCALAPPDATA")
    if local_app_data:
        return Path(local_app_data) / "Android" / "Sdk"
    return Path.home() / "AppData" / "Local" / "Android" / "Sdk"


def executable(path: Path) -> str:
    if os.name == "nt":
        for suffix in (".exe", ".bat", ".cmd"):
            candidate = path.with_suffix(suffix)
            if candidate.exists():
                return str(candidate)
    return str(path)


def latest_build_tools(sdk: Path) -> Path:
    build_tools = sdk / "build-tools"
    versions = sorted((path for path in build_tools.iterdir() if path.is_dir()), reverse=True)
    if not versions:
        raise RuntimeError(f"No Android build-tools installed under {build_tools}")
    return versions[0]


def check_16kb_zipalign(zipalign: str, apks: tuple[Path, ...]) -> None:
    for apk in apks:
        if not apk.exists():
            raise RuntimeError(f"Missing APK: {apk}")
        run([zipalign, "-c", "-P", "16", "-v", "4", str(apk)])
        print(f"PASS 16 KB zipalign: {apk}")


def adb(adb_path: str, serial: str, *args: str, check: bool = True) -> str:
    command = [adb_path, "-s", serial, *args]
    return run(command, check=check).stdout.strip()


def require_contains(text: str, needle: str, label: str) -> None:
    if needle not in text:
        raise RuntimeError(f"Missing {label}: {needle}")
    print(f"PASS {label}: {needle}")


def device_smoke(adb_path: str, serial: str, play_apk: Path, fresh_install: bool) -> None:
    state = adb(adb_path, serial, "get-state")
    if state != "device":
        raise RuntimeError(f"{serial} is not ready: {state}")

    sdk = adb(adb_path, serial, "shell", "getprop", "ro.build.version.sdk")
    page_size = adb(adb_path, serial, "shell", "getconf", "PAGE_SIZE")
    avd_name = adb(adb_path, serial, "shell", "getprop", "ro.boot.qemu.avd_name")
    if sdk != "37":
        raise RuntimeError(f"Expected API 37 device, got API {sdk}")
    if page_size != "16384":
        raise RuntimeError(f"Expected 16 KB page size, got {page_size}")
    print(f"PASS device: {serial} API {sdk}, page size {page_size}, avd={avd_name}")

    if fresh_install:
        adb(adb_path, serial, "uninstall", PACKAGE, check=False)

    run([adb_path, "-s", serial, "install", "-r", "-d", str(play_apk)])
    for permission in RUNTIME_PERMISSIONS:
        adb(adb_path, serial, "shell", "pm", "grant", PACKAGE, permission)

    dump = adb(adb_path, serial, "shell", "dumpsys", "package", PACKAGE)
    require_contains(dump, "versionCode=110", "installed versionCode")
    require_contains(dump, "versionName=1.15.8", "installed versionName")
    for permission in READY_PERMISSIONS:
        pattern = re.compile(rf"{re.escape(permission)}: granted=true")
        if not pattern.search(dump):
            raise RuntimeError(f"Permission is not granted: {permission}")
        print(f"PASS permission granted: {permission}")


def xml_from_dump(raw: str) -> ET.Element:
    start = raw.find("<?xml")
    end = raw.rfind("</hierarchy>")
    if start < 0 or end < 0:
        raise RuntimeError(f"Unable to parse UI hierarchy:\n{raw}")
    return ET.fromstring(raw[start : end + len("</hierarchy>")])


def node_bounds(node: ET.Element) -> tuple[int, int]:
    match = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.attrib.get("bounds", ""))
    if not match:
        raise RuntimeError(f"Node has no usable bounds: {node.attrib}")
    x1, y1, x2, y2 = (int(part) for part in match.groups())
    return ((x1 + x2) // 2, (y1 + y2) // 2)


def dump_ui(adb_path: str, serial: str) -> ET.Element:
    raw = adb(adb_path, serial, "exec-out", "uiautomator", "dump", "/dev/tty")
    return xml_from_dump(raw)


def nodes_with_text(root: ET.Element, text: str) -> list[ET.Element]:
    return [node for node in root.iter("node") if node.attrib.get("text") == text]


def tap_text(adb_path: str, serial: str, root: ET.Element, text: str, *, last: bool = False) -> bool:
    nodes = nodes_with_text(root, text)
    if not nodes:
        return False
    node = nodes[-1] if last else nodes[0]
    x, y = node_bounds(node)
    adb(adb_path, serial, "shell", "input", "tap", str(x), str(y))
    return True


def drive_test_alarm(adb_path: str, serial: str) -> None:
    adb(adb_path, serial, "logcat", "-c")
    adb(adb_path, serial, "shell", "monkey", "-p", PACKAGE, "1")
    ran_alarm = False
    deadline = time.time() + 90
    while time.time() < deadline:
        root = dump_ui(adb_path, serial)
        if nodes_with_text(root, "Test alarm completed"):
            print("PASS test alarm completed")
            crash_log = adb(adb_path, serial, "logcat", "-b", "crash", "-d")
            if "FATAL EXCEPTION" in crash_log:
                raise RuntimeError(f"Crash buffer contains a fatal exception:\n{crash_log}")
            print("PASS crash buffer clean")
            return
        if tap_text(adb_path, serial, root, "Dismiss test alarm"):
            time.sleep(2)
            continue
        if tap_text(adb_path, serial, root, "Run"):
            ran_alarm = True
            time.sleep(15)
            continue
        if tap_text(adb_path, serial, root, "Allow", last=True):
            time.sleep(1)
            continue
        if tap_text(adb_path, serial, root, "Continue", last=True):
            time.sleep(1)
            continue
        if ran_alarm:
            time.sleep(1)
            continue
        raise RuntimeError("Could not find onboarding Continue or test-alarm Run control.")
    raise RuntimeError("Timed out waiting for test alarm completion.")


def main() -> int:
    args = parse_args()
    sdk = android_home()
    build_tools = latest_build_tools(sdk)
    zipalign = executable(build_tools / "zipalign")
    adb_path = executable(sdk / "platform-tools" / "adb")

    apks = tuple(args.apk) if args.apk else DEFAULT_APKS
    check_16kb_zipalign(zipalign, apks)

    if args.device:
        device_smoke(adb_path, args.device, args.play_apk, args.fresh_install)
        if args.run_test_alarm:
            drive_test_alarm(adb_path, args.device)

    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except RuntimeError as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        raise SystemExit(1)
