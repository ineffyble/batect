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

package batect.execution.model.steps.runners

import batect.cli.CommandLineOptions
import batect.docker.DockerContainerType
import batect.docker.DockerResourceNameGenerator
import batect.dockerclient.DockerClient
import batect.dockerclient.DockerClientException
import batect.dockerclient.NetworkCreationFailedException
import batect.execution.model.events.CustomTaskNetworkCheckFailedEvent
import batect.execution.model.events.CustomTaskNetworkCheckedEvent
import batect.execution.model.events.TaskEventSink
import batect.execution.model.events.TaskNetworkCreatedEvent
import batect.execution.model.events.TaskNetworkCreationFailedEvent
import batect.logging.Logger
import kotlinx.coroutines.runBlocking

class PrepareTaskNetworkStepRunner(
    private val nameGenerator: DockerResourceNameGenerator,
    private val containerType: DockerContainerType,
    private val client: DockerClient,
    private val commandLineOptions: CommandLineOptions,
    private val logger: Logger
) {
    fun run(eventSink: TaskEventSink) {
        if (commandLineOptions.existingNetworkToUse == null) {
            createNewNetwork(eventSink)
        } else {
            checkExistingNetwork(eventSink, commandLineOptions.existingNetworkToUse)
        }
    }

    private fun createNewNetwork(eventSink: TaskEventSink) {
        try {
            val driver = when (containerType) {
                DockerContainerType.Linux -> "bridge"
                DockerContainerType.Windows -> "nat"
            }

            val name = nameGenerator.generateNameFor("network")

            val network = runBlocking {
                client.createNetwork(name, driver)
            }

            eventSink.postEvent(TaskNetworkCreatedEvent(network))
        } catch (e: NetworkCreationFailedException) {
            logger.error {
                message("Creating network failed.")
                exception(e)
            }

            eventSink.postEvent(TaskNetworkCreationFailedEvent(e.message ?: ""))
        }
    }

    private fun checkExistingNetwork(eventSink: TaskEventSink, networkIdentifier: String) {
        try {
            val network = runBlocking {
                client.getNetworkByNameOrID(networkIdentifier)
            }

            if (network == null) {
                eventSink.postEvent(CustomTaskNetworkCheckFailedEvent(networkIdentifier, "The network '$networkIdentifier' does not exist."))
                return
            }

            eventSink.postEvent(CustomTaskNetworkCheckedEvent(network))
        } catch (e: DockerClientException) {
            logger.error {
                message("Checking existing network failed.")
                exception(e)
            }

            eventSink.postEvent(CustomTaskNetworkCheckFailedEvent(networkIdentifier, e.message ?: ""))
        }
    }
}
