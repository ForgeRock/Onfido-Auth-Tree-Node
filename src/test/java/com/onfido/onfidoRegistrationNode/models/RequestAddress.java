package com.onfido.onfidoRegistrationNode.models;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RequestAddress {
    String street;
    String town;
    String postcode;
    String country;
}
