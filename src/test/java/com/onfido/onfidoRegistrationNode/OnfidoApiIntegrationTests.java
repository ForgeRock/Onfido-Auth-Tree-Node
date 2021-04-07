package com.onfido.onfidoRegistrationNode;

import com.onfido.models.Check;
import com.onfido.onfidoRegistrationNode.mocks.MockOnfidoApiConfiguration;
import com.onfido.onfidoRegistrationNode.mocks.MockOnfidoApiFactory;
import com.onfido.onfidoRegistrationNode.models.OnfidoApplicantRequest;
import com.onfido.onfidoRegistrationNode.models.OnfidoCheckRequest;
import com.onfido.onfidoRegistrationNode.models.OnfidoDocumentExtractionRequest;
import com.onfido.onfidoRegistrationNode.utilities.FileManagement;
import com.onfido.onfidoRegistrationNode.utilities.Files;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.assertj.core.api.SoftAssertions;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.when;

class OnfidoApiIntegrationTests extends ApiIntegrationTest {

    @Test
    void test_createCheck_with_selfie() throws IOException, InterruptedException, NodeProcessException {
        // Setup mock server to capture request send response mocking Onfido's POST /check endpoint
        String response = FileManagement.readFile(Files.CHECK_RESPONSE);
        MockWebServer mockWebServer = mockServerWithResponse(response, 201);

        // Setup mock OnfidoApi with 'user selected' settings
        MockOnfidoApiConfiguration mockOnfidoApiConfiguration =
                MockOnfidoApiConfiguration.builder()
                                          .checkType(onfidoRegistrationNode.biometricCheck.Selfie)
                                          .build();
        OnfidoAPI mockOnfidoAPI = MockOnfidoApiFactory.fakeOnfidoApi(mockOnfidoApiConfiguration, mockWebServer);

        // Test createCheck() method
        when(mockOnfidoAPI.createCheck(any())).thenCallRealMethod();
        Check check = mockOnfidoAPI.createCheck(mockOnfidoApiConfiguration.getApplicantId());

        // Allow mock server to receive request/return response
        RecordedRequest recordedRequest = mockWebServer.takeRequest(10, TimeUnit.SECONDS);

        // Map values in request JSON to Java object
        Moshi moshi = new Moshi.Builder().build();
        JsonAdapter<OnfidoCheckRequest> jsonAdapter = moshi.adapter(OnfidoCheckRequest.class);
        OnfidoCheckRequest onfidoCheckRequest = jsonAdapter.fromJson(recordedRequest.getBody().readUtf8());

        // Verify expectations
        verify(mockOnfidoAPI, times(1)).createCheck(mockOnfidoApiConfiguration.getApplicantId());
        verifyNoMoreInteractions(mockOnfidoAPI);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(onfidoCheckRequest.getApplicant_id()).isEqualTo("<applicant-id>");
            softly.assertThat(onfidoCheckRequest.getReport_names()).containsExactly("document", "facial_similarity_photo");
            softly.assertThat(onfidoCheckRequest.getAsynchronous()).isEqualTo(true);
            softly.assertThat(check.getApplicantId()).isEqualTo("<applicant-id>");
            softly.assertThat(check.getReportIds()).hasSize(2);
        });
    }

    @Test
    void test_createCheck_with_live_video() throws IOException, InterruptedException, NodeProcessException {
        // Setup mock server to capture request send response mocking Onfido's POST /check endpoint
        String response = FileManagement.readFile(Files.CHECK_RESPONSE);
        MockWebServer mockWebServer = mockServerWithResponse(response, 201);

        // Setup mock OnfidoApi with 'user selected' settings
        MockOnfidoApiConfiguration mockOnfidoApiConfiguration =
                MockOnfidoApiConfiguration.builder()
                                          .checkType(onfidoRegistrationNode.biometricCheck.Live)
                                          .build();
        OnfidoAPI mockOnfidoAPI = MockOnfidoApiFactory.fakeOnfidoApi(mockOnfidoApiConfiguration, mockWebServer);

        // Test createCheck() method
        when(mockOnfidoAPI.createCheck(any())).thenCallRealMethod();
        Check check = mockOnfidoAPI.createCheck(mockOnfidoApiConfiguration.getApplicantId());

        // Allow mock server to receive request/return response
        RecordedRequest recordedRequest = mockWebServer.takeRequest(10, TimeUnit.SECONDS);

        // Map values in request JSON to Java object
        Moshi moshi = new Moshi.Builder().build();
        JsonAdapter<OnfidoCheckRequest> jsonAdapter = moshi.adapter(OnfidoCheckRequest.class);
        OnfidoCheckRequest onfidoCheckRequest = jsonAdapter.fromJson(recordedRequest.getBody().readUtf8());

        // Verify expectations
        verify(mockOnfidoAPI, times(1)).createCheck(mockOnfidoApiConfiguration.getApplicantId());
        verifyNoMoreInteractions(mockOnfidoAPI);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(onfidoCheckRequest.getApplicant_id()).isEqualTo("<applicant-id>");
            softly.assertThat(onfidoCheckRequest.getReport_names()).containsExactly("document", "facial_similarity_video");
            softly.assertThat(onfidoCheckRequest.getAsynchronous()).isEqualTo(true);
            softly.assertThat(check.getApplicantId()).isEqualTo("<applicant-id>");
            softly.assertThat(check.getReportIds()).hasSize(2);
        });
    }

    @Test
    void test_createCheck_with_none() throws IOException, InterruptedException, NodeProcessException {
        // Setup mock server to capture request send response mocking Onfido's POST /check endpoint
        String response = FileManagement.readFile(Files.CHECK_RESPONSE);
        MockWebServer mockWebServer = mockServerWithResponse(response, 201);

        // Setup mock OnfidoApi with 'user selected' settings
        MockOnfidoApiConfiguration mockOnfidoApiConfiguration =
                MockOnfidoApiConfiguration.builder()
                                          .checkType(onfidoRegistrationNode.biometricCheck.None)
                                          .build();
        OnfidoAPI mockOnfidoAPI = MockOnfidoApiFactory.fakeOnfidoApi(mockOnfidoApiConfiguration, mockWebServer);

        // Test createCheck() method
        when(mockOnfidoAPI.createCheck(any())).thenCallRealMethod();
        Check check = mockOnfidoAPI.createCheck(mockOnfidoApiConfiguration.getApplicantId());

        // Allow mock server to receive request/return response
        RecordedRequest recordedRequest = mockWebServer.takeRequest(10, TimeUnit.SECONDS);

        // Map values in request JSON to Java object
        Moshi moshi = new Moshi.Builder().build();
        JsonAdapter<OnfidoCheckRequest> jsonAdapter = moshi.adapter(OnfidoCheckRequest.class);
        OnfidoCheckRequest onfidoCheckRequest = jsonAdapter.fromJson(recordedRequest.getBody().readUtf8());

        // Verify expectations
        verify(mockOnfidoAPI, times(1)).createCheck(mockOnfidoApiConfiguration.getApplicantId());
        verifyNoMoreInteractions(mockOnfidoAPI);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(onfidoCheckRequest.getApplicant_id()).isEqualTo("<applicant-id>");
            softly.assertThat(onfidoCheckRequest.getReport_names()).containsExactly("document");
            softly.assertThat(onfidoCheckRequest.getAsynchronous()).isEqualTo(true);
            softly.assertThat(check.getApplicantId()).isEqualTo("<applicant-id>");
            softly.assertThat(check.getReportIds()).hasSize(2);
        });
    }

    @Test
    void test_createApplicant_success() throws IOException, InterruptedException, NodeProcessException {
        String response = FileManagement.readFile(Files.APPLICANT_RESPONSE);
        MockWebServer mockWebServer = mockServerWithResponse(response, 201);
        MockOnfidoApiConfiguration mockOnfidoApiConfiguration =
                MockOnfidoApiConfiguration.builder().build();
        OnfidoAPI mockOnfidoAPI = MockOnfidoApiFactory.fakeOnfidoApi(mockOnfidoApiConfiguration, mockWebServer);

        when(mockOnfidoAPI.createApplicant()).thenCallRealMethod();
        mockOnfidoAPI.createApplicant();

        RecordedRequest recordedRequest = mockWebServer.takeRequest(10, TimeUnit.SECONDS);

        Moshi moshi = new Moshi.Builder().build();
        JsonAdapter<OnfidoApplicantRequest> jsonAdapter = moshi.adapter(OnfidoApplicantRequest.class);
        OnfidoApplicantRequest onfidoApplicantRequest = jsonAdapter.fromJson(recordedRequest.getBody().readUtf8());

        verify(mockOnfidoAPI, times(1)).createApplicant();
        verifyNoMoreInteractions(mockOnfidoAPI);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(onfidoApplicantRequest.getFirst_name()).isEqualTo("anonymous");
            softly.assertThat(onfidoApplicantRequest.getLast_name()).isEqualTo("anonymous");
        });
    }

    @Test
    void test_createApplicant_failure() throws IOException, InterruptedException, NodeProcessException {
        MockWebServer mockWebServer = mockServerWithErrorCode();

        MockOnfidoApiConfiguration mockOnfidoApiConfiguration =
                MockOnfidoApiConfiguration.builder().build();
        OnfidoAPI mockOnfidoAPI = MockOnfidoApiFactory.fakeOnfidoApi(mockOnfidoApiConfiguration, mockWebServer);

        when(mockOnfidoAPI.createApplicant()).thenCallRealMethod();

        Throwable thrown = catchThrowable(() -> {
            mockOnfidoAPI.createApplicant();
        });
        mockWebServer.takeRequest(10, TimeUnit.SECONDS);

        verify(mockOnfidoAPI, times(1)).createApplicant();
        verifyNoMoreInteractions(mockOnfidoAPI);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(thrown).isInstanceOf(NodeProcessException.class).hasMessageContaining("ApiException");
        });
    }

    @Test
    void test_getOcrResult() throws IOException, InterruptedException, NodeProcessException {
        String documentId = "<document-id>";

        // Setup mock server to capture request send response mocking Onfido's POST /check endpoint
        String response = FileManagement.readFile(Files.DOCUMENT_EXTRACTION_RESPONSE);
        MockWebServer mockWebServer = mockServerWithResponse(response, 200);

        // Configure mock OnfidoApi with 'user selected' settings
        MockOnfidoApiConfiguration mockOnfidoApiConfiguration =
                MockOnfidoApiConfiguration.builder().build();
        OnfidoAPI mockOnfidoAPI = MockOnfidoApiFactory.fakeOnfidoApi(mockOnfidoApiConfiguration, mockWebServer);

        // Test getOcrResults() method
        when(mockOnfidoAPI.getOcrResults(any())).thenCallRealMethod();
        UserData ocrResults = mockOnfidoAPI.getOcrResults(documentId);

        // Allow mock server to receive request/return response
        RecordedRequest recordedRequest = mockWebServer.takeRequest(10, TimeUnit.SECONDS);

        // Map values in request JSON to Java object
        Moshi moshi = new Moshi.Builder().build();
        JsonAdapter<OnfidoDocumentExtractionRequest> jsonAdapter = moshi.adapter(OnfidoDocumentExtractionRequest.class);
        OnfidoDocumentExtractionRequest onfidoDocumentExtractionRequest = jsonAdapter.fromJson(recordedRequest.getBody().readUtf8());

        // Verify expectations
        verify(mockOnfidoAPI, times(1)).getOcrResults(documentId);
        verifyNoMoreInteractions(mockOnfidoAPI);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(ocrResults.getAddressLine1()).isEqualTo("1600 Pennsylvania Ave");
            softly.assertThat(ocrResults.getAddressLine2()).isEqualTo("Washington, DC 20500");
            softly.assertThat(ocrResults.getAddressLine3()).isEqualTo(null);
            softly.assertThat(ocrResults.getAddressLine4()).isEqualTo(null);
            softly.assertThat(ocrResults.getAddressLine5()).isEqualTo(null);
            softly.assertThat(ocrResults.getDateOfBirth()).isEqualTo("1970-01-01");
            softly.assertThat(ocrResults.getDateOfExpiry()).isEqualTo("2050-01-01");
            softly.assertThat(ocrResults.getDocumentNumber()).isEqualTo("000000000");
            softly.assertThat(ocrResults.getFirstName()).isEqualTo("<first-name>");
            softly.assertThat(ocrResults.getGender()).isEqualTo("Male");
            softly.assertThat(ocrResults.getIssuingCountry()).isEqualTo("USA");
            softly.assertThat(ocrResults.getIssuingState()).isEqualTo("DC");
            softly.assertThat(ocrResults.getLastName()).isEqualTo("<last-name>");
            softly.assertThat(ocrResults.getMrzLine1()).isEqualTo(null);
            softly.assertThat(ocrResults.getMrzLine2()).isEqualTo(null);
            softly.assertThat(ocrResults.getMrzLine3()).isEqualTo(null);
            softly.assertThat(onfidoDocumentExtractionRequest.getDocument_id()).isEqualTo("<document-id>");
            softly.assertThat(recordedRequest.getPath()).contains("/extractions/");
        });
    }

    @Test
    public void test_getOcrResult_failure() throws IOException, InterruptedException, NodeProcessException {
        String documentId = "<document-id>";

        MockWebServer mockWebServer = mockServerWithErrorCode(403);
        MockOnfidoApiConfiguration mockOnfidoApiConfiguration =
                MockOnfidoApiConfiguration.builder().build();
        OnfidoAPI mockOnfidoAPI = MockOnfidoApiFactory.fakeOnfidoApi(mockOnfidoApiConfiguration, mockWebServer);

        when(mockOnfidoAPI.getOcrResults(documentId)).thenCallRealMethod();

        Throwable thrown = catchThrowable(() -> {
            mockOnfidoAPI.getOcrResults(documentId);
        });
        mockWebServer.takeRequest(10, TimeUnit.SECONDS);

        verify(mockOnfidoAPI, times(1)).getOcrResults(documentId);
        verifyNoMoreInteractions(mockOnfidoAPI);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(thrown)
                  .isInstanceOf(NodeProcessException.class)
                  .hasMessage("com.onfido.exceptions.ApiException: Unknown error (status code 403)");
        });
    }
}
