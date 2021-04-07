package com.onfido.onfidoRegistrationNode.models.onfidoExtractionResponse;

import com.squareup.moshi.Json;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ExtractedData {
    String address_line_1;
    String address_line_2;
    @Json(name = "class") //class is a java reserved keyword
            String class_of_driver;
    String date_of_birth;
    String document_number;
    String date_of_expiry;
    String first_name;
    String gender;
    String issuing_date;
    String last_name;
}
