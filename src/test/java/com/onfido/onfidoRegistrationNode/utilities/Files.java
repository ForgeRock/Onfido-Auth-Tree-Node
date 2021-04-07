package com.onfido.onfidoRegistrationNode.utilities;

import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public enum Files{

    APPLICANT_RESPONSE("src/test/resources/stubs/onfidoApi/createApplicantResponse.json"),
    CHECK_RESPONSE("src/test/resources/stubs/onfidoApi/createCheckResponse.json"),
    DOCUMENT_EXTRACTION_RESPONSE("src/test/resources/stubs/onfidoApi/documentExtractionResponse.json");

    private String path;

    Files(String path){
        this.path = path;
        assertTrue("File not found: " + path, java.nio.file.Files.exists(Paths.get(path)));
    }

    public String getPath(){
        return path;
    }
}
