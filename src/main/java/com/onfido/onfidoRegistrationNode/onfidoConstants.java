package com.onfido.onfidoRegistrationNode;

class onfidoConstants {
    static final String ONFIDO_APPLICANT_ID = "onfidoApplicantID";
    static final String FIRST_NAME = "first_name";
    static final String LAST_NAME = "last_name";
    static final String FAIL_FLAG = "fail";
    static final String PASS_FLAG = "pass";
    static final String USER_NAME_SHARED_STATE_KEY = "userName";
    static final String IDM_ATTRIBUTES_SHARED_STATE_KEY = "objectAttributes";

    static String SETUP_DOM_SCRIPT =
            "document.getElementById('loginButton_0').style.display = 'none'; %n%n" +
            "var onfido_div = document.createElement(\"div\"); %n" +
            "onfido_div.setAttribute(\"id\", \"onfido-mount\"); %n%n" +
            "var script = document.createElement('script'); %n" +
            "script.setAttribute(\"src\", \"%s\"); %n%n" +
            "var head = document.head; %n " +
            "var link = document.createElement(\"link\"); %n" +
            "link.setAttribute(\"type\", \"text/css\"); %n" +
            "link.setAttribute(\"rel\", \"stylesheet\"); %n" +
            "link.setAttribute(\"href\", \"%s\"); %n" +
            "head.appendChild(link); %n%n" +
            "var onfidoConfig = %s; %n" +
            "var onfido; %n" +
            "script.onload = function() { onfido = Onfido.init(onfidoConfig); }; %n%n" +
            "document.body.appendChild(onfido_div); %n" +
            "document.body.appendChild(script); %n";
}


