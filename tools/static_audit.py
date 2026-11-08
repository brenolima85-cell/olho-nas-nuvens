#!/usr/bin/env python3
"""Auditoria estática, sem Gradle e sem SDK Android.

Confere identidade do app, ausência de rede e XML bem formado.
Não compila, não gera APK e não fala com a TV.
"""

from __future__ import annotations

import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP_ID = "app.olhonasnuvens.tv"
FORBIDDEN = "com.clarivisionchart"
DISCLAIMER = "Entretenimento apenas. Não faz diagnóstico e não promete melhora da visão."
ANDROID = "{http://schemas.android.com/apk/res/android}"

failures: list[str] = []


def fail(message: str) -> None:
    failures.append(message)


def read(path: Path) -> str:
    if not path.is_file():
        fail(f"arquivo ausente: {path.relative_to(ROOT)}")
        return ""
    return path.read_text(encoding="utf-8")


def png_size(path: Path) -> tuple[int, int]:
    data = path.read_bytes()
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        fail(f"PNG inválido: {path.relative_to(ROOT)}")
        return (0, 0)
    if data[12:16] != b"IHDR":
        fail(f"IHDR ausente: {path.relative_to(ROOT)}")
        return (0, 0)
    width = int.from_bytes(data[16:20], "big")
    height = int.from_bytes(data[20:24], "big")
    return (width, height)


def main() -> int:
    gradle = read(ROOT / "app" / "build.gradle.kts")
    if f'applicationId = "{APP_ID}"' not in gradle:
        fail("applicationId divergente em app/build.gradle.kts")
    if "versionName = \"0.1.0\"" not in gradle or "versionCode = 1" not in gradle:
        fail("versão esperada 0.1.0 / versionCode 1")
    if "minSdk = 21" not in gradle or "targetSdk = 34" not in gradle:
        fail("minSdk 21 ou targetSdk 34 ausente")

    binary = {".png", ".jar", ".webp"}
    for path in (ROOT / "app").rglob("*"):
        if not path.is_file() or path.suffix.lower() in binary:
            continue
        if FORBIDDEN in path.read_text(encoding="utf-8", errors="replace"):
            fail(f"applicationId alheio encontrado em {path.relative_to(ROOT)}")
    for path in (ROOT / "build.gradle.kts", ROOT / "settings.gradle.kts"):
        if FORBIDDEN in read(path):
            fail(f"applicationId alheio encontrado em {path.name}")

    manifest_path = ROOT / "app" / "src" / "main" / "AndroidManifest.xml"
    manifest_text = read(manifest_path)
    if "uses-permission" in manifest_text or "INTERNET" in manifest_text:
        fail("manifesto não deve pedir permissão nem INTERNET")
    if "android.intent.category.LEANBACK_LAUNCHER" not in manifest_text:
        fail("LEANBACK_LAUNCHER ausente")
    try:
        root = ET.parse(manifest_path).getroot()
    except ET.ParseError as error:
        fail(f"manifesto XML inválido: {error}")
        root = None
    if root is not None:
        touch = [
            node
            for node in root.findall("uses-feature")
            if node.attrib.get(ANDROID + "name") == "android.hardware.touchscreen"
        ]
        if not touch or touch[0].attrib.get(ANDROID + "required") != "false":
            fail("touchscreen precisa ser opcional")
        gamepad = [
            node
            for node in root.findall("uses-feature")
            if node.attrib.get(ANDROID + "name") == "android.hardware.gamepad"
        ]
        if not gamepad or gamepad[0].attrib.get(ANDROID + "required") != "false":
            fail("gamepad precisa ser opcional")

    for path in (ROOT / "app" / "src" / "main").rglob("*.xml"):
        try:
            ET.parse(path)
        except ET.ParseError as error:
            fail(f"XML inválido em {path.relative_to(ROOT)}: {error}")

    strings = read(ROOT / "app" / "src" / "main" / "res" / "values" / "strings.xml")
    if DISCLAIMER not in strings or "Olho nas Nuvens" not in strings:
        fail("strings.xml sem nome ou aviso de entretenimento")

    copy = read(ROOT / "app" / "src" / "main" / "java" / "app" / "olhonasnuvens" / "tv" / "game" / "Copy.kt")
    if f'"{DISCLAIMER}"' not in copy:
        fail("Copy.DISCLAIMER diferente de strings.xml")
    if "marca provisória" not in copy or "Clínica de Olhos" not in copy:
        fail("marca provisória ausente em Copy.kt")

    pure_roots = [
        ROOT / "app" / "src" / "main" / "java" / "app" / "olhonasnuvens" / "tv" / "game",
        ROOT / "app" / "src" / "main" / "java" / "app" / "olhonasnuvens" / "tv" / "input",
        ROOT / "app" / "src" / "test",
    ]
    for folder in pure_roots:
        for path in folder.rglob("*.kt"):
            text = path.read_text(encoding="utf-8")
            if "import android." in text or "import java.net." in text:
                fail(f"import proibido em {path.relative_to(ROOT)}")
    board = read(
        ROOT / "app" / "src" / "main" / "java" / "app" / "olhonasnuvens" / "tv" / "persist" / "BestScoreBoard.kt"
    )
    if "import android." in board:
        fail("BestScoreBoard deve permanecer sem Android")

    banned = ("okhttp", "firebase", "WebView", "com.google.android.gms", "exoplayer")
    for path in (ROOT / "app" / "src").rglob("*.kt"):
        text = path.read_text(encoding="utf-8").lower()
        for token in banned:
            if token.lower() in text:
                fail(f"{token} encontrado em {path.relative_to(ROOT)}")

    banner = ROOT / "app" / "src" / "main" / "res" / "drawable-xhdpi" / "tv_banner.png"
    if not banner.is_file():
        fail("banner ausente")
    else:
        width, height = png_size(banner)
        if (width, height) != (320, 180):
            fail(f"banner deveria ser 320x180 e é {width}x{height}")
    for folder, size in {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192,
    }.items():
        icon = ROOT / "app" / "src" / "main" / "res" / folder / "ic_launcher.png"
        if not icon.is_file():
            fail(f"ícone ausente: {folder}")
            continue
        width, height = png_size(icon)
        if (width, height) != (size, size):
            fail(f"{folder} deveria ser {size}px e é {width}x{height}")

    gradlew = ROOT / "gradlew"
    if not gradlew.is_file() or not (gradlew.stat().st_mode & 0o111):
        fail("gradlew ausente ou sem permissão de execução")

    if failures:
        print("FALHOU")
        for item in failures:
            print(f"- {item}")
        return 1
    print("auditoria estática ok")
    return 0


if __name__ == "__main__":
    sys.exit(main())
