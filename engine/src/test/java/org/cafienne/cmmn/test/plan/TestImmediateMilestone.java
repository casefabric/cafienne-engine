package org.cafienne.cmmn.test.plan;

import org.cafienne.cmmn.actorapi.command.StartCase;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.test.TestScript;
import org.cafienne.cmmn.test.assertions.CaseAssertion;
import org.cafienne.json.ValueMap;
import org.junit.Test;

import static org.cafienne.cmmn.test.TestScript.*;

public class TestImmediateMilestone {
    private final CaseDefinition definitions = loadCaseDefinition("testdefinition/milestonedependency.xml");

    @Test
    public void testImmediateMilestone() {
        String caseInstanceId = "MilestoneDependencyTest";
        TestScript testCase = new TestScript("MilestoneDependencyTest");
        ValueMap greeting = new ValueMap();

        StartCase startCase = createCaseCommand(testUser, caseInstanceId, definitions, greeting);
        testCase.addStep(startCase, CaseAssertion::print);

        testCase.runTest();
    }
}
