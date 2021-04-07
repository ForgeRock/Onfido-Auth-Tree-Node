package com.onfido.onfidoRegistrationNode;

import com.onfido.exceptions.OnfidoException;
import com.onfido.models.Address;
import com.onfido.models.Applicant;
import com.onfido.models.Document;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.forgerock.openam.auth.node.api.NodeProcessException;

import java.time.LocalDate;
import java.util.List;

@Slf4j(topic="amAuth")
class onfidoAutoFill {
    private String backId = null;
    private String frontId = null;
    private final OnfidoAPI onfidoApi;

    public onfidoAutoFill(OnfidoAPI onfidoApi) {
        this.onfidoApi = onfidoApi;
    }


    UserData getIdAttributes(String applicantId) throws NodeProcessException {
        List<Document> documents = onfidoApi.listDocuments(applicantId);

        for (Document currentDocument : documents) {
            if (currentDocument.getSide().equals("back")) {
                backId = currentDocument.getId();
            } else {
                frontId = currentDocument.getId();
            }
        }

        if (backId != null) {
            UserData ocrBackResponse = this.onfidoApi.getOcrResults(backId);

            if (ocrBackResponse != null) {
                return ocrBackResponse;
            }
        }

        if (frontId != null) {
            UserData ocrFrontResponse = this.onfidoApi.getOcrResults(frontId);

            if (ocrFrontResponse != null) {
                return ocrFrontResponse;
            }
        }

        log.error("Unable to get json attributes from Onfido Service");

        return null;
    }

    void populateOnfidoApplicant(String applicantId, UserData documentAttributes) {

        Applicant.Request applicantRequest = Applicant.request();

        setApplicantName(applicantRequest, documentAttributes);
        setApplicantAddress(applicantRequest, documentAttributes);
        setApplicantDob(applicantRequest, documentAttributes);

        try {
            Applicant result = onfidoApi.updateApplicant(applicantId, applicantRequest);
            log.debug("Updating applicant result is: {}", result);
        } catch (OnfidoException e) {
            log.error("ERROR: Could not update applicant: {}", e.getMessage());
        }
    }

    // Helpers to populate an Applicant object from document information
    private void setApplicantName(Applicant.Request applicantRequest, UserData documentAttributes) {
        if (!StringUtils.isEmpty((documentAttributes.getFirstName()))) {
            applicantRequest.firstName((documentAttributes.getFirstName()));
        }

        if (!StringUtils.isEmpty((documentAttributes.getLastName()))) {
            applicantRequest.lastName(documentAttributes.getLastName());
        }
    }

    private void setApplicantAddress(Applicant.Request applicantRequest, UserData documentAttributes) {
        Address.Request addressRequest = Address.request();
        String addressLine1 = documentAttributes.getAddressLine1();
        boolean isAddressSet = false;

        if (!StringUtils.isEmpty((addressLine1))) {
            addressRequest.buildingNumber((addressLine1.replaceAll("[a-zA-Z]+", "").trim()));
            addressRequest.street(addressLine1.replaceAll("^[0-9]+", "").trim());
            isAddressSet = true;
        }

        if (!StringUtils.isEmpty((documentAttributes.getAddressLine2()))) {
            addressRequest.town(documentAttributes.getAddressLine2());
            isAddressSet = true;
        }

        if (!StringUtils.isEmpty((documentAttributes.getAddressLine3()))) {
            addressRequest.postcode(documentAttributes.getAddressLine3());
            isAddressSet = true;
        }

        if (isAddressSet) {
            applicantRequest.address(addressRequest);
        }

        if (!StringUtils.isEmpty((documentAttributes.getIssuingState()))) {
            addressRequest.state(documentAttributes.getIssuingState());
        }

        if (!StringUtils.isEmpty((documentAttributes.getIssuingCountry()))) {
            addressRequest.country(documentAttributes.getIssuingCountry());
        }
    }

    private void setApplicantDob(Applicant.Request applicantRequest, UserData documentAttributes) {
        if (documentAttributes.getDateOfBirth() != null) {
            applicantRequest.dob(documentAttributes.getDateOfBirth());
        }
    }
}
