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

@file:UseSerializers(
    ContainerNameOnlySerializer::class,
    PathSerializer::class,
    ImageReferenceSerializer::class,
    NetworkReferenceSerializer::class
)

package batect.execution.model.events

import batect.config.Container
import batect.config.PullImage
import batect.config.SetupCommand
import batect.docker.AggregatedImageBuildProgress
import batect.docker.AggregatedImagePullProgress
import batect.docker.DockerContainer
import batect.docker.ImageReferenceSerializer
import batect.docker.NetworkReferenceSerializer
import batect.dockerclient.ImageReference
import batect.dockerclient.NetworkReference
import batect.execution.model.steps.TaskStep
import batect.logging.ContainerNameOnlySerializer
import batect.logging.LogMessageBuilder
import batect.logging.PathSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.builtins.SetSerializer

@Serializable
sealed class TaskEvent(
    @Transient val isInformationalEvent: Boolean = false
)

@Serializable
data class ContainerBecameHealthyEvent(val container: Container) : TaskEvent()

@Serializable
data class ContainerBecameReadyEvent(val container: Container) : TaskEvent()

@Serializable
data class ContainerCreatedEvent(val container: Container, val dockerContainer: DockerContainer) : TaskEvent()

@Serializable
data class ContainerRemovedEvent(val container: Container) : TaskEvent()

@Serializable
data class ContainerStartedEvent(val container: Container) : TaskEvent()

@Serializable
data class ContainerStoppedEvent(val container: Container) : TaskEvent()

@Serializable
data class ImageBuildProgressEvent(val container: Container, val buildProgress: AggregatedImageBuildProgress) : TaskEvent(isInformationalEvent = true)

@Serializable
data class ImageBuiltEvent(val container: Container, val image: ImageReference) : TaskEvent()

@Serializable
data class ImagePulledEvent(val source: PullImage, val image: ImageReference) : TaskEvent()

@Serializable
data class ImagePullProgressEvent(val source: PullImage, val progress: AggregatedImagePullProgress) : TaskEvent(isInformationalEvent = true)

@Serializable
data class RunningContainerExitedEvent(val container: Container, val exitCode: Long) : TaskEvent()

@Serializable
data class RunningSetupCommandEvent(val container: Container, val command: SetupCommand, val commandIndex: Int) : TaskEvent()

@Serializable
data class SetupCommandsCompletedEvent(val container: Container) : TaskEvent()

@Serializable
data class StepStartingEvent(val step: TaskStep) : TaskEvent(true)

@Serializable
sealed class TaskNetworkReadyEvent : TaskEvent() {
    abstract val network: NetworkReference
}

@Serializable
data class TaskNetworkCreatedEvent(override val network: NetworkReference) : TaskNetworkReadyEvent()

@Serializable
data class CustomTaskNetworkCheckedEvent(override val network: NetworkReference) : TaskNetworkReadyEvent()

@Serializable
object TaskNetworkDeletedEvent : TaskEvent()

sealed class TaskFailedEvent : TaskEvent()

@Serializable
data class ExecutionFailedEvent(val message: String) : TaskFailedEvent()

@Serializable
data class TaskNetworkCreationFailedEvent(val message: String) : TaskFailedEvent()

@Serializable
data class CustomTaskNetworkCheckFailedEvent(val networkIdentifier: String, val message: String) : TaskFailedEvent()

@Serializable
data class ImageBuildFailedEvent(val container: Container, val message: String) : TaskFailedEvent()

@Serializable
data class ImagePullFailedEvent(val source: PullImage, val message: String) : TaskFailedEvent()

@Serializable
data class ContainerCreationFailedEvent(val container: Container, val message: String) : TaskFailedEvent()

@Serializable
data class ContainerDidNotBecomeHealthyEvent(val container: Container, val message: String) : TaskFailedEvent()

@Serializable
data class ContainerRunFailedEvent(val container: Container, val message: String) : TaskFailedEvent()

@Serializable
data class ContainerStopFailedEvent(val container: Container, val message: String) : TaskFailedEvent()

@Serializable
data class ContainerRemovalFailedEvent(val container: Container, val message: String) : TaskFailedEvent()

@Serializable
data class TaskNetworkDeletionFailedEvent(val message: String) : TaskFailedEvent()

@Serializable
object UserInterruptedExecutionEvent : TaskFailedEvent()

@Serializable
data class SetupCommandExecutionErrorEvent(val container: Container, val command: SetupCommand, val message: String) : TaskFailedEvent()

@Serializable
data class SetupCommandFailedEvent(val container: Container, val command: SetupCommand, val exitCode: Long, val output: String) : TaskFailedEvent()

fun LogMessageBuilder.data(key: String, value: TaskEvent) = this.data(key, value, TaskEvent.serializer())
fun LogMessageBuilder.data(key: String, value: Set<TaskEvent>) = this.data(key, value, SetSerializer(TaskEvent.serializer()))
