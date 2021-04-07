package com.onfido.onfidoRegistrationNode.models.onfidoExtractionResponse;

import lombok.Builder;
import lombok.Data;


@Builder
@Data
public class OnfidoDocumentExtractionResponse {
    String document_id;
    DocumentClassification document_classification;
    ExtractedData extractedData;
}

