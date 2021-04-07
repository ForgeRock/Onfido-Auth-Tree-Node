package com.onfido.onfidoRegistrationNode;

import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class OnfidoHelperTests {

    onfidoHelper testOnfidoHelper = new onfidoHelper();

    @Test
    void test_extracting_applicant_id_from_uri() throws NodeProcessException {
        String applicantId = "9a979d47-e158-4292-a30c-d9507e1c92af";
        String uri = "https://api.onfido.com/v2/applicants/9a979d47-e158-4292-a30c-d9507e1c92af/checks/563d5862-dd5a-4d64-955d-7caa21e6431d";

        assertThat(testOnfidoHelper.getApplicantId(uri))
                .isEqualToIgnoringCase(applicantId);
    }

    @Test
    void test_failure_extracting_applicant_id_from_uri_throws_NodeProcessException() {
        String uri = "badstring";

        Throwable thrown = catchThrowable(() -> {
            testOnfidoHelper.getApplicantId(uri);
        });

        assertThat(thrown)
                .isInstanceOf(NodeProcessException.class)
                .hasMessageContaining("Unable to extract applicantId");
    }
}
