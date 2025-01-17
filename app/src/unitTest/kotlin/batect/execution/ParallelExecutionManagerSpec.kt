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

package batect.execution

import batect.execution.model.events.ExecutionFailedEvent
import batect.execution.model.events.StepStartingEvent
import batect.execution.model.events.TaskEvent
import batect.execution.model.events.TaskEventSink
import batect.execution.model.events.TaskFailedEvent
import batect.execution.model.steps.TaskStepRunner
import batect.primitives.CancellationException
import batect.telemetry.CommonAttributes
import batect.telemetry.CommonEvents
import batect.telemetry.TestTelemetryCaptor
import batect.testutils.createForEachTest
import batect.testutils.createLoggerForEachTest
import batect.testutils.createMockTaskStep
import batect.testutils.given
import batect.testutils.on
import batect.ui.EventLogger
import batect.ui.containerio.ContainerIOStreamingOptions
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import kotlinx.serialization.json.JsonPrimitive
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutionException
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

object ParallelExecutionManagerSpec : Spek({
    describe("a parallel execution manager") {
        val ioStreamingOptions by createForEachTest { mock<ContainerIOStreamingOptions>() }
        val eventLogger by createForEachTest {
            mock<EventLogger> {
                on { it.ioStreamingOptions } doReturn ioStreamingOptions
            }
        }

        val taskStepRunner by createForEachTest { mock<TaskStepRunner>() }
        val stateMachine by createForEachTest { mock<TaskStateMachine>() }
        val telemetryCaptor by createForEachTest { TestTelemetryCaptor() }
        val logger by createLoggerForEachTest()

        given("there is no maximum level of parallelism set") {
            val maximumLevelOfParallelism: Int? = null
            val executionManager by createForEachTest { ParallelExecutionManager(eventLogger, taskStepRunner, stateMachine, telemetryCaptor, maximumLevelOfParallelism, logger) }

            given("a single step is provided by the state machine") {
                val step by createForEachTest { createMockTaskStep() }
                beforeEachTest { whenever(stateMachine.popNextStep(false)).doReturn(step, null) }

                given("that step runs successfully") {
                    given("the step posts no events") {
                        on("running the task") {
                            beforeEachTest { executionManager.run() }

                            it("logs the step to the event logger") {
                                verify(eventLogger).postEvent(StepStartingEvent(step))
                            }

                            it("runs the step") {
                                verify(taskStepRunner).run(eq(step), eq(executionManager))
                            }

                            it("logs the step to the event logger and then runs it") {
                                inOrder(eventLogger, taskStepRunner) {
                                    verify(eventLogger).postEvent(StepStartingEvent(step))
                                    verify(taskStepRunner).run(eq(step), eq(executionManager))
                                }
                            }
                        }
                    }

                    given("the step posts an informational event") {
                        val eventToPost by createForEachTest {
                            mock<TaskEvent> {
                                on { isInformationalEvent } doReturn true
                            }
                        }

                        val stepThatShouldNotBeRun by createForEachTest { createMockTaskStep() }

                        beforeEachTest {
                            whenever(taskStepRunner.run(eq(step), eq(executionManager))).then { invocation ->
                                val eventSink = invocation.arguments[1] as TaskEventSink

                                whenever(stateMachine.popNextStep(any())).doReturn(stepThatShouldNotBeRun, null)
                                eventSink.postEvent(eventToPost)
                                whenever(stateMachine.popNextStep(any())).doReturn(null)

                                null
                            }
                        }

                        on("running the task") {
                            beforeEachTest { executionManager.run() }

                            it("logs the posted event to the event logger") {
                                verify(eventLogger).postEvent(eventToPost)
                            }

                            it("does not forward the posted event to the state machine") {
                                verify(stateMachine, never()).postEvent(any())
                            }

                            it("does not queue any new work as a result of the event") {
                                verify(taskStepRunner, never()).run(eq(stepThatShouldNotBeRun), any())
                            }
                        }
                    }

                    given("the step posts a non-informational event") {
                        val eventToPost by createForEachTest {
                            mock<TaskEvent> {
                                on { isInformationalEvent } doReturn false
                            }
                        }

                        val stepTriggeredByEvent by createForEachTest { createMockTaskStep() }

                        beforeEachTest {
                            whenever(taskStepRunner.run(eq(step), eq(executionManager))).then { invocation ->
                                val eventSink = invocation.arguments[1] as TaskEventSink

                                whenever(stateMachine.popNextStep(any())).doReturn(stepTriggeredByEvent, null)
                                eventSink.postEvent(eventToPost)

                                null
                            }
                        }

                        on("running the task") {
                            beforeEachTest { executionManager.run() }

                            it("logs the posted event to the event logger") {
                                verify(eventLogger).postEvent(eventToPost)
                            }

                            it("forwards the posted event to the state machine") {
                                verify(stateMachine).postEvent(eventToPost)
                            }

                            it("logs the posted event to the event logger before forwarding it to the state machine") {
                                inOrder(eventLogger, stateMachine) {
                                    verify(eventLogger).postEvent(eventToPost)
                                    verify(stateMachine).postEvent(eventToPost)
                                }
                            }

                            it("queues any new work made available as a result of the event") {
                                verify(taskStepRunner).run(eq(stepTriggeredByEvent), any())
                            }
                        }
                    }
                }
            }

            given("a step throws an exception during execution") {
                val step = createMockTaskStep()

                beforeEachTest {
                    whenever(stateMachine.popNextStep(false)).doReturn(step, null)
                }

                given("the exception is not because the step was cancelled") {
                    val exception = ExecutionException("Something went wrong.", null)

                    beforeEachTest {
                        whenever(taskStepRunner.run(eq(step), eq(executionManager))).then { throw exception }
                    }

                    on("running the task") {
                        beforeEachTest { executionManager.run() }

                        it("logs a task failure event to the event logger") {
                            verify(eventLogger).postEvent(ExecutionFailedEvent("During execution of step of kind '${step::class.simpleName}': java.util.concurrent.ExecutionException: Something went wrong."))
                        }

                        it("logs a task failure event to the state machine") {
                            verify(stateMachine).postEvent(ExecutionFailedEvent("During execution of step of kind '${step::class.simpleName}': java.util.concurrent.ExecutionException: Something went wrong."))
                        }

                        it("reports the exception in telemetry") {
                            assertThat(telemetryCaptor.allEvents, hasSize(equalTo(1)))

                            val event = telemetryCaptor.allEvents.single()
                            assertThat(event.type, equalTo(CommonEvents.UnhandledException))
                            assertThat(event.attributes[CommonAttributes.ExceptionCaughtAt], equalTo(JsonPrimitive("batect.execution.ParallelExecutionManager.runStep\$lambda\$3")))
                        }
                    }
                }

                given("the exception directly signals that the step was cancelled") {
                    beforeEachTest {
                        whenever(taskStepRunner.run(eq(step), eq(executionManager))).thenThrow(CancellationException("The step was cancelled"))
                    }

                    on("running the task") {
                        beforeEachTest { executionManager.run() }

                        it("does not log a task failure event to the event logger") {
                            verify(eventLogger, never()).postEvent(any<TaskFailedEvent>())
                        }

                        it("does not log a task failure event to the state machine") {
                            verify(stateMachine, never()).postEvent(any<TaskFailedEvent>())
                        }
                    }
                }

                given("the exception directly signals that a coroutine within the step was cancelled") {
                    beforeEachTest {
                        whenever(taskStepRunner.run(eq(step), eq(executionManager))).thenThrow(kotlinx.coroutines.CancellationException("The step was cancelled"))
                    }

                    on("running the task") {
                        beforeEachTest { executionManager.run() }

                        it("does not log a task failure event to the event logger") {
                            verify(eventLogger, never()).postEvent(any<TaskFailedEvent>())
                        }

                        it("does not log a task failure event to the state machine") {
                            verify(stateMachine, never()).postEvent(any<TaskFailedEvent>())
                        }
                    }
                }

                given("the exception indirectly signals that the step was cancelled") {
                    beforeEachTest {
                        whenever(taskStepRunner.run(eq(step), eq(executionManager))).then {
                            throw ExecutionException(
                                "Something went wrong",
                                CancellationException("The step was cancelled")
                            )
                        }
                    }

                    on("running the task") {
                        beforeEachTest { executionManager.run() }

                        it("does not log a task failure event to the event logger") {
                            verify(eventLogger, never()).postEvent(any<TaskFailedEvent>())
                        }

                        it("does not log a task failure event to the state machine") {
                            verify(stateMachine, never()).postEvent(any<TaskFailedEvent>())
                        }
                    }
                }
            }

            on("two steps being provided by the state machine initially") {
                val step1 = createMockTaskStep()
                val step2 = createMockTaskStep()

                var step1SawStep2 = false
                var step2SawStep1 = false

                beforeEachTest {
                    step1SawStep2 = false
                    step2SawStep1 = false

                    whenever(stateMachine.popNextStep(false)).doReturn(step1, null)
                    whenever(stateMachine.popNextStep(true)).doReturn(step2, null)

                    val waitForStep1 = Semaphore(1)
                    val waitForStep2 = Semaphore(1)
                    waitForStep1.acquire()
                    waitForStep2.acquire()

                    whenever(taskStepRunner.run(eq(step1), eq(executionManager))).doAnswer {
                        waitForStep1.release()
                        step1SawStep2 = waitForStep2.tryAcquire(100, TimeUnit.MILLISECONDS)
                    }

                    whenever(taskStepRunner.run(eq(step2), eq(executionManager))).doAnswer {
                        waitForStep2.release()
                        step2SawStep1 = waitForStep1.tryAcquire(100, TimeUnit.MILLISECONDS)
                    }

                    executionManager.run()
                }

                it("logs the first step to the event logger") {
                    verify(eventLogger).postEvent(StepStartingEvent(step1))
                }

                it("runs the first step") {
                    verify(taskStepRunner).run(eq(step1), eq(executionManager))
                }

                it("logs the second step to the event logger") {
                    verify(eventLogger).postEvent(StepStartingEvent(step2))
                }

                it("runs the second step") {
                    verify(taskStepRunner).run(eq(step2), eq(executionManager))
                }

                it("runs step 1 in parallel with step 2") {
                    assertThat(step1SawStep2, equalTo(true))
                    assertThat(step2SawStep1, equalTo(true))
                }
            }

            on("one step being provided by the state machine initially and then another added as a result of the first step") {
                val step1 = createMockTaskStep()
                val step2 = createMockTaskStep()

                var step2StartedBeforeStep1Ended = false

                beforeEachTest {
                    step2StartedBeforeStep1Ended = false

                    val step2TriggerEvent = mock<TaskEvent>()
                    whenever(stateMachine.popNextStep(false)).doReturn(step1, null)

                    val waitForStep2 = Semaphore(1)
                    waitForStep2.acquire()

                    whenever(taskStepRunner.run(eq(step1), eq(executionManager))).doAnswer { invocation ->
                        val eventSink = invocation.arguments[1] as TaskEventSink
                        eventSink.postEvent(step2TriggerEvent)

                        step2StartedBeforeStep1Ended = waitForStep2.tryAcquire(100, TimeUnit.MILLISECONDS)
                    }

                    whenever(stateMachine.postEvent(step2TriggerEvent)).doAnswer {
                        whenever(stateMachine.popNextStep(true)).doReturn(step2, null)

                        Unit
                    }

                    whenever(taskStepRunner.run(eq(step2), eq(executionManager))).doAnswer {
                        waitForStep2.release()
                    }

                    executionManager.run()
                }

                it("logs the first step to the event logger") {
                    verify(eventLogger).postEvent(StepStartingEvent(step1))
                }

                it("runs the first step") {
                    verify(taskStepRunner).run(eq(step1), eq(executionManager))
                }

                it("logs the second step to the event logger") {
                    verify(eventLogger).postEvent(StepStartingEvent(step2))
                }

                it("runs the second step") {
                    verify(taskStepRunner).run(eq(step2), eq(executionManager))
                }

                it("runs the first step in parallel with the second step") {
                    assertThat(step2StartedBeforeStep1Ended, equalTo(true))
                }
            }
        }

        given("there is a maximum level of parallelism set") {
            val maximumLevelOfParallelism = 2
            val executionManager by createForEachTest { ParallelExecutionManager(eventLogger, taskStepRunner, stateMachine, telemetryCaptor, maximumLevelOfParallelism, logger) }

            given("the state machine provides more steps than the configured level of parallelism initially") {
                val stepsRunningInParallel by createForEachTest { AtomicInteger(0) }
                val otherStepsRunningInParallel by createForEachTest { ConcurrentLinkedQueue<Int>() }

                val createMockStep = { countsAgainstParallelismCap: Boolean ->
                    val step = createMockTaskStep(countsAgainstParallelismCap)

                    whenever(taskStepRunner.run(step, executionManager)).doAnswer {
                        val stepsRunningNow = stepsRunningInParallel.incrementAndGet()
                        otherStepsRunningInParallel.add(stepsRunningNow)
                        Thread.sleep(100)
                        stepsRunningInParallel.decrementAndGet()
                        Unit
                    }

                    step
                }

                given("all of those steps do not count towards the parallelism cap") {
                    val step1 by createForEachTest { createMockStep(false) }
                    val step2 by createForEachTest { createMockStep(false) }
                    val step3 by createForEachTest { createMockStep(false) }

                    beforeEachTest {
                        whenever(stateMachine.popNextStep(any())).doReturn(step1, step2, step3, null)

                        executionManager.run()
                    }

                    it("runs all steps in parallel") {
                        assertThat(otherStepsRunningInParallel.maxOrNull(), equalTo(3))
                    }
                }

                given("all of those steps count towards the parallelism cap") {
                    val step1 by createForEachTest { createMockStep(true) }
                    val step2 by createForEachTest { createMockStep(true) }
                    val step3 by createForEachTest { createMockStep(true) }

                    beforeEachTest {
                        whenever(stateMachine.popNextStep(any())).doReturn(step1, step2, step3, null)

                        executionManager.run()
                    }

                    it("only runs the number of steps allowed by the maximum level of parallelism in parallel") {
                        assertThat(otherStepsRunningInParallel.maxOrNull(), equalTo(2))
                    }
                }
            }
        }
    }
})
