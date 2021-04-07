package com.onfido.onfidoRegistrationNode.models.onfidoExtractionResponse;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class DocumentClassification {
    String version;
    String subtype;
    String issuing_country;
    String document_type;
    String issuing_state;
}
