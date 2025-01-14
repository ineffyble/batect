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

import batect.config.ExpressionEvaluationException
import batect.docker.DockerContainer
import batect.docker.DockerContainerCreationSpecFactory
import batect.dockerclient.ContainerCreationFailedException
import batect.dockerclient.DockerClient
import batect.dockerclient.ImageReference
import batect.dockerclient.NetworkReference
import batect.execution.RunAsCurrentUserConfigurationException
import batect.execution.RunAsCurrentUserConfigurationProvider
import batect.execution.VolumeMountResolutionException
import batect.execution.VolumeMountResolver
import batect.execution.model.events.ContainerCreatedEvent
import batect.execution.model.events.ContainerCreationFailedEvent
import batect.execution.model.events.TaskEventSink
import batect.execution.model.steps.CreateContainerStep
import batect.logging.Logger
import batect.ui.containerio.ContainerIOStreamingOptions
import kotlinx.coroutines.runBlocking

class CreateContainerStepRunner(
    private val client: DockerClient,
    private val volumeMountResolver: VolumeMountResolver,
    private val runAsCurrentUserConfigurationProvider: RunAsCurrentUserConfigurationProvider,
    private val creationRequestFactory: DockerContainerCreationSpecFactory,
    private val ioStreamingOptions: ContainerIOStreamingOptions,
    private val logger: Logger
) {
    fun run(step: CreateContainerStep, eventSink: TaskEventSink) {
        val container = step.container

        try {
            val resolvedMounts = volumeMountResolver.resolve(container.volumeMounts)
            val userAndGroup = runAsCurrentUserConfigurationProvider.determineUserAndGroup(container)

            runAsCurrentUserConfigurationProvider.createMissingMountDirectories(resolvedMounts, container)

            val creationSpec = creationRequestFactory.create(
                container,
                ImageReference(step.image.id),
                NetworkReference(step.network.id),
                resolvedMounts,
                userAndGroup,
                ioStreamingOptions.terminalTypeForContainer(container),
                ioStreamingOptions.useTTYForContainer(container),
                ioStreamingOptions.attachStdinForContainer(container)
            )

            runBlocking {
                val containerReference = client.createContainer(creationSpec)
                val dockerContainer = DockerContainer(containerReference, creationSpec.name!!)

                try {
                    runAsCurrentUserConfigurationProvider.applyConfigurationToContainer(container, dockerContainer)
                } finally {
                    // We always want to post that we created the container, even if we didn't finish configuring it -
                    // this ensures that we will clean it up correctly.
                    eventSink.postEvent(ContainerCreatedEvent(container, dockerContainer))
                }
            }
        } catch (e: ExpressionEvaluationException) {
            logger.error {
                message("Evaluating expression failed.")
                exception(e)
            }

            eventSink.postEvent(ContainerCreationFailedEvent(container, e.message ?: ""))
        } catch (e: ContainerCreationFailedException) {
            logger.error {
                message("Creating container failed.")
                exception(e)
            }

            eventSink.postEvent(ContainerCreationFailedEvent(container, e.message ?: ""))
        } catch (e: VolumeMountResolutionException) {
            logger.error {
                message("Resolving volume mounts failed.")
                exception(e)
            }

            eventSink.postEvent(ContainerCreationFailedEvent(container, e.message ?: ""))
        } catch (e: RunAsCurrentUserConfigurationException) {
            logger.error {
                message("Applying 'run as current user' configuration failed.")
                exception(e)
            }

            eventSink.postEvent(ContainerCreationFailedEvent(container, e.message ?: ""))
        }
    }
}
