package com.onfido.onfidoRegistrationNode.models;

import com.onfido.onfidoRegistrationNode.models.ResponseAddress;
import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.List;


@Data
@Builder
public class OnfidoApplicantResponse {
    String id;
    String created_at;
    Boolean sandbox;
    String first_name;
    String last_name;
    String dob;
    String email;
    String delete_at;
    String href;
    ResponseAddress address;
    List<String> id_numbers;
}

