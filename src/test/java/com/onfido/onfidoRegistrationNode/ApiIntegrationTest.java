package com.onfido.onfidoRegistrationNode;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;


public class ApiIntegrationTest {
    private static MockWebServer server;

    @BeforeAll
    public static void clearServer() {
        server = null;
    }

    @AfterAll
    public static void shutdownServer() throws IOException {
        if (server != null) {
            server.shutdown();
        }
    }

    public MockWebServer mockServerWithResponse(String response, Integer code) throws IOException {
        server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(response).setResponseCode(code));
        server.start();

        return server;
    }
    public MockWebServer mockServerWithResponse(String response) throws IOException {
        return mockServerWithResponse(response, 200);
    }

    public MockWebServer mockServerWithFileResponse(String content, String type) throws IOException {
        server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(content).addHeader("content-type", type));
        server.start();

        return server;
    }

    public MockWebServer mockServerWithParameters(String response, Integer code) throws IOException {
        server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(response).setResponseCode(code));
        server.start();

        return server;
    }

    public MockWebServer mockServerWithErrorResponse(String response) throws IOException {
        server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(response).setResponseCode(403));
        server.start();

        return server;
    }
    public MockWebServer mockServerWithErrorCode(int code) throws IOException {
        server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(code));
        server.start();

        return server;
    }
    public MockWebServer mockServerWithErrorCode() throws IOException {
        server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(403));
        server.start();

        return server;
    }
}
