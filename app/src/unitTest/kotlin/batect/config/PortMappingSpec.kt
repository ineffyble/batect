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

package batect.config

import batect.testutils.on
import batect.testutils.withColumn
import batect.testutils.withLineNumber
import batect.testutils.withMessage
import batect.testutils.withPath
import com.charleskorn.kaml.MissingRequiredPropertyException
import com.charleskorn.kaml.Yaml
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object PortMappingSpec : Spek({
    describe("a port mapping") {
        describe("deserializing from YAML") {
            describe("deserializing from compact form") {
                on("parsing a valid port mapping definition with single ports for both local and container ports") {
                    val portMapping = fromYaml("123:456")

                    it("returns the provided local port") {
                        assertThat(portMapping.local, equalTo(PortRange(123)))
                    }

                    it("returns the provided container port") {
                        assertThat(portMapping.container, equalTo(PortRange(456)))
                    }

                    it("returns TCP as the protocol") {
                        assertThat(portMapping.protocol, equalTo("tcp"))
                    }
                }

                on("parsing a valid port mapping definition with single ports for both local and container ports and a non-default protocol") {
                    val portMapping = fromYaml("123:456/abc")

                    it("returns the provided local port") {
                        assertThat(portMapping.local, equalTo(PortRange(123)))
                    }

                    it("returns the provided container port") {
                        assertThat(portMapping.container, equalTo(PortRange(456)))
                    }

                    it("returns the provided protocol") {
                        assertThat(portMapping.protocol, equalTo("abc"))
                    }
                }

                on("parsing a valid port mapping definition with ranges for both local and container ports") {
                    val portMapping = fromYaml("123-126:456-459")

                    it("returns the provided local ports") {
                        assertThat(portMapping.local, equalTo(PortRange(123, 126)))
                    }

                    it("returns the provided container ports") {
                        assertThat(portMapping.container, equalTo(PortRange(456, 459)))
                    }

                    it("returns TCP as the protocol") {
                        assertThat(portMapping.protocol, equalTo("tcp"))
                    }
                }

                on("parsing a valid port mapping definition with ranges for both local and container ports and a non-default protocol") {
                    val portMapping = fromYaml("123-126:456-459/abc")

                    it("returns the provided local ports") {
                        assertThat(portMapping.local, equalTo(PortRange(123, 126)))
                    }

                    it("returns the provided container ports") {
                        assertThat(portMapping.container, equalTo(PortRange(456, 459)))
                    }

                    it("returns the provided protocol") {
                        assertThat(portMapping.protocol, equalTo("abc"))
                    }
                }

                on("parsing an empty port mapping definition") {
                    it("fails with an appropriate error message") {
                        assertThat({ fromYaml("''") }, throws(withMessage("Port mapping definition cannot be empty.") and withLineNumber(1) and withColumn(1) and withPath("<root>")))
                    }
                }

                on("parsing a port mapping definition with ranges of different sizes") {
                    it("fails with an appropriate error message") {
                        assertThat({ fromYaml("10-11:20-23") }, throws(withMessage("Port mapping definition is invalid. The local port range has 2 ports and the container port range has 4 ports, but the ranges must be the same size.") and withLineNumber(1) and withColumn(1) and withPath("<root>")))
                    }
                }

                listOf(
                    "thing:",
                    "12:",
                    ":thing",
                    ":12",
                    "thing",
                    "12",
                    "thing:12",
                    "12:thing",
                    "-1:12",
                    "12:-1",
                    "0:12",
                    "12:0",
                    " ",
                    ":",
                    "/thing",
                    "1:2/",
                    ":2/thing",
                    "1:/thing",
                    "1/2:thing"
                ).map {
                    on("parsing the invalid port mapping definition '$it'") {
                        it("fails with an appropriate error message") {
                            assertThat({ fromYaml("'$it'") }, throws(withMessage("Port mapping definition '$it' is invalid. It must be in the form 'local:container', 'local:container/protocol', 'from-to:from-to' or 'from-to:from-to/protocol' and each port must be a positive integer.") and withLineNumber(1) and withColumn(1) and withPath("<root>")))
                        }
                    }
                }
            }

            describe("deserializing from expanded form") {
                on("parsing a valid port mapping definition with single ports for both local and container ports") {
                    val portMapping = fromYaml(
                        """
                            local: 123
                            container: 456
                        """.trimIndent()
                    )

                    it("returns the provided local port") {
                        assertThat(portMapping.local, equalTo(PortRange(123)))
                    }

                    it("returns the provided container port") {
                        assertThat(portMapping.container, equalTo(PortRange(456)))
                    }

                    it("returns TCP as the protocol") {
                        assertThat(portMapping.protocol, equalTo("tcp"))
                    }
                }

                on("parsing a valid port mapping definition with single ports for both local and container ports and a non-default protocol") {
                    val portMapping = fromYaml(
                        """
                            local: 123
                            container: 456
                            protocol: abc
                        """.trimIndent()
                    )

                    it("returns the provided local port") {
                        assertThat(portMapping.local, equalTo(PortRange(123)))
                    }

                    it("returns the provided container port") {
                        assertThat(portMapping.container, equalTo(PortRange(456)))
                    }

                    it("returns the provided protocol") {
                        assertThat(portMapping.protocol, equalTo("abc"))
                    }
                }

                on("parsing a valid port mapping definition with ranges for both local and container ports") {
                    val portMapping = fromYaml(
                        """
                            local: 123-126
                            container: 456-459
                        """.trimIndent()
                    )

                    it("returns the provided local ports") {
                        assertThat(portMapping.local, equalTo(PortRange(123, 126)))
                    }

                    it("returns the provided container ports") {
                        assertThat(portMapping.container, equalTo(PortRange(456, 459)))
                    }

                    it("returns TCP as the protocol") {
                        assertThat(portMapping.protocol, equalTo("tcp"))
                    }
                }

                on("parsing a valid port mapping definition with ranges for both local and container ports and a non-default protocol") {
                    val portMapping = fromYaml(
                        """
                            local: 123-126
                            container: 456-459
                            protocol: abc
                        """.trimIndent()
                    )

                    it("returns the provided local ports") {
                        assertThat(portMapping.local, equalTo(PortRange(123, 126)))
                    }

                    it("returns the provided container ports") {
                        assertThat(portMapping.container, equalTo(PortRange(456, 459)))
                    }

                    it("returns the provided protocol") {
                        assertThat(portMapping.protocol, equalTo("abc"))
                    }
                }

                on("parsing a port mapping with a non-positive local port") {
                    val yaml = """
                        local: 0
                        container: 456
                    """.trimIndent()

                    it("fails with an appropriate error message") {
                        assertThat(
                            { fromYaml(yaml) },
                            throws(
                                withMessage("Port range '0' is invalid. Ports must be positive integers.")
                                    and withLineNumber(1)
                                    and withColumn(8)
                                    and withPath("local")
                            )
                        )
                    }
                }

                on("parsing a port mapping with a non-positive container port") {
                    val yaml = """
                        local: 123
                        container: 0
                    """.trimIndent()

                    it("fails with an appropriate error message") {
                        assertThat(
                            { fromYaml(yaml) },
                            throws(
                                withMessage("Port range '0' is invalid. Ports must be positive integers.")
                                    and withLineNumber(2)
                                    and withColumn(12)
                                    and withPath("container")
                            )
                        )
                    }
                }

                on("parsing a port mapping missing the 'local' field") {
                    val yaml = "container: 456"

                    it("fails with an appropriate error message") {
                        assertThat({ fromYaml(yaml) }, throws<MissingRequiredPropertyException>(withPropertyName("local")))
                    }
                }

                on("parsing a port mapping missing the 'container' field") {
                    val yaml = "local: 123"

                    it("fails with an appropriate error message") {
                        assertThat({ fromYaml(yaml) }, throws<MissingRequiredPropertyException>(withPropertyName("container")))
                    }
                }

                on("parsing a port mapping definition with ranges of different sizes") {
                    val yaml = """
                        local: 10-11
                        container: 20-23
                    """.trimIndent()

                    it("fails with an appropriate error message") {
                        assertThat({ fromYaml(yaml) }, throws(withMessage("Port mapping definition is invalid. The local port range has 2 ports and the container port range has 4 ports, but the ranges must be the same size.") and withLineNumber(1) and withColumn(1) and withPath("<root>")))
                    }
                }
            }

            describe("deserializing from something that is neither a string nor a map") {
                val yaml = """
                    - thing
                """.trimIndent()

                it("fails with an appropriate error message") {
                    assertThat(
                        { fromYaml(yaml) },
                        throws(
                            withMessage("Port mapping definition is invalid. It must either be an object or a literal in the form 'local:container', 'local:container/protocol', 'from-to:from-to' or 'from-to:from-to/protocol'.") and withLineNumber(1) and withColumn(1) and withPath("<root>")
                        )
                    )
                }
            }
        }
    }
})

private fun fromYaml(yaml: String): PortMapping = Yaml.default.decodeFromString(PortMappingConfigSerializer, yaml)
