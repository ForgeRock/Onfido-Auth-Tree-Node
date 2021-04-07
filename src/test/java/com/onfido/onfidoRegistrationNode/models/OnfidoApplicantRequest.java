package com.onfido.onfidoRegistrationNode.models;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OnfidoApplicantRequest {

    String first_name;
    String last_name;
    String dob;
    String email;
    RequestAddress address;
}

