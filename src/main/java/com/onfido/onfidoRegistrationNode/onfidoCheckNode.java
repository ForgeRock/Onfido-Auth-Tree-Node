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

import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.sm.annotations.adapters.Password;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;

import lombok.extern.slf4j.Slf4j;
/**
 * The onfidoCheckNode is used in an authentication tree to require an end user to go through an identity
 * verification (IDV) check using document, face, or video. This check is run by onfido. Information is pulled
 * from the document to initiate the IDV check.
 */
@Slf4j(topic = "amAuth")
@Node.Metadata(outcomeProvider = onfidoCheckNode.OutcomeProvider.class,
        configClass = onfidoCheckNode.Config.class, tags = {"marketplace","trustnetwork"})
public class onfidoCheckNode implements Node {

    private final Config config;
    private final CoreWrapper coreWrapper;
    private final OnfidoAPICheck onfidoApi;
    private String loggerPrefix = "[Onfido Check Node]" + onfidoRegistrationNodePlugin.logAppender;
    private static final Logger log = LoggerFactory.getLogger(OnfidoAPI.class);

    /**
     * Configuration for the node.
     */
    public interface Config {

        @Attribute(order = 100)
        @Password
        char[] onfidoToken();

        @Attribute(order = 200)
        default String onfidoApiBaseUrl() {
            return "https://api.onfido.com/v3/";
        }

        @Attribute(order = 300)
        default String onfidoCheckIdAttribute() {
            return "description";
        }
    }


    /**
     * Create the node using Guice injection. Just-in-time bindings can be used to obtain instances of other classes
     * from the plugin.
     *
     * @param config The service config.
     */
    @Inject
    public onfidoCheckNode(@Assisted Config config, CoreWrapper coreWrapper) throws NodeProcessException {
        log.debug(loggerPrefix+"onfidoCheckNode config: {}", config);
        this.config = config;
        this.coreWrapper = coreWrapper;
        this.onfidoApi = new OnfidoAPICheck(config);
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {

        log.debug(loggerPrefix+"Calling onfidoCheckNode process method. Context: {}", context);
        try {
        	
        	
        	NodeState ns = context.getStateFor(this);

        	 String username = ns.get(USERNAME).asString();
             String checkId = "";
             if (ns.get("checkId").asString() != null && !ns.get("checkId").asString().isEmpty()) {
                log.debug(loggerPrefix + "checkId is present");
                checkId=ns.get("checkId").asString();
                log.debug(loggerPrefix + "checkId: " + checkId);
             } else {
                 log.debug(loggerPrefix + "username: "+ username);
                 Set<String> identifiers;
                 log.debug(loggerPrefix + "Grabbing user identifiers for " + config.onfidoCheckIdAttribute());

                 identifiers = coreWrapper.getIdentityOrElseSearchUsingAuthNUserAlias(username, coreWrapper.convertRealmPathToRealmDn(ns.get(REALM).asString())).getAttribute(config.onfidoCheckIdAttribute());
                 
                 
                 if (identifiers != null && !identifiers.isEmpty()) {
                     checkId = identifiers.iterator().next();
                     log.debug(loggerPrefix + "CheckID found: " + checkId);
                 }
             }
             

        	String status = onfidoApi.checkStatus(checkId);
             ns.putShared("Onfido_Result", status);
        	if (status.equals("clear")) {
        		return Action.goTo("clear").build();
        	}
        	if (status.equals("consider")) {
                return Action.goTo("consider").build();
            }
            if (status.equals("pending")) {
                return Action.goTo("pending").build();
            }

            return Action.goTo("deny").build();
        } catch(Exception ex) {
        	log.error(loggerPrefix + "Exception occurred: " + ex.getStackTrace());
			context.getStateFor(this).putShared(loggerPrefix + "Exception", new Date() + ": " + ex.getMessage());
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			ex.printStackTrace(pw);
			context.getStateFor(this).putShared(loggerPrefix + "StackTrack", new Date() + ": " + sw.toString());
            return Action.goTo("error").build();
        }
    }
    
    public enum biometricCheck {
        None,
        Selfie,
        Live
    }



    public static final class OutcomeProvider implements org.forgerock.openam.auth.node.api.OutcomeProvider {
            /**
             * Outcomes Ids for this node.
             */
            static final String CLEAR_OUTCOME = "clear";
            static final String CONSIDER_OUTCOME = "consider";
            static final String DENY_OUTCOME = "deny";
            static final String PENDING_OUTCOME = "pending";
            static final String ERROR_OUTCOME = "error";
            
            private static final String BUNDLE = onfidoCheckNode.class.getName();

            @Override
            public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {


                List<Outcome> results = new ArrayList<>(
                        Arrays.asList(
                                new Outcome(CLEAR_OUTCOME, "Clear")
                        )
                );
                results.add(new Outcome(CONSIDER_OUTCOME, "Consider"));
                results.add(new Outcome(DENY_OUTCOME, "Deny"));
                results.add(new Outcome(PENDING_OUTCOME, "Pending"));
                results.add(new Outcome(ERROR_OUTCOME, "Error"));

                return Collections.unmodifiableList(results);
            }
    }

}
