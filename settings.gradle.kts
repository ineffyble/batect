/*
    Copyright 2017-2021 Charles Korn.

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

pluginManagement {
    val reckonPluginVersion: String by settings
    val shadowPluginVersion: String by settings

    plugins {
        id("org.ajoberstar.reckon") version reckonPluginVersion
        id("com.github.johnrengelman.shadow") version shadowPluginVersion
    }
}

rootProject.name = "batect"

include(":app")
include(":libs")
include(":libs:docker-client")
include(":libs:git-client")
include(":libs:io")
include(":libs:logging")
include(":libs:logging-test-utils")
include(":libs:os")
include(":libs:primitives")
include(":libs:sockets")
include(":libs:telemetry")
include(":libs:test-utils")
include(":tools")
include(":tools:schema")
include(":wrapper")
include(":wrapper:testapp")
include(":wrapper:unix")
include(":wrapper:windows")
