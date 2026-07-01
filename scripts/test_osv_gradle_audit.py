import argparse
import unittest

from scripts import osv_gradle_audit as audit


class OsvGradleAuditTest(unittest.TestCase):
    def test_default_targets_cover_release_runtime_graphs(self):
        args = argparse.Namespace(target=None, project=None, configuration=None)

        self.assertEqual(
            audit.DEFAULT_TARGETS,
            audit.targets_from_args(args),
        )

    def test_configuration_only_defaults_to_app_project(self):
        args = argparse.Namespace(
            target=None,
            project=None,
            configuration="fdroidReleaseRuntimeClasspath",
        )

        self.assertEqual(
            (audit.GradleTarget(":app", "fdroidReleaseRuntimeClasspath"),),
            audit.targets_from_args(args),
        )

    def test_parse_target_spec_keeps_nested_project_path(self):
        self.assertEqual(
            audit.GradleTarget(":wear", "releaseRuntimeClasspath"),
            audit.parse_target_spec(":wear:releaseRuntimeClasspath"),
        )

    def test_parse_target_spec_rejects_missing_project(self):
        with self.assertRaises(ValueError):
            audit.parse_target_spec("releaseRuntimeClasspath")

    def test_target_cannot_be_mixed_with_project_configuration(self):
        args = argparse.Namespace(
            target=[":wear:releaseRuntimeClasspath"],
            project=":app",
            configuration=None,
        )

        with self.assertRaises(ValueError):
            audit.targets_from_args(args)

    def test_affected_target_labels_identifies_each_runtime_graph(self):
        dependency = audit.Dependency("androidx.room", "room-runtime", "2.6.1")
        target_dependencies = {
            audit.GradleTarget(":app", "playReleaseRuntimeClasspath"): {dependency},
            audit.GradleTarget(":app", "fdroidReleaseRuntimeClasspath"): set(),
            audit.GradleTarget(":wear", "releaseRuntimeClasspath"): {dependency},
        }

        self.assertEqual(
            [
                ":app:playReleaseRuntimeClasspath",
                ":wear:releaseRuntimeClasspath",
            ],
            audit.affected_target_labels(dependency, target_dependencies),
        )

    def test_vulnerability_ids_handles_missing_id(self):
        self.assertEqual(
            "GHSA-test, <unknown>",
            audit.vulnerability_ids([{"id": "GHSA-test"}, {}]),
        )

    def test_jackson_54515_override_only_suppresses_patched_2_18_line(self):
        dependency = audit.Dependency(
            "com.fasterxml.jackson.core",
            "jackson-databind",
            "2.18.9",
        )

        override = audit.resolved_advisory_override(
            dependency,
            {"id": "GHSA-5jmj-h7xm-6q6v"},
        )

        self.assertIsNotNone(override)
        self.assertFalse(audit.is_jackson_54515_patched("2.18.8"))

    def test_jackson_54515_override_does_not_mask_affected_later_lines(self):
        self.assertFalse(audit.is_jackson_54515_patched("2.19.4"))
        self.assertFalse(audit.is_jackson_54515_patched("2.21.4"))
        self.assertTrue(audit.is_jackson_54515_patched("2.21.5"))
        self.assertTrue(audit.is_jackson_54515_patched("2.22.1"))


if __name__ == "__main__":
    unittest.main()
