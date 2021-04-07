package com.onfido.onfidoRegistrationNode.mocks;

import com.onfido.onfidoRegistrationNode.onfidoRegistrationNode;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MockOnfidoApiConfiguration {
    @Builder.Default
    String applicantId = "<applicant-id>";

    @Builder.Default
    String token = "token";

    @Builder.Default
    onfidoRegistrationNode.biometricCheck checkType = onfidoRegistrationNode.biometricCheck.None;
}
