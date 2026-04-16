package com.traceroot.platform.metric;

public class TopIncidentPatternResponse {

    private String fingerPrint;
    private String title;
    private Long incidentCount;

    public TopIncidentPatternResponse(String fingerPrint, String title, Long incidentCount) {
        this.fingerPrint = fingerPrint;
        this.title = title;
        this.incidentCount = incidentCount;
    }

    public String getFingerPrint() {
        return fingerPrint;
    }

    public void setFingerPrint(String fingerPrint) {
        this.fingerPrint = fingerPrint;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getIncidentCount() {
        return incidentCount;
    }

    public void setIncidentCount(Long incidentCount) {
        this.incidentCount = incidentCount;
    }
}
