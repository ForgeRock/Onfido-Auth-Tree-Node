package com.onfido.onfidoRegistrationNode;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.onfido.Onfido;
import com.onfido.exceptions.OnfidoException;
import com.onfido.models.Applicant;
import com.onfido.models.Check;
import com.onfido.models.Document;
import com.onfido.models.Extraction;
import com.onfido.models.Report;
import com.onfido.models.SdkToken;



public class OnfidoAPI {
    protected static final String DEFAULT_FIRST_NAME = "anonymous";
    protected static final String DEFAULT_LAST_NAME = "anonymous";

    private final onfidoRegistrationNode.Config registrationConfig;

    private final Onfido onfido;
    private String loggerPrefix = "[OnfidoAPI]" + onfidoRegistrationNodePlugin.logAppender;
    private static final Logger log = LoggerFactory.getLogger(OnfidoAPI.class);    

    public OnfidoAPI(onfidoRegistrationNode.Config config) throws NodeProcessException {
        log.debug(loggerPrefix + "initializing OnfidoAPI: {}", config);

        this.registrationConfig = config;


        this.onfido = Onfido.builder()
                            .apiToken(getApiToken())
                            .unknownApiUrl(getBaseUrl())
                            .build();
    }



    public Applicant createApplicant(String first_name, String last_name) throws NodeProcessException {

        Applicant.Request applicantRequest = null;
        try {
            applicantRequest = Applicant.request().firstName(first_name).lastName(last_name);

            return onfido.applicant.create(applicantRequest);
        } catch (OnfidoException e) {
            log.error(loggerPrefix + "Exception creating the applicant: {}", applicantRequest);
            throw new NodeProcessException(e);
        }
    }

    public Check createCheck(String applicantId) throws NodeProcessException {
        log.debug(loggerPrefix + "Creating check for applicant: {}", applicantId);

        log.debug(loggerPrefix + "START: {}", new Timestamp(System.currentTimeMillis()));

        // Create check configuration
        Check.Request checkRequest = Check.request()
                                          .applicantId(applicantId)
                                          .reportNames(getReportTypes())
                                          .asynchronous(true);

        log.debug(loggerPrefix + "Submitting check request: {}", checkRequest);

        // Create check
        try {
            Check check = onfido.check.create(checkRequest);
            log.debug(loggerPrefix + "Check: {}", check);

            return check;
        } catch (OnfidoException e) {
            log.error(loggerPrefix + "Exception creating the check");

            throw new NodeProcessException(e);
        } finally {
            log.debug(loggerPrefix + "END: {}", new Timestamp(System.currentTimeMillis()));
        }
    }

    public String requestSdkToken(Applicant applicant) throws NodeProcessException {
        log.debug(loggerPrefix + "Creating SDK token for applicant: {}", applicant.getId());
        SdkToken.Request sdkTokenRequest = SdkToken.request().applicantId(applicant.getId()).referrer(getReferrer());
        try {
            return onfido.sdkToken.generate(sdkTokenRequest);
        } catch (OnfidoException e) {
            log.error(loggerPrefix + "Exception creating the SDKToken: {}", sdkTokenRequest);
            throw new NodeProcessException(e);
        }
    }

    public List<Document> listDocuments(String applicantId) throws NodeProcessException {
        log.debug(loggerPrefix + "Obtaining document list for applicant: {}", applicantId);
        try {
            return onfido.document.list(applicantId);
        } catch (OnfidoException e) {
            log.error(loggerPrefix + "Failed to list documents for: {}", applicantId);
            throw new NodeProcessException(e);
        }
    }

    public List<Report> listReports(String checkId) throws NodeProcessException {
        log.debug(loggerPrefix + "Obtaining document list for applicant: {}", checkId);
        try {
            return onfido.report.list(checkId);
        } catch (OnfidoException e) {
            log.error(loggerPrefix + "Failed to list documents for: {}", checkId);
            throw new NodeProcessException(e);
        }
    }

    public Applicant updateApplicant(String applicantId, Applicant.Request applicantRequest) throws OnfidoException {
        log.debug(loggerPrefix + "Updating information for applicant: {}", applicantId);
        log.debug(loggerPrefix + "Changes: {}", applicantRequest);
        try {
            return onfido.applicant.update(applicantId, applicantRequest);
        } catch (OnfidoException e) {
            log.error(loggerPrefix + "Exception updating the applicant: {}", applicantRequest);
            throw (e);
        }
    }

    public UserData getOcrResults(String documentId) throws NodeProcessException {
        log.debug(loggerPrefix + "Fetching extracted data for document: {}", documentId);

        try {
            Extraction extraction = onfido.extraction.perform(documentId);

            return UserData.fromOcrExtraction(extraction);
        } catch (OnfidoException e) {
            log.error(loggerPrefix + "Exception extracting data for document: {}", documentId);

            throw new NodeProcessException(e);
        }
    }

    private String getApiToken() throws NodeProcessException {
        if (registrationConfig != null) {
            return new String(registrationConfig.onfidoToken());
        } 

        throw new NodeProcessException("Onfido API Token not configured");
    }

    private String getBaseUrl() throws NodeProcessException {
        if (registrationConfig != null) {
            return registrationConfig.onfidoApiBaseUrl();
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
