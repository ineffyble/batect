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

package batect.ioc

import batect.docker.DockerContainerType
import batect.dockerclient.BuilderVersion
import batect.dockerclient.DockerClient
import org.kodein.di.DirectDI
import org.kodein.di.bind
import org.kodein.di.direct
import org.kodein.di.instance
import org.kodein.di.subDI

class DockerConfigurationKodeinFactory(
    private val baseKodein: DirectDI
) {
    fun create(client: DockerClient, containerType: DockerContainerType, builderVersion: BuilderVersion): DirectDI = subDI(baseKodein.di) {
        bind<DockerClient>() with instance(client)
        bind<DockerContainerType>() with instance(containerType)
        bind<BuilderVersion>() with instance(builderVersion)
        import(dockerConfigurationModule)
    }.direct
}
