package com.onfido.onfidoRegistrationNode;

import com.google.common.collect.Lists;
import com.onfido.Onfido;
import com.onfido.exceptions.OnfidoException;
import com.onfido.models.Applicant;
import com.onfido.models.Check;
import com.onfido.models.Document;
import com.onfido.models.Extraction;
import com.onfido.models.Report;
import com.onfido.models.SdkToken;
import lombok.extern.slf4j.Slf4j;
import org.forgerock.openam.auth.node.api.NodeProcessException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Slf4j(topic="amAuth")
public class OnfidoAPI {
    private static final String DEFAULT_FIRST_NAME = "anonymous";
    private static final String DEFAULT_LAST_NAME = "anonymous";

    private final onfidoRegistrationNode.Config registrationConfig;
    private final onfidoWebhookNode.Config webhookConfig;
    private final Onfido onfido;

    public OnfidoAPI(onfidoRegistrationNode.Config config) throws NodeProcessException {
        log.debug("initializing OnfidoAPI: {}", config);

        this.registrationConfig = config;
        this.webhookConfig = null;

        this.onfido = Onfido.builder()
                            .apiToken(getApiToken())
                            .unknownApiUrl(getBaseUrl())
                            .build();
    }

    public OnfidoAPI(onfidoWebhookNode.Config config) throws NodeProcessException {
        log.debug("initializing OnfidoAPI: {}", config);

        this.registrationConfig = null;
        this.webhookConfig = config;

        this.onfido = Onfido.builder()
                            .apiToken(getApiToken())
                            .unknownApiUrl(getBaseUrl())
                            .build();
    }

    public Applicant createApplicant() throws NodeProcessException {

        Applicant.Request applicantRequest = null;
        try {
            applicantRequest = Applicant.request().firstName(DEFAULT_FIRST_NAME).lastName(DEFAULT_LAST_NAME);

            return onfido.applicant.create(applicantRequest);
        } catch (OnfidoException e) {
            log.error("Exception creating the applicant: {}", applicantRequest);
            throw new NodeProcessException(e);
        }
    }

    public Check createCheck(String applicantId) throws NodeProcessException {
        log.debug("Creating check for applicant: {}", applicantId);

        log.debug("START: {}", new Timestamp(System.currentTimeMillis()));

        // Create check configuration
        Check.Request checkRequest = Check.request()
                                          .applicantId(applicantId)
                                          .reportNames(getReportTypes())
                                          .asynchronous(true);

        log.debug("Submitting check request: {}", checkRequest);

        // Create check
        try {
            Check check = onfido.check.create(checkRequest);
            log.debug("Check: {}", check);

            return check;
        } catch (OnfidoException e) {
            log.error("Exception creating the check");

            throw new NodeProcessException(e);
        } finally {
            log.debug("END: {}", new Timestamp(System.currentTimeMillis()));
        }
    }

    public String requestSdkToken(Applicant applicant) throws NodeProcessException {
        log.debug("Creating SDK token for applicant: {}", applicant.getId());
        SdkToken.Request sdkTokenRequest = SdkToken.request().applicantId(applicant.getId()).referrer(getReferrer());
        try {
            return onfido.sdkToken.generate(sdkTokenRequest);
        } catch (OnfidoException e) {
            log.error("Exception creating the SDKToken: {}", sdkTokenRequest);
            throw new NodeProcessException(e);
        }
    }

    public List<Document> listDocuments(String applicantId) throws NodeProcessException {
        log.debug("Obtaining document list for applicant: {}", applicantId);
        try {
            return onfido.document.list(applicantId);
        } catch (OnfidoException e) {
            log.error("Failed to list documents for: {}", applicantId);
            throw new NodeProcessException(e);
        }
    }

    public List<Report> listReports(String checkId) throws NodeProcessException {
        log.debug("Obtaining document list for applicant: {}", checkId);
        try {
            return onfido.report.list(checkId);
        } catch (OnfidoException e) {
            log.error("Failed to list documents for: {}", checkId);
            throw new NodeProcessException(e);
        }
    }

    public Applicant updateApplicant(String applicantId, Applicant.Request applicantRequest) throws OnfidoException {
        log.debug("Updating information for applicant: {}", applicantId);
        log.debug("Changes: {}", applicantRequest);
        try {
            return onfido.applicant.update(applicantId, applicantRequest);
        } catch (OnfidoException e) {
            log.error("Exception updating the applicant: {}", applicantRequest);
            throw (e);
        }
    }

    public UserData getOcrResults(String documentId) throws NodeProcessException {
        log.debug("Fetching extracted data for document: {}", documentId);

        try {
            Extraction extraction = onfido.extraction.perform(documentId);

            return UserData.fromOcrExtraction(extraction);
        } catch (OnfidoException e) {
            log.error("Exception extracting data for document: {}", documentId);

            throw new NodeProcessException(e);
        }
    }

    private String getApiToken() throws NodeProcessException {
        if (registrationConfig != null) {
            return new String(registrationConfig.onfidoToken());
        } else if (webhookConfig != null){
            return new String(webhookConfig.onfidoToken());
        }

        throw new NodeProcessException("Onfido API Token not configured");
    }

    private String getBaseUrl() throws NodeProcessException {
        if (registrationConfig != null) {
            return registrationConfig.onfidoApiBaseUrl();
        } else if (webhookConfig != null) {
            return webhookConfig.onfidoApiBaseUrl();
        }

        throw new NodeProcessException("Onfido API Base URL not configured");
    }

    private List<String> getReportTypes() throws NodeProcessException {
        if (null == registrationConfig) {
            throw new NodeProcessException("Registration Configuration not initialized");
        }

        ArrayList<String> reports = Lists.newArrayList();
        
        switch (registrationConfig.biometricCheck()) {
            case None:
                reports.add(ReportNames.DOCUMENT.toString());
                break;

            case Live:
                reports.add(ReportNames.DOCUMENT.toString());
                reports.add(ReportNames.FACIAL_SIMILARITY_VIDEO.toString());
                break;

            case Selfie:
                reports.add(ReportNames.DOCUMENT.toString());
                reports.add(ReportNames.FACIAL_SIMILARITY_PHOTO.toString());
                break;

            default:
                throw new NodeProcessException("Unknown Report Type selected");
        }

        return reports;
    }

    private String getReferrer() throws NodeProcessException {
        if (null == registrationConfig) {
            throw new NodeProcessException("Registration Configuration not initialized");
        }

        return registrationConfig.onfidoJWTreferrer();
    }
}
