/*
    Copyright 2017-2022 Charles Korn.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

package batect.docker

import batect.docker.client.DockerContainerType
import batect.dockerclient.BuilderVersion
import batect.primitives.Version

sealed class DockerConnectivityCheckResult {
    data class Succeeded(val containerType: DockerContainerType, val dockerVersion: Version, val builderVersion: BuilderVersion, val experimentalFeaturesEnabled: Boolean) : DockerConnectivityCheckResult()

    data class Failed(val message: String) : DockerConnectivityCheckResult()
}