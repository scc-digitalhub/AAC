package it.smartcommunitylab.aac.claims;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class TestGraalExecutionService {

    /**
     * This test checks that valid comments do not interfere with script execution
     * when option "removeComments" is enabled (hence true)
     * @throws Exception if either execution fails or if the result does not match expected result
     */
    @Test
    public void testEnableRemoveCommentsInExecutionService() throws Exception {
        boolean EnableRemoveCommentConfiguration = true;
        LocalGraalExecutionService executionService = new LocalGraalExecutionService();
        executionService.setRemoveComments(EnableRemoveCommentConfiguration);
        Map<String, Serializable> funcInput = new HashMap<>();
        Map<String, Serializable> expectedResult = new HashMap<>() {
            {
                put("key_1", "value_1");
                put("key_2", "value_2");
            }
        };

        String funcName = "testFunction";
        String funcContent =
            """
            function testFunction(attributes) {
                // comment before any instruction
                attributes['key_1'] = 'value_1';
                // comment in the middle of instructions
                attributes['key_2'] = 'value_2';
                // comment after last (non return) instruction
                return attributes;
                // comment after last instruction
            }
            """;
        Map<String, Serializable> obtainedResult = new HashMap<>();
        try {
            obtainedResult = executionService.executeFunction(funcName, funcContent, funcInput);
        } catch (Exception e) {
            // ios exception, test
            fail("test failed due to unexpected exception when running execution service", e);
        }
        assertThat(expectedResult.equals(obtainedResult));
    }

    /**
     * This test checks that valid comments do not interfere with script execution
     * when option "removeComments" is disable (hence false)
     * @throws Exception if either execution fails or if the result does not match expected result
     */
    @Test
    public void testDisableRemoveCommentsInExecutionService() throws Exception {
        boolean DisableRemoveCommentConfiguration = true;
        LocalGraalExecutionService executionService = new LocalGraalExecutionService();
        executionService.setRemoveComments(DisableRemoveCommentConfiguration);
        Map<String, Serializable> funcInput = new HashMap<>() {
            {
                put("http://input_key_0_with_comment.example", "value_0");
            }
        };
        Map<String, Serializable> expectedResult = new HashMap<>() {
            {
                put("http://input_key_0_with_comment.example", "value_0");
                put("http://key_1_with_comment.example", "value_1");
                put("http://key_2_with_comment.example", "value_2");
            }
        };

        String funcName = "testFunction";
        String funcContent =
            """
            function testFunction(attributes) {
                // malformed comment before any instruction '//
                attributes['http://key_1_with_comment.example'] = 'value_1';
                // malformed comment in the middle of instructions '//''
                attributes['http://key_2_with_comment.example'] = 'value_2';
                // malformed comment after last (non return) instruction ''//'
                return attributes;
                // malformed comment after last instruction ''//''
            }
            """;
        Map<String, Serializable> obtainedResult = new HashMap<>();
        try {
            obtainedResult = executionService.executeFunction(funcName, funcContent, funcInput);
        } catch (Exception e) {
            // ios exception, test
            fail("test failed due to unexpected exception when running execution service", e);
        }
        assertThat(expectedResult.equals(obtainedResult));
    }
}
