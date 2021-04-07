package com.onfido.onfidoRegistrationNode;

public enum ReportNames {
    DOCUMENT("document"),
    FACIAL_SIMILARITY_PHOTO("facial_similarity_photo"),
    FACIAL_SIMILARITY_VIDEO("facial_similarity_video");

    private String reportName;

    ReportNames(String reportName){
        this.reportName = reportName;
    }

    @Override
    public String toString(){
        return reportName;
    }

    public static ReportNames fromString(String reportName) {
        for (ReportNames name : ReportNames.values()) {
            if (reportName.equals(name.toString())) {
                return name;
            }
        }

        return null;
    }
}
