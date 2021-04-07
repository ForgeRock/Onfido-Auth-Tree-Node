package com.onfido.onfidoRegistrationNode.models;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OnfidoCheckRequest {
    String applicant_id;
    List<String> report_names;
    Boolean asynchronous;
}
