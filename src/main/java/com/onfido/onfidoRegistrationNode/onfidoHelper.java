package com.onfido.onfidoRegistrationNode;

import com.onfido.models.Report;
import org.apache.commons.lang.StringUtils;
import org.forgerock.openam.auth.node.api.NodeProcessException;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class onfidoHelper {
    /*
     * Returns the first UUID (applicantId) extracted from a string (check URI)
     *
     * @param href a URI such as https://api.onfido.com/v2/applicants/4f5bd603-2475-4d95-86e9-5ce943dd7862/checks/39236dbe-5f98-4fb4-8ff0-cb5a179b42a3
     * @return the applicantId
     * @throws NodeProcessException
     */
    String getApplicantId(String href) throws NodeProcessException {
        final String regex = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
        final Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        final Matcher matcher = pattern.matcher(href);

        if (matcher.find()) {
            return matcher.group(0);
        }
        else {
            throw new NodeProcessException("Unable to extract applicantId from URL: " + StringUtils.left(href, 100));
        }
    }

    public boolean parseReports(List<Report> reports, String[] breakdowns) throws NodeProcessException {
        boolean docResult = false;
        boolean faceResult = true;

        for (Report report : reports) {
            ReportNames reportName = ReportNames.fromString(report.getName());

            switch (reportName) {
                case DOCUMENT:
                    docResult = parseDocumentReport(report, breakdowns);
                    break;

                case FACIAL_SIMILARITY_PHOTO: // Fall through
                case FACIAL_SIMILARITY_VIDEO:
                    faceResult = parseFacialReport(report);
                    break;
            }
        }

        return docResult && faceResult;
    }

    private boolean parseDocumentReport(Report report, String[] breakdowns) throws NodeProcessException {
        if (!"clear".equals(report.getResult())) {
            // Use the breakdowns??? (Examine the sub-results, that is???)
        }
        return true;
    }

    private boolean parseFacialReport(Report report) {
        return "clear".equals(report.getResult());
    }
}


