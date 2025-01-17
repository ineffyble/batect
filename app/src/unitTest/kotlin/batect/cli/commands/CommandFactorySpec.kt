/*
    Copyright 2017-2022 Charles Korn.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

package batect.cli.commands

import batect.cli.CommandLineOptions
import batect.cli.commands.completion.GenerateShellTabCompletionScriptCommand
import batect.cli.commands.completion.GenerateShellTabCompletionTaskInformationCommand
import batect.cli.commands.completion.Shell
import batect.testutils.given
import batect.testutils.on
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.isA
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.mockito.kotlin.mock
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object CommandFactorySpec : Spek({
    describe("a command factory") {
        val factory = CommandFactory()
        val kodein = DI.direct {
            bind<CleanupCachesCommand>() with instance(mock())
            bind<DisableTelemetryCommand>() with instance(mock())
            bind<EnableTelemetryCommand>() with instance(mock())
            bind<GenerateShellTabCompletionScriptCommand>() with instance(mock())
            bind<GenerateShellTabCompletionTaskInformationCommand>() with instance(mock())
            bind<HelpCommand>() with instance(mock())
            bind<ListTasksCommand>() with instance(mock())
            bind<RunTaskCommand>() with instance(mock())
            bind<UpgradeCommand>() with instance(mock())
            bind<VersionInfoCommand>() with instance(mock())
        }

        given("a set of options with the 'show help' flag set") {
            val options = CommandLineOptions(showHelp = true)
            val command = factory.createCommand(options, kodein)

            on("creating the command") {
                it("returns a help command") {
                    assertThat(command, isA<HelpCommand>())
                }
            }
        }

        given("a set of options with the 'show version info' flag set") {
            val options = CommandLineOptions(showVersionInfo = true)
            val command = factory.createCommand(options, kodein)

            on("creating the command") {
                it("returns a version info command") {
                    assertThat(command, isA<VersionInfoCommand>())
                }
            }
        }

        given("a set of options with the 'list tasks' flag set") {
            val options = CommandLineOptions(listTasks = true)
            val command = factory.createCommand(options, kodein)

            on("creating the command") {
                it("returns a list tasks command") {
                    assertThat(command, isA<ListTasksCommand>())
                }
            }
        }

        given("a set of options with the 'upgrade' flag set") {
            val options = CommandLineOptions(runUpgrade = true)
            val command = factory.createCommand(options, kodein)

            on("creating the command") {
                it("returns a upgrade command") {
                    assertThat(command, isA<UpgradeCommand>())
                }
            }
        }

        given("a set of options with the 'cleanup' flag set") {
            val options = CommandLineOptions(runCleanup = true)
            val command = factory.createCommand(options, kodein)

            on("creating the command") {
                it("returns a cleanup command") {
                    assertThat(command, isA<CleanupCachesCommand>())
                }
            }
        }

        given("a set of options with a specific cache to clean provided") {
            val options = CommandLineOptions(cleanCaches = setOf("some-cache"))
            val command = factory.createCommand(options, kodein)

            on("creating the command") {
                it("returns a cleanup command") {
                    assertThat(command, isA<CleanupCachesCommand>())
                }
            }
        }

        given("a set of options with the 'disable telemetry' flag set") {
            val options = CommandLineOptions(permanentlyDisableTelemetry = true)
            val command = factory.createCommand(options, kodein)

            on("creating the command") {
                it("returns a 'disable telemetry' command") {
                    assertThat(command, isA<DisableTelemetryCommand>())
                }
            }
        }

        given("a set of options with the 'enable telemetry' flag set") {
            val options = CommandLineOptions(permanentlyEnableTelemetry = true)
            val command = factory.createCommand(options, kodein)

            on("creating the command") {
                it("returns a 'enable telemetry' command") {
                    assertThat(command, isA<EnableTelemetryCommand>())
                }
            }
        }

        given("a set of options with the 'generate shell tab completion script' flag set") {
            val options = CommandLineOptions(generateShellTabCompletionScript = Shell.Fish)
            val command = factory.createCommand(options, kodein)

            on("creating the command") {
                it("returns a 'generate shell tab completion script' command") {
                    assertThat(command, isA<GenerateShellTabCompletionScriptCommand>())
                }
            }
        }

        given("a set of options with the 'generate shell tab completion task information' flag set") {
            val options = CommandLineOptions(generateShellTabCompletionTaskInformation = Shell.Fish)
            val command = factory.createCommand(options, kodein)

            on("creating the command") {
                it("returns a 'generate shell tab completion task information' command") {
                    assertThat(command, isA<GenerateShellTabCompletionTaskInformationCommand>())
                }
            }
        }
        given("a set of options with no special flags set") {
            val options = CommandLineOptions()
            val command = factory.createCommand(options, kodein)

            on("creating the command") {
                it("returns a run task command") {
                    assertThat(command, isA<RunTaskCommand>())
                }
            }
        }
    }
})
