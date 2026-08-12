import tempfile
import unittest
from pathlib import Path

from scripts.verify_release_metadata import VerificationFailure, verify_release_metadata


class ReleaseMetadataVerificationTest(unittest.TestCase):
    def make_fixture(self) -> Path:
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        root = Path(temp.name)
        files = {
            "build.gradle.kts": "// AlarmClockXtreme v1.2.3\n",
            "app/build.gradle.kts": (
                "// AlarmClockXtreme v1.2.3\n"
                "versionCode = 42\n"
                'versionName = "1.2.3"\n'
            ),
            "wear/build.gradle.kts": 'versionCode = 42\nversionName = "1.2.3"\n',
            "README.md": (
                "https://img.shields.io/badge/version-1.2.3-blue\n"
                "AlarmClockXtreme-v1.2.3-play-release.apk\n"
            ),
            "CHANGELOG.md": "## [1.2.3] - 2026-01-01\n",
            "scripts/verify_api37_release.py": (
                'EXPECTED_VERSION_CODE = "42"\n'
                'EXPECTED_VERSION_NAME = "1.2.3"\n'
            ),
            "metadata/com.sysadmindoc.alarmclock.yml": self.metadata(),
            "metadata/en-US/fdroid.yml": self.metadata(),
            "app/src/main/java/com/sysadmindoc/alarmclock/data/local/AlarmDatabase.kt": (
                "@Database(entities = [], version = 7, exportSchema = true)\n"
                "@TypeConverters\n"
            ),
            "app/src/androidTest/java/com/sysadmindoc/alarmclock/data/local/AlarmDatabaseMigrationTest.kt": (
                "LATEST_SCHEMA_VERSION = 7\n"
            ),
            "app/src/main/java/com/sysadmindoc/alarmclock/data/backup/BackupManager.kt": (
                "data class BackupData(\n    val version: Int = 5\n)\n"
                "const val MAX_SUPPORTED_BACKUP_VERSION = 5\n"
            ),
            "app/schemas/com.sysadmindoc.alarmclock.data.local.AlarmDatabase/7.json": "{}\n",
        }
        for relative, content in files.items():
            path = root / relative
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(content, encoding="utf-8")
        return root

    @staticmethod
    def metadata() -> str:
        return (
            "Builds:\n"
            "  - versionName: 1.2.3\n"
            "    versionCode: 42\n"
            "    commit: v1.2.3\n"
            "CurrentVersion: 1.2.3\n"
            "CurrentVersionCode: 42\n"
        )

    def replace(self, root: Path, relative: str, old: str, new: str) -> None:
        path = root / relative
        path.write_text(path.read_text(encoding="utf-8").replace(old, new), encoding="utf-8")

    def test_consistent_release_metadata_passes(self):
        snapshot = verify_release_metadata(self.make_fixture())

        self.assertEqual("1.2.3", snapshot.version_name)
        self.assertEqual(42, snapshot.version_code)
        self.assertEqual(7, snapshot.database_version)
        self.assertEqual(5, snapshot.backup_version)

    def test_each_release_boundary_fails_when_stale(self):
        mismatches = (
            ("wear/build.gradle.kts", "1.2.3", "1.2.2", "wear/build.gradle.kts"),
            ("README.md", "1.2.3", "1.2.2", "README.md"),
            ("CHANGELOG.md", "## [1.2.3]", "## [1.2.2]", "CHANGELOG.md"),
            (
                "scripts/verify_api37_release.py",
                'EXPECTED_VERSION_CODE = "42"',
                'EXPECTED_VERSION_CODE = "41"',
                "verify_api37_release.py",
            ),
            (
                "metadata/com.sysadmindoc.alarmclock.yml",
                "CurrentVersionCode: 42",
                "CurrentVersionCode: 41",
                "CurrentVersionCode",
            ),
            (
                "app/src/androidTest/java/com/sysadmindoc/alarmclock/data/local/AlarmDatabaseMigrationTest.kt",
                "LATEST_SCHEMA_VERSION = 7",
                "LATEST_SCHEMA_VERSION = 6",
                "LATEST_SCHEMA_VERSION",
            ),
            (
                "app/src/main/java/com/sysadmindoc/alarmclock/data/backup/BackupManager.kt",
                "MAX_SUPPORTED_BACKUP_VERSION = 5",
                "MAX_SUPPORTED_BACKUP_VERSION = 4",
                "MAX_SUPPORTED_BACKUP_VERSION",
            ),
        )
        for relative, old, new, expected_message in mismatches:
            with self.subTest(relative=relative):
                root = self.make_fixture()
                self.replace(root, relative, old, new)
                with self.assertRaises(VerificationFailure) as raised:
                    verify_release_metadata(root)
                self.assertIn(expected_message, str(raised.exception))

    def test_missing_current_room_schema_fails(self):
        root = self.make_fixture()
        schema = root / "app/schemas/com.sysadmindoc.alarmclock.data.local.AlarmDatabase/7.json"
        schema.unlink()
        older = schema.with_name("6.json")
        older.write_text("{}\n", encoding="utf-8")

        with self.assertRaises(VerificationFailure) as raised:
            verify_release_metadata(root)

        self.assertIn("latest schema", str(raised.exception))


if __name__ == "__main__":
    unittest.main()
