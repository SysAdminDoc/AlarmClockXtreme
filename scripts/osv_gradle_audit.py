#!/usr/bin/env python3
"""Query OSV for resolved Gradle Maven dependencies.

The generic OSV source scanner did not reliably inspect this Gradle Kotlin DSL
Android project during the 2026-05-17 audit. This script resolves a real Gradle
classpath, extracts Maven coordinates from Gradle's dependency report, and
queries OSV's batch API against the resolved versions.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
import urllib.error
import urllib.request
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable


COORDINATE_RE = re.compile(
    r"(?P<group>[A-Za-z0-9_.-]+):"
    r"(?P<artifact>[A-Za-z0-9_.-]+):"
    r"(?P<declared>[A-Za-z0-9_.+v-]+)"
    r"(?:\s*->\s*(?P<resolved>[A-Za-z0-9_.+v-]+))?"
)


@dataclass(frozen=True, order=True)
class Dependency:
    group: str
    artifact: str
    version: str

    @property
    def name(self) -> str:
        return f"{self.group}:{self.artifact}"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--configuration",
        default="playReleaseRuntimeClasspath",
        help="Gradle configuration to resolve before querying OSV.",
    )
    parser.add_argument(
        "--project",
        default=":app",
        help="Gradle project path that owns the configuration.",
    )
    parser.add_argument(
        "--no-fail",
        action="store_true",
        help="Print vulnerabilities but exit 0.",
    )
    return parser.parse_args()


def gradle_command() -> list[str]:
    if os.name == "nt":
        return ["gradlew.bat"]
    return ["./gradlew"]


def resolved_dependencies(project: str, configuration: str) -> set[Dependency]:
    command = gradle_command() + [
        f"{project}:dependencies",
        "--configuration",
        configuration,
        "--console=plain",
    ]
    result = subprocess.run(
        command,
        check=True,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
    )

    dependencies: set[Dependency] = set()
    for match in COORDINATE_RE.finditer(result.stdout):
        group = match.group("group")
        artifact = match.group("artifact")
        version = match.group("resolved") or match.group("declared")
        version = version.rstrip(".,)")
        if version and not version.startswith("project"):
            dependencies.add(Dependency(group, artifact, version))
    return dependencies


def chunks(items: list[Dependency], size: int) -> Iterable[list[Dependency]]:
    for index in range(0, len(items), size):
        yield items[index : index + size]


def query_osv(dependencies: list[Dependency]) -> dict[Dependency, list[dict]]:
    findings: dict[Dependency, list[dict]] = {}
    for batch in chunks(dependencies, 100):
        payload = {
            "queries": [
                {
                    "version": dependency.version,
                    "package": {
                        "ecosystem": "Maven",
                        "name": dependency.name,
                    },
                }
                for dependency in batch
            ]
        }
        request = urllib.request.Request(
            "https://api.osv.dev/v1/querybatch",
            data=json.dumps(payload).encode("utf-8"),
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        try:
            with urllib.request.urlopen(request, timeout=30) as response:
                data = json.loads(response.read().decode("utf-8"))
        except urllib.error.URLError as exc:
            raise RuntimeError(f"OSV query failed: {exc}") from exc

        for dependency, result in zip(batch, data.get("results", [])):
            vulnerabilities = result.get("vulns") or []
            if vulnerabilities:
                findings[dependency] = vulnerabilities
    return findings


def main() -> int:
    args = parse_args()
    repo_root = Path.cwd()
    if not (repo_root / "settings.gradle.kts").exists():
        print("Run from the repository root.", file=sys.stderr)
        return 2

    dependencies = sorted(resolved_dependencies(args.project, args.configuration))
    print(f"Resolved {len(dependencies)} Maven dependencies from {args.configuration}.")

    findings = query_osv(dependencies)
    if not findings:
        print("OSV: no vulnerabilities reported for resolved dependencies.")
        return 0

    print("OSV vulnerabilities found:")
    for dependency, vulnerabilities in sorted(findings.items()):
        ids = ", ".join(vulnerability["id"] for vulnerability in vulnerabilities)
        print(f"- {dependency.name}:{dependency.version} -> {ids}")

    return 0 if args.no_fail else 1


if __name__ == "__main__":
    raise SystemExit(main())
