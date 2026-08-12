#!/usr/bin/env python3
"""Fail when tracked AlarmClockXtreme release declarations drift apart."""

from __future__ import annotations

import re
import sys
from dataclasses import dataclass
from pathlib import Path


VERSION_PATTERN = r"[0-9]+\.[0-9]+\.[0-9]+"
SCHEMA_DIRECTORY = Path(
    "app/schemas/com.sysadmindoc.alarmclock.data.local.AlarmDatabase"
)


class VerificationFailure(RuntimeError):
    def __init__(self, errors: list[str]) -> None:
        self.errors = errors
        super().__init__("\n".join(errors))


@dataclass(frozen=True)
class ReleaseSnapshot:
    version_name: str
    version_code: int
    database_version: int
    backup_version: int


def read_text(root: Path, relative: str | Path) -> str:
    path = root / relative
    try:
        return path.read_text(encoding="utf-8")
    except (OSError, UnicodeError) as error:
        raise VerificationFailure([f"{relative}: cannot read file: {error}"]) from error


def extract_one(text: str, pattern: str, source: str, flags: int = 0) -> str:
    matches = re.findall(pattern, text, flags)
    if len(matches) != 1:
        raise VerificationFailure(
            [f"{source}: expected one matching declaration, found {len(matches)}"]
        )
    value = matches[0]
    return value if isinstance(value, str) else value[0]


def verify_release_metadata(root: Path) -> ReleaseSnapshot:
    root = root.resolve()
    errors: list[str] = []

    app_gradle = read_text(root, "app/build.gradle.kts")
    version_name = extract_one(
        app_gradle,
        rf'^\s*versionName\s*=\s*"({VERSION_PATTERN})"\s*$',
        "app/build.gradle.kts versionName",
        re.MULTILINE,
    )
    version_code = int(
        extract_one(
            app_gradle,
            r"^\s*versionCode\s*=\s*([0-9]+)\s*$",
            "app/build.gradle.kts versionCode",
            re.MULTILINE,
        )
    )

    def expect(source: str, actual: str | int, expected: str | int) -> None:
        if actual != expected:
            errors.append(f"{source}: expected {expected}, found {actual}")

    app_header = extract_one(
        app_gradle,
        rf"^// AlarmClockXtreme v({VERSION_PATTERN})$",
        "app/build.gradle.kts header",
        re.MULTILINE,
    )
    expect("app/build.gradle.kts header", app_header, version_name)

    root_gradle = read_text(root, "build.gradle.kts")
    root_header = extract_one(
        root_gradle,
        rf"^// AlarmClockXtreme v({VERSION_PATTERN})$",
        "build.gradle.kts header",
        re.MULTILINE,
    )
    expect("build.gradle.kts header", root_header, version_name)

    wear_gradle = read_text(root, "wear/build.gradle.kts")
    wear_name = extract_one(
        wear_gradle,
        rf'^\s*versionName\s*=\s*"({VERSION_PATTERN})"\s*$',
        "wear/build.gradle.kts versionName",
        re.MULTILINE,
    )
    wear_code = int(
        extract_one(
            wear_gradle,
            r"^\s*versionCode\s*=\s*([0-9]+)\s*$",
            "wear/build.gradle.kts versionCode",
            re.MULTILINE,
        )
    )
    expect("wear/build.gradle.kts versionName", wear_name, version_name)
    expect("wear/build.gradle.kts versionCode", wear_code, version_code)

    readme = read_text(root, "README.md")
    badge_version = extract_one(
        readme,
        rf"shields\.io/badge/version-({VERSION_PATTERN})-",
        "README.md version badge",
    )
    expect("README.md version badge", badge_version, version_name)
    artifact_versions = re.findall(
        rf"AlarmClockXtreme-v({VERSION_PATTERN})-play-release\.apk", readme
    )
    if not artifact_versions:
        errors.append("README.md: no Play release artifact filename found")
    for artifact_version in artifact_versions:
        expect("README.md Play release artifact", artifact_version, version_name)

    changelog = read_text(root, "CHANGELOG.md")
    changelog_versions = re.findall(
        rf"^## \[({VERSION_PATTERN})\]",
        changelog,
        re.MULTILINE,
    )
    if not changelog_versions:
        errors.append("CHANGELOG.md: no semantic release heading found")
    else:
        expect("CHANGELOG.md latest release", changelog_versions[0], version_name)

    verifier = read_text(root, "scripts/verify_api37_release.py")
    verifier_name = extract_one(
        verifier,
        rf'^EXPECTED_VERSION_NAME\s*=\s*"({VERSION_PATTERN})"$',
        "scripts/verify_api37_release.py versionName",
        re.MULTILINE,
    )
    verifier_code = int(
        extract_one(
            verifier,
            r'^EXPECTED_VERSION_CODE\s*=\s*"([0-9]+)"$',
            "scripts/verify_api37_release.py versionCode",
            re.MULTILINE,
        )
    )
    expect("scripts/verify_api37_release.py versionName", verifier_name, version_name)
    expect("scripts/verify_api37_release.py versionCode", verifier_code, version_code)

    for relative in (
        "metadata/com.sysadmindoc.alarmclock.yml",
        "metadata/en-US/fdroid.yml",
    ):
        metadata = read_text(root, relative)
        build_names = re.findall(
            rf"^\s*-\s+versionName:\s*({VERSION_PATTERN})\s*$", metadata, re.MULTILINE
        )
        build_codes = re.findall(
            r"^\s+versionCode:\s*([0-9]+)\s*$", metadata, re.MULTILINE
        )
        commits = re.findall(
            rf"^\s+commit:\s*v({VERSION_PATTERN})\s*$", metadata, re.MULTILINE
        )
        if not build_names or not build_codes or not commits:
            errors.append(f"{relative}: latest build version/code/commit is incomplete")
        else:
            expect(f"{relative} latest versionName", build_names[-1], version_name)
            expect(f"{relative} latest versionCode", int(build_codes[-1]), version_code)
            expect(f"{relative} latest commit", commits[-1], version_name)
        current_name = extract_one(
            metadata,
            rf"^CurrentVersion:\s*({VERSION_PATTERN})\s*$",
            f"{relative} CurrentVersion",
            re.MULTILINE,
        )
        current_code = int(
            extract_one(
                metadata,
                r"^CurrentVersionCode:\s*([0-9]+)\s*$",
                f"{relative} CurrentVersionCode",
                re.MULTILINE,
            )
        )
        expect(f"{relative} CurrentVersion", current_name, version_name)
        expect(f"{relative} CurrentVersionCode", current_code, version_code)

    database_source = read_text(
        root,
        "app/src/main/java/com/sysadmindoc/alarmclock/data/local/AlarmDatabase.kt",
    )
    database_block = extract_one(
        database_source,
        r"@Database\((.*?)\)\s*@TypeConverters",
        "AlarmDatabase.kt @Database",
        re.DOTALL,
    )
    database_version = int(
        extract_one(
            database_block,
            r"\bversion\s*=\s*([0-9]+)",
            "AlarmDatabase.kt schema version",
        )
    )
    migration_test = read_text(
        root,
        "app/src/androidTest/java/com/sysadmindoc/alarmclock/data/local/AlarmDatabaseMigrationTest.kt",
    )
    tested_database_version = int(
        extract_one(
            migration_test,
            r"LATEST_SCHEMA_VERSION\s*=\s*([0-9]+)",
            "AlarmDatabaseMigrationTest.kt LATEST_SCHEMA_VERSION",
        )
    )
    expect(
        "AlarmDatabaseMigrationTest.kt LATEST_SCHEMA_VERSION",
        tested_database_version,
        database_version,
    )
    schema_dir = root / SCHEMA_DIRECTORY
    schema_versions = sorted(
        int(path.stem)
        for path in schema_dir.glob("*.json")
        if path.stem.isdigit()
    )
    if not schema_versions:
        errors.append(f"{SCHEMA_DIRECTORY}: no exported Room schemas found")
    else:
        expect(f"{SCHEMA_DIRECTORY} latest schema", schema_versions[-1], database_version)

    backup_source = read_text(
        root,
        "app/src/main/java/com/sysadmindoc/alarmclock/data/backup/BackupManager.kt",
    )
    backup_version = int(
        extract_one(
            backup_source,
            r"data class BackupData\(\s*val version:\s*Int\s*=\s*([0-9]+)",
            "BackupData default version",
            re.DOTALL,
        )
    )
    max_backup_version = int(
        extract_one(
            backup_source,
            r"MAX_SUPPORTED_BACKUP_VERSION\s*=\s*([0-9]+)",
            "BackupManager MAX_SUPPORTED_BACKUP_VERSION",
        )
    )
    expect(
        "BackupManager MAX_SUPPORTED_BACKUP_VERSION",
        max_backup_version,
        backup_version,
    )

    if errors:
        raise VerificationFailure(errors)
    return ReleaseSnapshot(version_name, version_code, database_version, backup_version)


def main() -> int:
    root = Path(__file__).resolve().parents[1]
    try:
        snapshot = verify_release_metadata(root)
    except VerificationFailure as failure:
        print("Release metadata verification failed:", file=sys.stderr)
        for error in failure.errors:
            print(f"- {error}", file=sys.stderr)
        return 1
    print(
        "Release metadata verified: "
        f"v{snapshot.version_name} ({snapshot.version_code}), "
        f"Room v{snapshot.database_version}, backup v{snapshot.backup_version}."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
