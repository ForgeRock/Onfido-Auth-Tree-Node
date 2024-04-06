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
        this.sdkConfig = initSdkConfig();
    }

    public JSONObject getSdkConfig() {
        return sdkConfig;
    }

    private JSONObject initSdkConfig() {
        JSONObject sdkConfiguration = new JSONObject();

        try {
            String onModalRequestClose = "function() { onfido.setOptions({isModalOpen: false}); window.location.reload(false); }";
            String onComplete = "function(data) { onfido.setOptions({isModalOpen: false}); document.getElementById('loginButton_0').click(); }";

            sdkConfiguration.put("useModal", true);
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

    private JSONObject initStepsConfig() {
        JSONObject stepsConfig = new JSONObject();

        try {
            ArrayList<String> descriptions = new ArrayList<>();
            descriptions.add(config.onfidoWelcomeMessage());
            descriptions.add(config.onfidoHelpMessage());

            JSONObject options = new JSONObject();
            options.put("title", config.onfidoWelcomeMessage());
            options.put("descriptions", descriptions);
            options.put("forceCrossDevice", false);

            stepsConfig.put("type", "welcome");
            stepsConfig.put("options", options);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return stepsConfig;
    }

    private ArrayList<Object> buildSteps() throws JSONException {
        ArrayList<Object> steps = new ArrayList<>();
        steps.add(initStepsConfig());
        steps.add("document");

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
