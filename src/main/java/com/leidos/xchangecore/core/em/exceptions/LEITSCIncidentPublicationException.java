package com.leidos.xchangecore.core.em.exceptions;

import com.leidos.xchangecore.core.infrastructure.exceptions.UICDSException;

@SuppressWarnings("serial")
public class LEITSCIncidentPublicationException
    extends UICDSException {

    private String action;
    private String leitscIncidentID;
    private String reasonForFailure;

    public LEITSCIncidentPublicationException(String leitscIncidentID, String action,
                                              String reasonForFailure) {

        super("Error occured while attempting to " + action + "  leitsc incident,  ID=" +
              leitscIncidentID + " ,  reason for failure: " + reasonForFailure);
        setAction(action);
        setLeitscIncidentID(leitscIncidentID);
        setReasonForFailure(reasonForFailure);
    }

    public String getAction() {

        return action;
    }

    public void setAction(String action) {

        this.action = action;
    }

    public String getLeitscIncidentID() {

        return leitscIncidentID;
    }

    public void setLeitscIncidentID(String leitscIncidentID) {

        this.leitscIncidentID = leitscIncidentID;
    }

    public String getReasonForFailure() {

        return reasonForFailure;
    }

    public void setReasonForFailure(String reasonForFailure) {

        this.reasonForFailure = reasonForFailure;
    }

}
