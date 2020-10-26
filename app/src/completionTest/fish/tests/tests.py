#! /usr/bin/env python3

import subprocess
import unittest


class FishCompletionTests(unittest.TestCase):
    def __init__(self, methodName):
        super().__init__(methodName)
        self.maxDiff = None

    def test_option_completion(self):
        results = self.run_completions_for("./batect -", "/app/bin")
        self.assertEqual(results, [
            "--cache-type",
            "--clean",
            "--config-file",
            "--config-var",
            "--config-vars-file",
            "--disable-ports",
            "--docker-cert-path",
            "--docker-host",
            "--docker-tls",
            "--docker-tls-ca-cert",
            "--docker-tls-cert",
            "--docker-tls-key",
            "--docker-tls-verify",
            "--enable-buildkit",
            "--help",
            "--linux-cache-init-image",
            "--list-tasks",
            "--log-file",
            "--no-cleanup",
            "--no-cleanup-after-failure",
            "--no-cleanup-after-success",
            "--no-color",
            "--no-proxy-vars",
            "--no-telemetry",
            "--no-update-notification",
            "--no-wrapper-cache-cleanup",
            "--output",
            "--override-image",
            "--permanently-disable-telemetry",
            "--permanently-enable-telemetry",
            "--skip-prerequisites",
            "--upgrade",
            "--use-network",
            "--version",
            "-T",
            "-f",
            "-o",
        ])

    def test_single_use_flag_completion(self):
        results = self.run_completions_for("./batect --list-tasks -", "/app/bin")
        self.assertNotIn('--list-tasks', results)
        self.assertNotIn('-T', results)

    def test_multi_value_option_completion(self):
        results = self.run_completions_for("./batect --config-var=SOME_OPTION=some-value -", "/app/bin")
        self.assertIn("--config-var", results)

    def test_enum_option_completion(self):
        results = self.run_completions_for("./batect --output=", "/app/bin")
        self.assertEqual(results, ["--output=all", "--output=fancy", "--output=quiet", "--output=simple"])

    def test_completion_after_task_name(self):
        batect_results = self.run_completions_for("./batect my-task -- ls -", "/app/bin")
        system_results = self.run_completions_for("ls -", "/app/bin")
        self.assertEqual(batect_results, system_results)

    def run_completions_for(self, input, working_directory):
        stdout = self.run_fish_command('complete -C"{}"'.format(input), working_directory)

        return sorted(stdout.splitlines())

    def run_fish_command(self, command, working_directory):
        command_line = ["fish", "--private", "--command", command]

        result = subprocess.run(
            command_line,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            cwd=working_directory,
            text=True,
            encoding='utf-8'
        )

        self.assertEqual(result.stderr, '')
        self.assertEqual(result.returncode, 0)

        return result.stdout


if __name__ == '__main__':
    unittest.main()