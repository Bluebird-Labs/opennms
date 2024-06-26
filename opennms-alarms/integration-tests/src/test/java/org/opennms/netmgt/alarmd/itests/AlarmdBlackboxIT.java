/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.netmgt.alarmd.itests;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Every.everyItem;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.opennms.core.test.alarms.AlarmMatchers.acknowledged;
import static org.opennms.core.test.alarms.AlarmMatchers.hasSeverity;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.opennms.core.test.alarms.driver.Scenario;
import org.opennms.core.test.alarms.driver.ScenarioResults;
import org.opennms.core.test.alarms.driver.State;
import org.opennms.netmgt.events.api.EventConstants;
import org.opennms.netmgt.model.OnmsSeverity;

/**
 * This test suite allows us to:
 *  A) Define and play out scenarios using timestamped events and actions.
 *  B) Playback the scenarios
 *  C) Analyze the state of alarms at various points in time.
 *  D) Analyze the state changes of a particular alarm over time.
 *
 * Using these tools we can validate the behavior of the alarms in various scenarios
 * without worrying about the underlying mechanics.
 *
 * @author jwhite
 */
public class AlarmdBlackboxIT {

    /**
     * Verifies the basic life-cycle of a trigger, followed by a clear.
     *
     * Indirectly verifies the cosmicClear and cleanUp automations.
     */
    @Test
    public void canTriggerAndClearAlarm() {
        Scenario scenario = Scenario.builder()
                .withLegacyAlarmBehavior()
                .withNodeDownEvent(1, 1)
                .withNodeUpEvent(2, 1)
                .build();
        ScenarioResults results = scenario.play();

        // Verify the set of alarms at various points in time

        // t=0, no alarms
        assertThat(results.getAlarms(0), hasSize(0));
        // t=1, a single problem alarm
        assertThat(results.getAlarms(1), hasSize(1));
        assertThat(results.getProblemAlarm(1), hasSeverity(OnmsSeverity.MAJOR));
        // t=2, a (cleared) problem and a resolution
        assertThat(results.getAlarms(2), hasSize(2));
        assertThat(results.getProblemAlarm(2), hasSeverity(OnmsSeverity.CLEARED));
        assertThat(results.getResolutionAlarm(2), hasSeverity(OnmsSeverity.NORMAL));
        // t=∞
        assertThat(results.getAlarmsAtLastKnownTime(), hasSize(0));

        // Now verify the state changes for the particular alarms

        // the problem
        List<State> problemStates = results.getStateChangesForAlarmWithId(results.getProblemAlarm(1).getId());
        assertThat(problemStates, hasSize(3)); // warning, cleared, deleted
        // state 0 at t=1
        assertThat(problemStates.get(0).getTime(), equalTo(1L));
        assertThat(problemStates.get(0).getAlarm(), hasSeverity(OnmsSeverity.MAJOR));
        // state 1 at t=2
        assertThat(problemStates.get(1).getTime(), equalTo(2L));
        assertThat(problemStates.get(1).getAlarm(), hasSeverity(OnmsSeverity.CLEARED));
        assertThat(problemStates.get(1).getAlarm().getCounter(), equalTo(1));
        // state 2 at t in [5m2ms, 10m]
        assertThat(problemStates.get(2).getTime(), greaterThanOrEqualTo(2L + TimeUnit.MINUTES.toMillis(5)));
        assertThat(problemStates.get(2).getTime(), lessThan(TimeUnit.MINUTES.toMillis(10)));
        assertThat(problemStates.get(2).getAlarm(), nullValue()); // DELETED

        // the resolution
        List<State> resolutionStates = results.getStateChangesForAlarmWithId(results.getResolutionAlarm(2).getId());
        assertThat(resolutionStates, hasSize(2)); // cleared, deleted
        // state 0 at t=2
        assertThat(resolutionStates.get(0).getTime(), equalTo(2L));
        assertThat(resolutionStates.get(0).getAlarm(), hasSeverity(OnmsSeverity.NORMAL));
        // state 1 at t in [5m2ms, 10m]
        assertThat(resolutionStates.get(1).getTime(), greaterThanOrEqualTo(2L + TimeUnit.MINUTES.toMillis(5)));
        assertThat(resolutionStates.get(1).getTime(), lessThan(TimeUnit.MINUTES.toMillis(10)));
        assertThat(resolutionStates.get(1).getAlarm(), nullValue()); // DELETED
    }


    /**
     * Indirectly verifies the cosmicClear, unclear and GC automations.
     */
    @Test
    public void canFlapAlarm() {
        // Alarms may not immediately clear/unclear due to the way to rules are structured
        // so we add some delay between the steps to make sure that they do
        long step = TimeUnit.MINUTES.toMillis(2);
        Scenario scenario = Scenario.builder()
                .withLegacyAlarmBehavior()
                .withTickLength(1, TimeUnit.MINUTES)
                .withNodeDownEvent(step, 1)
                .withNodeUpEvent(2*step, 1)
                .withNodeDownEvent(3*step, 1)
                .withNodeUpEvent(4*step, 1)
                .withNodeDownEvent(5*step, 1)
                .build();
        ScenarioResults results = scenario.play();

        // Verify the set of alarms at various points in time

        // t=0, no alarms
        assertThat(results.getAlarms(0), hasSize(0));
        // t=1, a single problem alarm
        assertThat(results.getAlarms(step), hasSize(1));
        assertThat(results.getProblemAlarm(step), hasSeverity(OnmsSeverity.MAJOR));
        assertThat(results.getProblemAlarm(step).getCounter(), equalTo(1));
        // t=2, a (cleared) problem and a resolution
        assertThat(results.getAlarms(2*step), hasSize(2));
        assertThat(results.getProblemAlarm(2*step), hasSeverity(OnmsSeverity.CLEARED));
        assertThat(results.getProblemAlarm(2*step).getCounter(), equalTo(1));
        assertThat(results.getResolutionAlarm(2*step), hasSeverity(OnmsSeverity.NORMAL));
        assertThat(results.getResolutionAlarm(2*step).getCounter(), equalTo(1));
        // t=3, a (re-armed) problem and a resolution
        assertThat(results.getAlarms(3*step), hasSize(2));
        assertThat(results.getProblemAlarm(3*step), hasSeverity(OnmsSeverity.MAJOR));
        assertThat(results.getProblemAlarm(3*step).getCounter(), equalTo(2));
        assertThat(results.getResolutionAlarm(3*step), hasSeverity(OnmsSeverity.NORMAL));
        assertThat(results.getResolutionAlarm(3*step).getCounter(), equalTo(1));
        // t=4, a (cleared) problem and a resolution
        assertThat(results.getAlarms(4*step), hasSize(2));
        assertThat(results.getProblemAlarm(4*step), hasSeverity(OnmsSeverity.CLEARED));
        assertThat(results.getProblemAlarm(4*step).getCounter(), equalTo(2));
        assertThat(results.getResolutionAlarm(4*step), hasSeverity(OnmsSeverity.NORMAL));
        // Allow the resolution to have a counter of 1 or 2 - the alarm may have been deleted
        assertThat(results.getResolutionAlarm(4*step).getCounter(), anyOf(equalTo(1), equalTo(2)));
        // t=5, a (re-armed) problem and a resolution
        assertThat(results.getAlarms(5*step), hasSize(2));
        assertThat(results.getProblemAlarm(5*step), hasSeverity(OnmsSeverity.MAJOR));
        assertThat(results.getProblemAlarm(5*step).getCounter(), equalTo(3));
        assertThat(results.getResolutionAlarm(5*step), hasSeverity(OnmsSeverity.NORMAL));
        // Allow the resolution to have a counter of 1 or 2 - the alarm may have been deleted
        assertThat(results.getResolutionAlarm(5*step).getCounter(), anyOf(equalTo(1), equalTo(2)));
        // t=∞
        assertThat(results.getAlarmsAtLastKnownTime(), hasSize(0));
    }

    /**
     * Verifies the basic life-cycle of a trigger, followed by a clear.
     *
     * Indirectly verifies the cosmicClear and fullCleanUp automations.
     */
    @Test
    public void canTriggerAcknowledgeAndClearAlarm() {
        Scenario scenario = Scenario.builder()
                .withLegacyAlarmBehavior()
                .withNodeDownEvent(1, 1)
                .withAcknowledgmentForNodeDownAlarm(2, 1)
                .withNodeUpEvent(3, 1)
                .build();
        ScenarioResults results = scenario.play();

        // Verify the set of alarms at various points in time

        // t=0, no alarms
        assertThat(results.getAlarms(0), hasSize(0));
        // t=1, a single problem alarm that is not yet acknowledged
        assertThat(results.getAlarms(1), hasSize(1));
        assertThat(results.getProblemAlarm(1), hasSeverity(OnmsSeverity.MAJOR));
        assertThat(results.getProblemAlarm(1), not(acknowledged()));
        // t=2, a single problem alarm that is acknowledged
        assertThat(results.getAlarms(2), hasSize(1));
        assertThat(results.getProblemAlarm(2), hasSeverity(OnmsSeverity.MAJOR));
        assertThat(results.getProblemAlarm(2), acknowledged());
        // t=3, a (acknowledged & cleared) problem and a resolution
        assertThat(results.getAlarms(3), hasSize(2));
        assertThat(results.getProblemAlarm(3), hasSeverity(OnmsSeverity.CLEARED));
        assertThat(results.getProblemAlarm(3), acknowledged());
        assertThat(results.getResolutionAlarm(3), hasSeverity(OnmsSeverity.NORMAL));
        // t=∞
        assertThat(results.getAlarmsAtLastKnownTime(), hasSize(0));

        // Now verify the state changes for the particular alarms

        // the problem
        List<State> problemStates = results.getStateChangesForAlarmWithId(results.getProblemAlarm(1).getId());
        assertThat(problemStates, hasSize(4)); // major, major+acked, cleared+acked, deleted
        // state 0 at t=1
        assertThat(problemStates.get(0).getTime(), equalTo(1L));
        assertThat(problemStates.get(0).getAlarm(), hasSeverity(OnmsSeverity.MAJOR));
        // state 1 at t=2
        assertThat(problemStates.get(1).getTime(), equalTo(2L));
        assertThat(problemStates.get(1).getAlarm(), hasSeverity(OnmsSeverity.MAJOR));
        assertThat(problemStates.get(1).getAlarm(), acknowledged());
        // state 2 at t=3
        assertThat(problemStates.get(2).getTime(), equalTo(3L));
        assertThat(problemStates.get(2).getAlarm(), hasSeverity(OnmsSeverity.CLEARED));
        assertThat(problemStates.get(2).getAlarm(), acknowledged());
        // state 3 at t in [23h,25h]
        assertThat(problemStates.get(3).getTime(), greaterThanOrEqualTo(TimeUnit.HOURS.toMillis(23)));
        assertThat(problemStates.get(3).getTime(), lessThan(TimeUnit.HOURS.toMillis(25)));
        assertThat(problemStates.get(3).getAlarm(), nullValue()); // DELETED
    }

    /**
     * Verifies the basic life-cycle of a trigger, followed by a clear.
     *
     * Indirectly verifies the fullGC automation.
     */
    @Test
    public void canTriggerAndAcknowledgeAlarm() {
        Scenario scenario = Scenario.builder()
                .withNodeDownEvent(1, 1)
                .withAcknowledgmentForNodeDownAlarm(2, 1)
                .build();
        ScenarioResults results = scenario.play();

        // Verify the set of alarms at various points in time

        // t=0, no alarms
        assertThat(results.getAlarms(0), hasSize(0));
        // t=1, a single problem alarm that is not yet acknowledged
        assertThat(results.getAlarms(1), hasSize(1));
        assertThat(results.getProblemAlarm(1), hasSeverity(OnmsSeverity.MAJOR));
        assertThat(results.getProblemAlarm(1), not(acknowledged()));
        // t=2, a single problem alarm that is acknowledged
        assertThat(results.getAlarms(2), hasSize(1));
        assertThat(results.getProblemAlarm(2), hasSeverity(OnmsSeverity.MAJOR));
        assertThat(results.getProblemAlarm(2), acknowledged());
        // t=∞
        assertThat(results.getAlarmsAtLastKnownTime(), hasSize(0));

        // Now verify the state changes for the particular alarms

        // the problem
        List<State> problemStates = results.getStateChangesForAlarmWithId(results.getProblemAlarm(1).getId());
        assertThat(problemStates, hasSize(3)); // major, major+acked, deleted
        // state 0 at t=1
        assertThat(problemStates.get(0).getTime(), equalTo(1L));
        assertThat(problemStates.get(0).getAlarm(), hasSeverity(OnmsSeverity.MAJOR));
        // state 1 at t=2
        assertThat(problemStates.get(1).getTime(), equalTo(2L));
        assertThat(problemStates.get(1).getAlarm(), hasSeverity(OnmsSeverity.MAJOR));
        assertThat(problemStates.get(1).getAlarm(), acknowledged());
        // state 2 at t in [7d,9d]
        assertThat(problemStates.get(2).getTime(), greaterThanOrEqualTo(TimeUnit.DAYS.toMillis(2)));
        assertThat(problemStates.get(2).getTime(), lessThan(TimeUnit.DAYS.toMillis(9)));
        assertThat(problemStates.get(2).getAlarm(), nullValue()); // DELETED
    }


    /**
     * Verifies the basic lifecycle of a situation.
     */
    @Test
    public void canCreateSituation() {
        Scenario scenario = Scenario.builder()
                .withLegacyAlarmBehavior()
                // Create some node down alarms
                .withNodeDownEvent(1, 1)
                .withNodeDownEvent(2, 2)
                // Create a situation that contains the node down alarms
                .withSituationForNodeDownAlarms(3, "situation#1", 1, 2)
                // Now clear the node down alarms
                .withNodeUpEvent(4, 1)
                .withNodeUpEvent(4, 2)
                .build();
        ScenarioResults results = scenario.play();

        // Verify the set of alarms at various points in time

        // t=0, no alarms
        assertThat(results.getAlarms(0), hasSize(0));
        // t=1, a single problem alarm
        assertThat(results.getAlarms(1), hasSize(1));
        assertThat(results.getProblemAlarm(1), hasSeverity(OnmsSeverity.MAJOR));
        // t=2, two problem alarms
        assertThat(results.getAlarms(2), hasSize(2));
        // t=3, two problem alarms + 1 situation
        assertThat(results.getAlarms(3), hasSize(3)); // the situation is also an alarm, so it is counted here
        assertThat(results.getSituations(3), hasSize(1));
        assertThat(results.getSituation(3), hasSeverity(OnmsSeverity.CRITICAL)); // the situation should be escalated in severity
        // t=4, everything should be cleared
        assertThat(results.getProblemAlarm(4), hasSeverity(OnmsSeverity.CLEARED));
        assertThat(results.getSituation(4), hasSeverity(OnmsSeverity.CLEARED));
        // t=∞
        assertThat(results.getAlarmsAtLastKnownTime(), hasSize(0));
    }


    /**
     * Verifies a Situation will de-escalate if the Max Severity of related alarms decreases.
     */
    @Test
    public void canDeEscalateSituation() {
        Scenario scenario = Scenario.builder()
                .withLegacyAlarmBehavior()
                // Create some node down alarms
                .withNodeDownEvent(1, 1)
                .withInterfaceDownEvent(2, 2)
                // Create a situation that contains the node down alarms
                .withSituationForAlarmReductionKeys(3, "situation#1", 
                                                    EventConstants.NODE_DOWN_EVENT_UEI + ":1", 
                                                    EventConstants.INTERFACE_DOWN_EVENT_UEI + ":2")
                // Now clear the node down alarms
                .withNodeUpEvent(4, 1)
                .build();
        ScenarioResults results = scenario.play();

        // Verify the set of alarms at various points in time

        // t=0, no alarms
        assertThat(results.getAlarms(0), hasSize(0));
        // t=1, a single problem alarm
        assertThat(results.getAlarms(1), hasSize(1));
        assertThat(results.getProblemAlarm(1), hasSeverity(OnmsSeverity.MAJOR));
        // t=2, two problem alarms
        assertThat(results.getAlarms(2), hasSize(2));
        // t=3, two problem alarms + 1 situation
        assertThat(results.getAlarms(3), hasSize(3)); // the situation is also an alarm, so it is counted here
        assertThat(results.getSituations(3), hasSize(1));
        assertThat(results.getSituation(3), hasSeverity(OnmsSeverity.CRITICAL)); // the situation should be escalated in severity
        // t=4, situation should now have severity MAJOR (interfaceDown alarm is still present and has severity=Minor)
        assertThat(results.getSituation(4), hasSeverity(OnmsSeverity.MAJOR));
        // t=∞
        assertThat(results.getAlarmsAtLastKnownTime(), hasSize(0));
    }

    /**
     * Verifies ACK'ing a situation will ACK all of the related alarms which are unacked.
     */
    @Test
    public void situationAcknowledgmentAcknowledgesAllAlarms() {
        Scenario scenario = Scenario.builder()
                .withLegacyAlarmBehavior()
                // Create some node down alarms
                .withNodeDownEvent(1, 1)
                .withNodeDownEvent(2, 2)
                // Create a situation that contains the node down alarms
                .withSituationForNodeDownAlarms(3, "situation#1", 1, 2)
                // Now ACK the situation
                .withAcknowledgmentForSituation(4, "situation#1")
                .build();
        ScenarioResults results = scenario.play();

        // Verify the set of alarms at various points in time

        // t=0, no alarms
        assertThat(results.getAlarms(0), hasSize(0));
        // t=1, a single problem alarm
        assertThat(results.getAlarms(1), hasSize(1));
        // t=2, two problem alarms
        assertThat(results.getAlarms(2), hasSize(2));
        // t=3, two problem alarms + 1 situation, situation is not acknowledged
        assertThat(results.getAlarms(3), hasSize(3)); // the situation is also an alarm, so it is counted here
        assertThat(results.getSituation(3), not(acknowledged()));
        assertThat(results.getAlarms(3), everyItem(not(acknowledged())));
        // t=4, everything should be Ack'd
        assertThat(results.getSituation(4), acknowledged());
        assertThat(results.getAlarms(4), everyItem(acknowledged()));
        // t=∞
        assertThat(results.getAlarmsAtLastKnownTime(), hasSize(0));
    }

    /**
     * Verifies Unacking a situation should unack all previously acked related
     * alarms.
     */
    @Test
    public void situationUnAcknowledgmentUnAcknowledgesAllAlarms() {
        Scenario scenario = Scenario.builder()
                .withLegacyAlarmBehavior()
                // Create some node down alarms
                .withNodeDownEvent(1, 1)
                .withNodeDownEvent(2, 2)
                // Create a situation that contains the node down alarms
                .withSituationForNodeDownAlarms(3, "situation#1", 1, 2)
                // Now ACK the situation
                .withAcknowledgmentForSituation(4, "situation#1")
                .withUnAcknowledgmentForSituation(5, "situation#1")
                .build();
        ScenarioResults results = scenario.play();

        // Verify the set of alarms at various points in time

        // t=0, no alarms
        assertThat(results.getAlarms(0), hasSize(0));
        // t=1, a single problem alarm
        assertThat(results.getAlarms(1), hasSize(1));
        // t=2, two problem alarms
        assertThat(results.getAlarms(2), hasSize(2));
        // t=3, two problem alarms + 1 situation, situation is not acknowledged
        assertThat(results.getAlarms(3), hasSize(3)); // the situation is also an alarm, so it is counted here
        assertThat(results.getSituation(3), not(acknowledged()));
        assertThat(results.getAlarms(3), everyItem(not(acknowledged())));
        // t=4, everything should be Ack'd
        assertThat(results.getSituation(4), acknowledged());
        assertThat(results.getAlarms(4), everyItem(acknowledged()));
        // t=5, all alarms and situation should be unacked
        assertThat(results.getAlarms(5), everyItem(not(acknowledged())));
        // t=∞
        assertThat(results.getAlarmsAtLastKnownTime(), hasSize(0));
    }

    @Test
    public void alarmsAckSituation() {
        // A situation is deemed "acked" if all the related alarms are acked
        Scenario scenario = Scenario.builder()
                .withLegacyAlarmBehavior()
                // Create some node down alarms
                .withNodeDownEvent(1, 1)
                .withNodeDownEvent(2, 2)
                // Create a situation that contains the node down alarms
                .withSituationForNodeDownAlarms(3, "situation#1", 1, 2)
                // Now ACK both alarms
                .withAcknowledgmentForNodeDownAlarm(4, 1)
                .withAcknowledgmentForNodeDownAlarm(4, 2)
                .build();
        ScenarioResults results = scenario.play();

        // Verify the set of alarms at various points in time

        // t=0, no alarms
        assertThat(results.getAlarms(0), hasSize(0));
        // t=1, a single problem alarm
        assertThat(results.getAlarms(1), hasSize(1));
        // t=2, two problem alarms
        assertThat(results.getAlarms(2), hasSize(2));
        // t=3, two problem alarms + 1 situation, situation is not acknowledged
        assertThat(results.getAlarms(3), hasSize(3)); // the situation is also an alarm, so it is counted here
        assertThat(results.getSituation(3), not(acknowledged()));

        // t=4, everything should be Ack'd
        assertThat(results.getSituation(4), acknowledged());
        assertThat(results.getAlarms(4), everyItem(acknowledged()));
        // t=∞
        assertThat(results.getAlarmsAtLastKnownTime(), hasSize(0));
    }

    @Test
    public void alarmsUnAckSituation() {
        // If a new unacked alarm gets added to an acked situation, or an existing related alarm is unacknowledged,
        // then the situation itself should be unacked
        // (but all other related alarms which were acked should remain acked)
        Scenario scenario = Scenario.builder()
                .withLegacyAlarmBehavior()
                // Create some node down alarms
                .withNodeDownEvent(1, 1)
                .withNodeDownEvent(2, 2)
                // Create a situation that contains the node down alarms
                .withSituationForNodeDownAlarms(3, "situation#1", 1, 2)
                // Now ACK the situation
                .withAcknowledgmentForSituation(4, "situation#1")
                // now un-acknowledge one of the alarms
                .withUnAcknowledgmentForNodeDownAlarm(5, 1)
                .build();
        ScenarioResults results = scenario.play();

        // Verify the set of alarms at various points in time

        // t=0, no alarms
        assertThat(results.getAlarms(0), hasSize(0));
        // t=1, a single problem alarm
        assertThat(results.getAlarms(1), hasSize(1));
        // t=2, two problem alarms
        assertThat(results.getAlarms(2), hasSize(2));
        // t=3, two problem alarms + 1 situation, situation is not acknowledged
        assertThat(results.getAlarms(3), hasSize(3)); // the situation is also an alarm, so it is counted here
        assertThat(results.getSituation(3), not(acknowledged()));

        // t=4, everything should be Ack'd
        assertThat(results.getSituation(4), acknowledged());
        assertThat(results.getAlarms(4), everyItem(acknowledged()));
        // t=5, alarm and situation should be unacked
        assertThat(results.getSituation(5), not(acknowledged()));
        // t=6, but other alarm should still be ACK'd
        assertThat(results.getAlarms(6), not(everyItem(not(acknowledged()))));
        // t=∞
        assertThat(results.getAlarmsAtLastKnownTime(), hasSize(0));
    }

    @Test
    public void oldAlarmsCanUnAckSituation() {
        // If an older unacked alarm gets added to an acked situation, or an
        // existing related alarm is unacknowledged,
        // then the situation itself should be unacked (but all other related
        // alarms which were acked should remain acked)
        Scenario scenario = Scenario.builder().withLegacyAlarmBehavior()
                // Create some node down alarms
                .withNodeDownEvent(1, 1).withNodeDownEvent(2, 2).withNodeDownEvent(3, 3)
                // Create a situation that contains the node down alarms
                .withSituationForNodeDownAlarms(4, "situation#1", 2, 3)
                // Now ACK the situation
                .withAcknowledgmentForSituation(5, "situation#1")
                // now add old un-acknowledged alarm to situation
                .withSituationForNodeDownAlarms(6, "situation#1", 2, 3, 1).build();
        // .withCorrelationAddsAlarm(6, "situation#1", 1).build();
        ScenarioResults results = scenario.play();

        // Verify the set of alarms at various points in time

        // t=0, no alarms
        assertThat(results.getAlarms(0), hasSize(0));
        // t=1, a single problem alarm
        assertThat(results.getAlarms(1), hasSize(1));
        // t=2, two problem alarms
        assertThat(results.getAlarms(2), hasSize(2));
        // t=3, three problem alarms
        assertThat(results.getAlarms(3), hasSize(3));
        // t=4, two problem alarms + 1 situation + 1 other alarm, situation is not acknowledged
        assertThat(results.getAlarms(4), hasSize(4)); // the situation is also
                                                      // an alarm, so it is
                                                      // counted here
        assertThat(results.getSituation(4), not(acknowledged()));

        // t=5, Situation and 2 Alarms should be Ack'd
        assertThat(results.getSituation(5), acknowledged());
        assertThat(results.getAcknowledgedAlarms(5), hasSize(3));
        assertThat(results.getUnAcknowledgedAlarms(5), hasSize(1));
        // t=6, alarm and situation should be unacked
        assertThat(results.getSituation(6), not(acknowledged()));
        assertThat(results.getAlarmAt(6, 1), not(acknowledged()));
        // but other alarm should still be ACK'd
        assertThat(results.getAcknowledgedAlarms(6), hasSize(2));
        // t=∞
        assertThat(results.getAlarmsAtLastKnownTime(), hasSize(0));
    }

}
