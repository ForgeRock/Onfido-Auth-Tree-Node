package com.onfido.onfidoRegistrationNode.models;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponseAddress {
    String flat_number;
    String building_number;
    String building_name;
    String street;
    String sub_street;
    String town;
    String postcode;
    String country;
    String line1;
    String line2;
    String line3;
}
