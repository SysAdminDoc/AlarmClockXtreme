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


@dataclass(frozen=True, order=True)
class GradleTarget:
    project: str
    configuration: str

    @property
    def dependency_task(self) -> str:
        return f"{self.project}:dependencies"

    @property
    def label(self) -> str:
        return f"{self.project}:{self.configuration}"


DEFAULT_TARGETS: tuple[GradleTarget, ...] = (
    GradleTarget(":app", "playReleaseRuntimeClasspath"),
    GradleTarget(":app", "fdroidReleaseRuntimeClasspath"),
    GradleTarget(":wear", "releaseRuntimeClasspath"),
)


@dataclass(frozen=True)
class ResolvedAdvisory:
    dependency: Dependency
    vulnerability_id: str
    reason: str


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--configuration",
        help=(
            "Gradle configuration to resolve before querying OSV. Defaults to "
            "playReleaseRuntimeClasspath when --project is supplied without a "
            "configuration. When omitted with no --project, all release runtime "
            "graphs are audited."
        ),
    )
    parser.add_argument(
        "--project",
        help=(
            "Gradle project path that owns --configuration. Defaults to :app "
            "when --configuration is supplied without a project."
        ),
    )
    parser.add_argument(
        "--target",
        action="append",
        help=(
            "Gradle target in PROJECT:CONFIGURATION form, for example "
            ":wear:releaseRuntimeClasspath. Repeat to audit multiple targets. "
            "When no target/project/configuration is supplied, the release gate "
            "audits Play, F-Droid, and Wear release runtime classpaths."
        ),
    )
    parser.add_argument(
        "--no-fail",
        action="store_true",
        help="Print vulnerabilities but exit 0.",
    )
    return parser.parse_args()


def parse_target_spec(spec: str) -> GradleTarget:
    project, separator, configuration = spec.rpartition(":")
    if not separator or not project or not configuration:
        raise ValueError(
            f"Invalid target '{spec}'. Use PROJECT:CONFIGURATION, e.g. "
            ":app:playReleaseRuntimeClasspath."
        )
    if not project.startswith(":"):
        project = f":{project}"
    return GradleTarget(project, configuration)


def targets_from_args(args: argparse.Namespace) -> tuple[GradleTarget, ...]:
    if args.target:
        if args.project or args.configuration:
            raise ValueError("Use either --target or --project/--configuration, not both.")
        return tuple(parse_target_spec(target) for target in args.target)

    if args.project or args.configuration:
        return (
            GradleTarget(
                project=args.project or ":app",
                configuration=args.configuration or "playReleaseRuntimeClasspath",
            ),
        )

    return DEFAULT_TARGETS


def gradle_command() -> list[str]:
    if os.name == "nt":
        return ["gradlew.bat"]
    return ["./gradlew"]


def resolved_dependencies(project: str, configuration: str) -> set[Dependency]:
    command = gradle_command() + [
        GradleTarget(project, configuration).dependency_task,
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


def numeric_version_parts(version: str) -> tuple[int, ...]:
    parts = [part for part in re.split(r"[^0-9]+", version) if part]
    return tuple(int(part) for part in parts)


def version_at_least(version: str, minimum: str) -> bool:
    current = numeric_version_parts(version)
    floor = numeric_version_parts(minimum)
    width = max(len(current), len(floor))
    current += (0,) * (width - len(current))
    floor += (0,) * (width - len(floor))
    return current >= floor


def is_jackson_54515_patched(version: str) -> bool:
    parts = numeric_version_parts(version)
    if len(parts) < 2 or parts[0] != 2:
        return False

    minor = parts[1]
    if minor <= 18:
        return version_at_least(version, "2.18.9")
    if 19 <= minor <= 21:
        return version_at_least(version, "2.21.5")
    if minor == 22:
        return version_at_least(version, "2.22.1")
    return False


def resolved_advisory_override(
    dependency: Dependency,
    vulnerability: dict,
) -> ResolvedAdvisory | None:
    vulnerability_id = str(vulnerability.get("id") or "")
    if (
        dependency.name == "com.fasterxml.jackson.core:jackson-databind"
        and vulnerability_id == "GHSA-5jmj-h7xm-6q6v"
        and is_jackson_54515_patched(dependency.version)
    ):
        return ResolvedAdvisory(
            dependency=dependency,
            vulnerability_id=vulnerability_id,
            reason=(
                "upstream advisory marks this Jackson release line fixed; "
                "OSV range metadata has not emitted fixed events"
            ),
        )
    return None


def query_osv(
    dependencies: list[Dependency],
) -> tuple[dict[Dependency, list[dict]], list[ResolvedAdvisory]]:
    findings: dict[Dependency, list[dict]] = {}
    resolved_overrides: list[ResolvedAdvisory] = []
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
            unresolved: list[dict] = []
            for vulnerability in vulnerabilities:
                override = resolved_advisory_override(dependency, vulnerability)
                if override is not None:
                    resolved_overrides.append(override)
                else:
                    unresolved.append(vulnerability)
            if unresolved:
                findings[dependency] = unresolved
    return findings, resolved_overrides


def affected_target_labels(
    dependency: Dependency,
    target_dependencies: dict[GradleTarget, set[Dependency]],
) -> list[str]:
    return [
        target.label
        for target, dependencies in sorted(target_dependencies.items())
        if dependency in dependencies
    ]


def vulnerability_ids(vulnerabilities: list[dict]) -> str:
    return ", ".join(
        str(vulnerability.get("id") or "<unknown>") for vulnerability in vulnerabilities
    )


def main() -> int:
    args = parse_args()
    repo_root = Path.cwd()
    if not (repo_root / "settings.gradle.kts").exists():
        print("Run from the repository root.", file=sys.stderr)
        return 2

    try:
        targets = targets_from_args(args)
    except ValueError as exc:
        print(str(exc), file=sys.stderr)
        return 2

    target_dependencies: dict[GradleTarget, set[Dependency]] = {}
    all_dependencies: set[Dependency] = set()
    for target in targets:
        try:
            dependencies = resolved_dependencies(target.project, target.configuration)
        except subprocess.CalledProcessError as exc:
            print(f"Gradle dependency resolution failed for {target.label}.", file=sys.stderr)
            print(exc.stdout, file=sys.stderr)
            return 2
        target_dependencies[target] = dependencies
        all_dependencies.update(dependencies)
        print(f"Resolved {len(dependencies)} Maven dependencies from {target.label}.")

    dependencies = sorted(all_dependencies)
    print(
        f"Querying OSV for {len(dependencies)} unique Maven dependencies across "
        f"{len(targets)} runtime graph(s)."
    )

    try:
        findings, resolved_overrides = query_osv(dependencies)
    except RuntimeError as exc:
        print(str(exc), file=sys.stderr)
        return 2

    for override in resolved_overrides:
        print(
            "OSV resolved advisory override: "
            f"{override.dependency.name}:{override.dependency.version} "
            f"{override.vulnerability_id} ({override.reason})."
        )

    if not findings:
        print("OSV: no vulnerabilities reported for resolved dependencies.")
        return 0

    print("OSV vulnerabilities found:")
    for dependency, vulnerabilities in sorted(findings.items()):
        ids = vulnerability_ids(vulnerabilities)
        affected = ", ".join(affected_target_labels(dependency, target_dependencies))
        print(f"- {dependency.name}:{dependency.version} -> {ids} [{affected}]")

    return 0 if args.no_fail else 1


if __name__ == "__main__":
    raise SystemExit(main())
