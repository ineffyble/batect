/*
   Copyright 2017-2019 Charles Korn.

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

package batect.cli.options.defaultvalues

import batect.cli.options.ValueConversionResult

class EnvironmentVariableDefaultValueProviderFactory(private val environment: Map<String, String> = System.getenv()) {
    fun <StorageType, ValueType : StorageType> create(
        name: String,
        fallback: StorageType,
        valueConverter: (String) -> ValueConversionResult<ValueType>
    ): DefaultValueProvider<StorageType> = object : DefaultValueProvider<StorageType> {
        override val value: PossibleValue<StorageType>
            get() {
                val environmentVariableValue = environment[name]

                if (environmentVariableValue == null) {
                    return PossibleValue.Valid(fallback)
                }

                return when (val conversionResult = valueConverter(environmentVariableValue)) {
                    is ValueConversionResult.ConversionSucceeded -> PossibleValue.Valid(conversionResult.value)
                    is ValueConversionResult.ConversionFailed -> PossibleValue.Invalid("The value of the $name environment variable ('$environmentVariableValue') is invalid: ${conversionResult.message}")
                }
            }

        override val description: String
            get() = "defaults to the value of the $name environment variable (which is currently $currentStateDescription)$fallbackDescription"

        private val fallbackDescription = if (fallback == null) {
            ""
        } else {
            " or '$fallback' if $name is not set"
        }

        private val currentStateDescription = if (environment.containsKey(name)) {
            "'${environment.getValue(name)}'"
        } else {
            "not set"
        }
    }
}
