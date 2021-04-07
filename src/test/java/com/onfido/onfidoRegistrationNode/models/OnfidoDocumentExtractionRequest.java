package com.onfido.onfidoRegistrationNode.models;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OnfidoDocumentExtractionRequest {
    String document_id;
}
