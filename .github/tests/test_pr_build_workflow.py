"""
Tests for .github/workflows/pr-build.yml

Validates the MySQL service container and build environment variable configuration
introduced in this PR. Run with:
    python3 -m unittest .github/tests/test_pr_build_workflow.py -v
"""

import os
import unittest
import yaml


WORKFLOW_PATH = os.path.join(
    os.path.dirname(__file__), "..", "workflows", "pr-build.yml"
)


def load_workflow():
    with open(WORKFLOW_PATH, "r") as f:
        return yaml.safe_load(f)


class TestWorkflowYamlValidity(unittest.TestCase):
    """The YAML file must be syntactically valid and load without errors."""

    def test_workflow_file_exists(self):
        self.assertTrue(os.path.exists(WORKFLOW_PATH), "pr-build.yml must exist")

    def test_yaml_is_parseable(self):
        workflow = load_workflow()
        self.assertIsNotNone(workflow)
        self.assertIsInstance(workflow, dict)

    def test_workflow_has_required_top_level_keys(self):
        workflow = load_workflow()
        # PyYAML parses the bare `on` keyword as Python True (YAML boolean).
        # Accept either the boolean True key or the string "on".
        has_on = True in workflow or "on" in workflow
        self.assertTrue(has_on, "Top-level 'on' trigger key is missing")
        self.assertIn("jobs", workflow, "Top-level key 'jobs' is missing")

    def test_build_job_exists(self):
        workflow = load_workflow()
        self.assertIn("Build", workflow["jobs"], "Build job must be defined")


class TestMysqlServiceConfiguration(unittest.TestCase):
    """MySQL service container added in this PR must be fully configured."""

    def setUp(self):
        workflow = load_workflow()
        self.services = workflow["jobs"]["Build"].get("services", {})
        self.mysql = self.services.get("mysql", {})

    def test_mysql_service_is_present(self):
        self.assertIn("mysql", self.services, "mysql service must be declared")

    def test_mysql_image_is_8(self):
        self.assertEqual(
            self.mysql.get("image"),
            "mysql:8.0",
            "MySQL image must be mysql:8.0",
        )

    def test_mysql_root_password_set(self):
        env = self.mysql.get("env", {})
        self.assertIn("MYSQL_ROOT_PASSWORD", env)
        self.assertTrue(env["MYSQL_ROOT_PASSWORD"], "MYSQL_ROOT_PASSWORD must not be empty")

    def test_mysql_database_name(self):
        env = self.mysql.get("env", {})
        self.assertEqual(env.get("MYSQL_DATABASE"), "haksamo")

    def test_mysql_user(self):
        env = self.mysql.get("env", {})
        self.assertEqual(env.get("MYSQL_USER"), "haksamo")

    def test_mysql_password_set(self):
        env = self.mysql.get("env", {})
        self.assertIn("MYSQL_PASSWORD", env)
        self.assertTrue(env["MYSQL_PASSWORD"], "MYSQL_PASSWORD must not be empty")

    def test_mysql_port_mapping(self):
        ports = self.mysql.get("ports", [])
        self.assertIn("3306:3306", ports, "Port 3306:3306 must be mapped for MySQL")

    def test_mysql_has_health_check_options(self):
        options = self.mysql.get("options", "")
        self.assertIn(
            "mysqladmin ping",
            options,
            "--health-cmd must use 'mysqladmin ping'",
        )

    def test_health_check_interval_is_5s(self):
        options = self.mysql.get("options", "")
        self.assertIn(
            "--health-interval=5s",
            options,
            "Health check interval must be 5s",
        )

    def test_health_check_timeout_is_5s(self):
        options = self.mysql.get("options", "")
        self.assertIn(
            "--health-timeout=5s",
            options,
            "Health check timeout must be 5s",
        )

    def test_health_check_retries_is_10(self):
        options = self.mysql.get("options", "")
        self.assertIn(
            "--health-retries=10",
            options,
            "Health check retries must be 10 (regression: previous config had fewer retries)",
        )

    def test_health_retries_not_less_than_10(self):
        """Regression: retries must be >= 10 so slow CI runners can spin up MySQL."""
        options = self.mysql.get("options", "")
        import re
        match = re.search(r"--health-retries=(\d+)", options)
        self.assertIsNotNone(match, "--health-retries must be present")
        retries = int(match.group(1))
        self.assertGreaterEqual(retries, 10, "health-retries must be >= 10")


class TestBuildStepEnvironmentVariables(unittest.TestCase):
    """Build step must expose all DB + JWT env vars required by the Spring app."""

    def setUp(self):
        workflow = load_workflow()
        steps = workflow["jobs"]["Build"].get("steps", [])
        self.build_step = next(
            (s for s in steps if "clean build" in s.get("run", "")),
            None,
        )
        self.env = self.build_step.get("env", {}) if self.build_step else {}

    def test_build_step_found(self):
        self.assertIsNotNone(self.build_step, "Gradle clean build step must exist")

    def test_spring_profiles_active_is_dev(self):
        self.assertEqual(
            self.env.get("SPRING_PROFILES_ACTIVE"),
            "dev",
            "SPRING_PROFILES_ACTIVE must be 'dev' in CI",
        )

    def test_db_host_is_loopback(self):
        """Service containers are reachable via 127.0.0.1, not 'localhost', in GHA."""
        self.assertEqual(
            self.env.get("DB_HOST"),
            "127.0.0.1",
            "DB_HOST must be 127.0.0.1 (not 'localhost') to reach the service container",
        )

    def test_db_host_is_not_localhost_string(self):
        """Negative: using 'localhost' instead of '127.0.0.1' breaks service connectivity."""
        self.assertNotEqual(
            self.env.get("DB_HOST"),
            "localhost",
            "DB_HOST must not be 'localhost'; use '127.0.0.1' for GitHub Actions service containers",
        )

    def test_db_port_is_3306(self):
        self.assertEqual(
            self.env.get("DB_PORT"),
            3306,
            "DB_PORT must be 3306 to match the mapped MySQL service port",
        )

    def test_db_name_matches_mysql_service(self):
        workflow = load_workflow()
        mysql_db = (
            workflow["jobs"]["Build"]
            .get("services", {})
            .get("mysql", {})
            .get("env", {})
            .get("MYSQL_DATABASE")
        )
        self.assertEqual(
            self.env.get("DB_NAME"),
            mysql_db,
            "DB_NAME in build step must match MYSQL_DATABASE in service",
        )

    def test_db_username_matches_mysql_service(self):
        workflow = load_workflow()
        mysql_user = (
            workflow["jobs"]["Build"]
            .get("services", {})
            .get("mysql", {})
            .get("env", {})
            .get("MYSQL_USER")
        )
        self.assertEqual(
            self.env.get("DB_USERNAME"),
            mysql_user,
            "DB_USERNAME in build step must match MYSQL_USER in service",
        )

    def test_db_password_matches_mysql_service(self):
        workflow = load_workflow()
        mysql_pass = (
            workflow["jobs"]["Build"]
            .get("services", {})
            .get("mysql", {})
            .get("env", {})
            .get("MYSQL_PASSWORD")
        )
        self.assertEqual(
            self.env.get("DB_PASSWORD"),
            mysql_pass,
            "DB_PASSWORD in build step must match MYSQL_PASSWORD in service",
        )

    def test_jwt_secret_is_present(self):
        self.assertIn("JWT_SECRET", self.env, "JWT_SECRET must be set in the build step")

    def test_jwt_secret_minimum_length(self):
        """JWT_SECRET must be at least 32 characters for HS256 signing."""
        secret = str(self.env.get("JWT_SECRET", ""))
        self.assertGreaterEqual(
            len(secret),
            32,
            f"JWT_SECRET must be >= 32 characters (got {len(secret)})",
        )

    def test_jwt_secret_boundary_exactly_meets_minimum(self):
        """Boundary: secret length must be exactly >= 32, not 31."""
        secret = str(self.env.get("JWT_SECRET", ""))
        self.assertGreater(
            len(secret),
            31,
            "JWT_SECRET length must be greater than 31 characters",
        )

    def test_all_required_env_vars_present(self):
        required = {
            "SPRING_PROFILES_ACTIVE",
            "DB_HOST",
            "DB_PORT",
            "DB_NAME",
            "DB_USERNAME",
            "DB_PASSWORD",
            "JWT_SECRET",
        }
        missing = required - set(self.env.keys())
        self.assertFalse(missing, f"Missing required env vars: {missing}")


class TestWorkflowTriggerAndPermissions(unittest.TestCase):
    """Workflow trigger and job runner must not have changed from baseline."""

    def setUp(self):
        self.workflow = load_workflow()
        # PyYAML parses the bare `on` keyword as Python True (YAML boolean).
        self.on_config = self.workflow.get(True) or self.workflow.get("on") or {}

    def test_runs_on_ubuntu_latest(self):
        runner = self.workflow["jobs"]["Build"].get("runs-on")
        self.assertEqual(runner, "ubuntu-latest")

    def test_triggered_on_pull_request_to_dev(self):
        pr_config = self.on_config.get("pull_request", {})
        branches = pr_config.get("branches", [])
        self.assertIn("dev", branches)

    def test_pr_event_types(self):
        pr_types = self.on_config.get("pull_request", {}).get("types", [])
        for expected in ("opened", "synchronize", "reopened"):
            self.assertIn(expected, pr_types)

    def test_permissions_contents_read(self):
        perms = self.workflow.get("permissions", {})
        self.assertEqual(perms.get("contents"), "read")


if __name__ == "__main__":
    unittest.main(verbosity=2)
