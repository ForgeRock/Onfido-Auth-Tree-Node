package com.onfido.onfidoRegistrationNode;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONString;

import java.util.ArrayList;

public class OnfidoWebSdkConfig {
    private final onfidoRegistrationNode.Config config;
    private final JSONObject sdkConfig;
    private final String sdkToken;

    public OnfidoWebSdkConfig(onfidoRegistrationNode.Config config, String sdkToken) {
        this.config = config;
        this.sdkToken = sdkToken;
        this.sdkConfig = initSdkConfig(this.config);
    }

    public JSONObject getSdkConfig() {
        return sdkConfig;
    }

    private JSONObject initSdkConfig(onfidoRegistrationNode.Config config) {
        JSONObject sdkConfiguration = new JSONObject();

        try {
            String onModalRequestClose = "function() { onfido.setOptions({isModalOpen: false}); window.location.reload(false); }";
            String onComplete = "function(data) { onfido.setOptions({isModalOpen: false}); document.getElementById('loginButton_0').click(); }";

            sdkConfiguration.put("useModal", config.onfidoUseModal());
            sdkConfiguration.put("customUI", config.onfidoCustomUI());
            sdkConfiguration.put("isModalOpen", true);

            sdkConfiguration.put("onModalRequestClose", new OnfidoWebSdkConfig.JSONFunction(onModalRequestClose));
            sdkConfiguration.put("onComplete", new OnfidoWebSdkConfig.JSONFunction(onComplete));

            sdkConfiguration.put("steps", buildSteps());
            sdkConfiguration.put("token", sdkToken);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return sdkConfiguration;
    }

    private JSONObject welcomeStepConfig() {
        JSONObject step = new JSONObject();
        try {
            ArrayList<String> descriptions = new ArrayList<>();
            descriptions.add(config.onfidoWelcomeMessage());
            descriptions.add(config.onfidoHelpMessage());

            JSONObject options = new JSONObject();
            options.put("title", config.onfidoWelcomeMessage());
            options.put("descriptions", descriptions);
            options.put("forceCrossDevice", false);
            step.put("type", "welcome");
            step.put("options", options);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return step;
    }

    private JSONObject documentStepsConfig() {
        JSONObject step = new JSONObject();

        try {
            JSONObject options = new JSONObject();
            JSONObject docTypes = new JSONObject();
            if (config.onfidoDocumentCountry() != "") {
                JSONObject country = new JSONObject();
                country.put("country", config.onfidoDocumentCountry());
                for (String docType : config.onfidoDocumentTypes()) {
                    docTypes.put(docType, country);
                }
                options.put("documentTypes", docTypes);
                step.put("options", options);
            } else {
                options.put("documentTypes", config.onfidoDocumentTypes());
                step.put("options", options);
            }
            step.put("type", "document");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return step;
    }
    

    private ArrayList<Object> buildSteps() throws JSONException {
        ArrayList<Object> steps = new ArrayList<>();
        if (config.onfidoShowWelcome()) {
            steps.add(welcomeStepConfig());
        }
        if (config.onfidoDocumentTypes() != null) {
            steps.add(documentStepsConfig());
        } else {
            steps.add("document");
        }

        if (!config.biometricCheck().toString().equals("None")) {
            if (config.biometricCheck().toString().equals("Live")) {
                JSONObject faceOptions = new JSONObject();
                faceOptions.put("type", "face");
                faceOptions.put("options", new JSONObject().put("requestedVariant", "video"));
                steps.add(faceOptions);
            } else {
                steps.add("face");
            }
        }

        return steps;
    }

    //class that helps with passing JSONFunctions as value in JSONObject
    public static class JSONFunction implements JSONString {

        private String string;

        JSONFunction(String string) {
            this.string = string;
        }

        @Override
        public String toJSONString() {
            return string;
        }
    }
}
