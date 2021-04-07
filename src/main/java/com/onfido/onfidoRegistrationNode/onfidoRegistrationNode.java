/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2017-2018 ForgeRock AS.
 */


package com.onfido.onfidoRegistrationNode;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOTokenManager;
import com.onfido.models.Applicant;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import lombok.extern.slf4j.Slf4j;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.SharedStateConstants;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.sm.annotations.adapters.Password;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.TextOutputCallback;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.Action.send;

/**
 * The onfidoRegistrationNode is used in an authentication tree to require an end user to go through an identity
 * verification (IDV) check using document, face, or video. This check is run by onfido. Information is pulled
 * from the document to initiate the IDV check.
 */
@Slf4j(topic = "amAuth")
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = onfidoRegistrationNode.Config.class)
public class onfidoRegistrationNode extends SingleOutcomeNode {

    private final Config config;
    private final CoreWrapper coreWrapper;
    private final OnfidoAPI onfidoApi;

    /**
     * Configuration for the node.
     */
    public interface Config {

        @Attribute(order = 100)
        @Password
        char[] onfidoToken();

        @Attribute(order = 200)
        default boolean JITProvisioning() {
            return false;
        }

        @Attribute(order = 250)
        default String onfidoJWTreferrer() {
            return "*://*/*";
        }

        @Attribute(order = 270)
        default String onfidoApiBaseUrl() {
            return "https://api.onfido.com/v3/";
        }

        @Attribute(order = 300)
        default biometricCheck biometricCheck() {
            return biometricCheck.None;
        }

        @Attribute(order = 400)
        default String onfidoApplicantIdAttribute() {
            return "title";
        }

        @Attribute(order = 500)
        default Map<String, String> attributeMappingConfiguration() {
            return new HashMap<String, String>() {{
                //key is id_token key value is ldap attribute name
                put("cn", onfidoConstants.FIRST_NAME);
                put("givenName", onfidoConstants.FIRST_NAME);
                put("sn", onfidoConstants.LAST_NAME);
                put("postalAddress", "address_line_1");
                put("city", "address_line_3");
                put("postalCode", "address_line_4");
                put("stateProvince", "address_line_5");
            }};
        }


        @Attribute(order = 600)
        default String onfidoWelcomeMessage() {
            return "Identity Verification";
        }

        @Attribute(order = 700)
        default String onfidoHelpMessage() {
            return "Thank you for using Onfido for Identity Verification";
        }

        @Attribute(order = 900)
        default String onfidoJSURL() {
            return "https://assets.onfido.com/web-sdk-releases/6.7.1/onfido.min.js";
        }

        @Attribute(order = 1000)
        default String onfidoCSSUrl() {
            return "https://assets.onfido.com/web-sdk-releases/6.7.1/style.css";
        }
    }


    /**
     * Create the node using Guice injection. Just-in-time bindings can be used to obtain instances of other classes
     * from the plugin.
     *
     * @param config The service config.
     */
    @Inject
    public onfidoRegistrationNode(@Assisted Config config, CoreWrapper coreWrapper) throws NodeProcessException {
        log.debug("onfidoRegistrationNode config: {}", config);
        this.config = config;
        this.coreWrapper = coreWrapper;
        this.onfidoApi = new OnfidoAPI(config);
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        log.debug("Calling onfidoRegistrationNode process method. Context: {}", context);

        if (!context.getCallback(TextOutputCallback.class).isPresent()) {
            return buildCallbacks(context);
        }

        onfidoAutoFill autofill = new onfidoAutoFill(onfidoApi);

        // Registration Flow (New User)
        if (config.JITProvisioning()) {
            handleJITProvisioning(context, autofill);
        }

        // Step-Up Flow (Existing User)
        if (!context.request.ssoTokenId.isEmpty()) {
            useSSOToken(context, autofill);
        }

        String applicantId = context.sharedState.get(onfidoConstants.ONFIDO_APPLICANT_ID).asString();
        context.sharedState.remove(onfidoConstants.ONFIDO_APPLICANT_ID);

        onfidoApi.createCheck(applicantId);

        return goToNext().build();
    }

    private Action buildCallbacks(TreeContext context) throws NodeProcessException {
        // Limit the at-rest region, if needed (optional, see https://documentation.onfido.com/#regions)
        Applicant newApplicant = onfidoApi.createApplicant();
        String sdkToken = onfidoApi.requestSdkToken(newApplicant);

        context.sharedState.put(onfidoConstants.ONFIDO_APPLICANT_ID, newApplicant.getId());

        log.debug("SDK Token is {}", sdkToken);

        OnfidoWebSdkConfig webSdkConfig = new OnfidoWebSdkConfig(config, sdkToken);

        String configuredScript = String.format(
            onfidoConstants.SETUP_DOM_SCRIPT,
            config.onfidoJSURL(),
            config.onfidoCSSUrl(),
            webSdkConfig.getSdkConfig()
        );

        List<Callback> callbackList = new ArrayList<>() {{
            add(new ScriptTextOutputCallback(configuredScript));
        }};

        return send(callbackList).build();
    }

    private void useSSOToken(TreeContext context, onfidoAutoFill autofill) throws NodeProcessException {
        AMIdentity userIdentity = getAMIdentity(context);
        String username = userIdentity.getName();

        if (username.equals("anonymous")) {
            return;
        }

        try {
            String onfidoApplicantId = context.sharedState.get(onfidoConstants.ONFIDO_APPLICANT_ID).asString();
            UserData idAttributes = UserData.fromAmIdentityWith(userIdentity, config.attributeMappingConfiguration());

            setObjectAttributes(context, idAttributes, autofill);

            Map<String, String> attrMap = new HashMap<>() {{ put(config.onfidoApplicantIdAttribute(), onfidoApplicantId); }};

            userIdentity.setAttributes(attrMap);
            userIdentity.store();
        } catch (SSOException | IdRepoException e) {
            String message = "Unable to store attribute for user. Make sure the configuration for called Onfido ApplicantID Attribute is a valid LDAP Attribute";
            log.error(message, e);
        }
    }

    private void handleJITProvisioning(TreeContext context, onfidoAutoFill autofill) throws NodeProcessException {
        String onfidoApplicantId = context.sharedState.get(onfidoConstants.ONFIDO_APPLICANT_ID).asString();

        UserData idAttributes = autofill.getIdAttributes(onfidoApplicantId);

        setObjectAttributes(context, idAttributes, autofill);
    }

    private void setObjectAttributes(TreeContext context, UserData userData, onfidoAutoFill autofill) throws NodeProcessException {
        String onfidoApplicantId = context.sharedState.get(onfidoConstants.ONFIDO_APPLICANT_ID).asString();
        String username = context.sharedState.get(SharedStateConstants.USERNAME).asString();

        JsonValue objectAttributes = json(object());
        JsonValue oldAttributes = context.sharedState.get("objectAttributes");

        // Ensure that we keep any existing attributes
        for (String key : oldAttributes.keys()) {
            objectAttributes.put(key, oldAttributes.get(key));
        }

        if (userData == null) {
            context.sharedState.put(onfidoConstants.IDM_ATTRIBUTES_SHARED_STATE_KEY, objectAttributes);

            return;
        }

        // Handle onfido-gathered document attributes, overriding any existing attributes
        autofill.populateOnfidoApplicant(onfidoApplicantId, userData);

        JsonValue onfidoAttributes = userData.asJsonValueWith(config.attributeMappingConfiguration());

        for (String key : onfidoAttributes.keys()) {
            objectAttributes.put(key, onfidoAttributes.get(key));
        }

        // Ensure additional non-document attributes are included
        objectAttributes.put(onfidoConstants.USER_NAME_SHARED_STATE_KEY, username);
        objectAttributes.put(config.onfidoApplicantIdAttribute(), onfidoApplicantId);

        context.sharedState.put(onfidoConstants.IDM_ATTRIBUTES_SHARED_STATE_KEY, objectAttributes);
    }

    private AMIdentity getAMIdentity(TreeContext context) throws NodeProcessException {
        try {
            return coreWrapper.getIdentity(SSOTokenManager.getInstance().createSSOToken(context.request.ssoTokenId));
        } catch (IdRepoException | SSOException e) {
            log.error("Unable to find user identity for user with SSO token: {}", context.request.ssoTokenId);

            throw new NodeProcessException(e);
        }
    }

    public enum biometricCheck {
        None,
        Selfie,
        Live
    }
}
