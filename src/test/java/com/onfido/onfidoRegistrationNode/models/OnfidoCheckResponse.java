package com.onfido.onfidoRegistrationNode.models;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OnfidoCheckResponse {
    String id;
    String created_at;
    String status;
    String redirect_uri;
    List<String> result;
    Boolean sandbox;
    List<String> tags;
    String results_uri;
    String form_uri;
    Boolean paused;
    String version;
    List<String> report_ids;
    String href;
    String applicant_id;
    Boolean applicant_provides_data;
}
