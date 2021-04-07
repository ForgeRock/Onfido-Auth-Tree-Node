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
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.onfido.exceptions.OnfidoException;
import com.onfido.models.Report;
import com.onfido.webhooks.WebhookEvent;
import com.onfido.webhooks.WebhookEventVerifier;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import lombok.extern.slf4j.Slf4j;
import org.forgerock.am.cts.CTSPersistentStore;
import org.forgerock.am.cts.api.filter.TokenFilter;
import org.forgerock.am.cts.api.filter.TokenFilterBuilder;
import org.forgerock.am.cts.api.query.PartialToken;
import org.forgerock.am.cts.api.tokens.CoreTokenField;
import org.forgerock.am.cts.exceptions.CoreTokenException;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.SharedStateConstants;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.sm.annotations.adapters.Password;
import org.forgerock.openam.utils.StringUtils;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.onfido.onfidoRegistrationNode.onfidoConstants.FAIL_FLAG;
import static com.onfido.onfidoRegistrationNode.onfidoConstants.PASS_FLAG;

@Slf4j(topic = "amAuth")
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class, configClass = onfidoWebhookNode.Config.class)
public class onfidoWebhookNode extends SingleOutcomeNode {

    private final Config config;
    private final CTSPersistentStore ctsPersistentStore;
    private final OnfidoAPI onfidoApi;
    private final onfidoHelper onfidoHelper;


    /**
     * Configuration for the node.
     */
    public interface Config {

        @Attribute(order = 100)
        @Password
        char[] onfidoToken();

        @Attribute(order = 200)
        @Password
        char[] webhookToken();

        @Attribute(order = 270)
        default String onfidoApiBaseUrl() {
            return "https://api.onfido.com/v3/";
        }

        @Attribute(order = 300)
        default boolean lockUser() {
            return false;
        }

        @Attribute(order = 400)
        default String userFlagAttribute() {
            return "carLicense";
        }

        @Attribute(order = 450)
        default String onfidoApplicantIdAttribute() {
            return "title";
        }

        //needs to be SET of strings
        @Attribute(order = 500)
        default Set<String> breakDowns() {
            return new HashSet<String>() {{
                add("age_validation");
                add("compromised_document");
                add("data_comparison");
                add("data_validation");
                add("visual_authenticity");
                add("image_integrity");
            }};
        }
    }


    /**
     * Create the node using Guice injection. Just-in-time bindings can be used to obtain instances of other classes
     * from the plugin.
     *
     * @param config The service config.
     */
    @Inject
    public onfidoWebhookNode(@Assisted Config config, CTSPersistentStore ctsPersistentStore) throws NodeProcessException {
        log.debug("onfidoWebhookNode config: {}", config);

        this.config = config;
        this.ctsPersistentStore = ctsPersistentStore;
        this.onfidoApi = new OnfidoAPI(config);
        this.onfidoHelper = new onfidoHelper();
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        final HttpServletRequest request = context.request.servletRequest;

        log.debug("inside onfidoWebhookNode process");

        context.sharedState.put(SharedStateConstants.USERNAME, "anonymous");

        WebhookEvent webhookEvent = verifyWebhookEvent(request);
        String checksHref = webhookEvent.getObject().getHref();
        String checkId = webhookEvent.getObject().getId();

        AMIdentity userIdentity =
                Optional.ofNullable(
                        findUser(
                                context.sharedState.get(SharedStateConstants.REALM).asString(),
                                onfidoHelper.getApplicantId(checksHref)
                        ))
                        .orElseThrow(() -> new NodeProcessException("Could not find user identity"));

        log.debug("Processing user: {}", userIdentity.getUniversalId());
        log.debug("Fetching results for check: {}", checkId);

        setUserCheckResults(userIdentity, checkId);

        return goToNext().build();
    }

    private AMIdentity findUser(String realm, String applicantId) {
        // The onfidoApplicantIdAttribute is the DS attribute to which the IDM Onfido Applicant ID is linked
        Set<String> userSearchAttributes = new HashSet<String>() {{ add(config.onfidoApplicantIdAttribute()); }};

        return IdUtils.getIdentity(applicantId, realm, userSearchAttributes);
    }

    private void setUserCheckResults(AMIdentity userIdentity, String checkId) throws NodeProcessException {
        List<Report> reports = onfidoApi.listReports(checkId);
        String[] breakdowns = config.breakDowns().toArray(new String[0]);
        boolean checkPassed = onfidoHelper.parseReports(reports, breakdowns);

        if (checkPassed) {
            flagUser(userIdentity, PASS_FLAG);
        } else {
            flagUser(userIdentity, FAIL_FLAG);

            if (config.lockUser()) {
                lockUser(userIdentity);
            }
        }
    }

    private void flagUser(AMIdentity userIdentity, String flag) throws NodeProcessException {
        String userFlagAttribute = config.userFlagAttribute();

        if (StringUtils.isBlank(userFlagAttribute)) {
            return;
        }

        try {
            Map<String, Set> attrMap = new HashMap<>();
            attrMap.put(userFlagAttribute, new HashSet<String>() {{ add(flag); }});

            // Include the (default) required fields
            attrMap.put("givenName", userIdentity.getAttribute("givenName"));
            attrMap.put("sn", userIdentity.getAttribute("sn"));
            attrMap.put("mail", userIdentity.getAttribute("mail"));

            userIdentity.setAttributes(attrMap);
            userIdentity.store();

            log.debug("User was flagged");
        } catch (IdRepoException | SSOException e) {
            throw new NodeProcessException(e);
        }
    }

    private void lockUser(AMIdentity userIdentity) throws NodeProcessException {
        try {
            TokenFilter filter = new TokenFilterBuilder()
                    .returnAttribute(CoreTokenField.STRING_FIVE)
                    .and().withAttribute(CoreTokenField.USER_ID, userIdentity.getUniversalId())
                    .build();

            Collection<PartialToken> tokens = ctsPersistentStore.attributeQuery(filter);

            while (tokens.iterator().hasNext()) {
                String tokenString = tokens.iterator().next().getValue(CoreTokenField.STRING_FIVE).toString();

                try {
                    SSOToken token = SSOTokenManager.getInstance().createSSOToken(tokenString);
                    SSOTokenManager.getInstance().logout(token);
                } catch (SSOException e) {
                    tokens.remove(tokens.iterator().next());
                }
            }
        } catch (CoreTokenException e) {
            throw new NodeProcessException(e);
        }

        try {
            userIdentity.setActiveStatus(false);
        } catch (IdRepoException | SSOException e) {
            throw new NodeProcessException(e);
        }

        log.debug("Lock User");
    }

    private WebhookEvent verifyWebhookEvent(HttpServletRequest request) throws NodeProcessException {
        try {
            String jsonContent = request.getParameter("jsonContent").replaceAll("\\s+(?![1-9a-zA-Z])", "");
            String signature = request.getHeader("X-SHA2-Signature");
            String webhookToken = new String(config.webhookToken());

            WebhookEventVerifier webhookEventVerifier = new WebhookEventVerifier(webhookToken);

            return webhookEventVerifier.readPayload(jsonContent, signature);
        } catch (OnfidoException e) {
            throw new NodeProcessException(e);
        }
    }
}
