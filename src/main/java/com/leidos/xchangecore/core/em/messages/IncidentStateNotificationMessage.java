/**
 * 
 */
package com.leidos.xchangecore.core.em.messages;

import org.uicds.incidentManagementService.IncidentInfoType;

import com.leidos.xchangecore.core.infrastructure.messages.InterestGroupStateNotificationMessage;

/**
 * @author roger
 * 
 */
public class IncidentStateNotificationMessage {

    private InterestGroupStateNotificationMessage.State state;
    private IncidentInfoType incidentInfo;

    public InterestGroupStateNotificationMessage.State getState() {

        return state;
    }

    public void setState(InterestGroupStateNotificationMessage.State state) {

        this.state = state;
    }

    public IncidentInfoType getIncidentInfo() {

        return incidentInfo;
    }

    public void setIncidentInfo(IncidentInfoType incidentInfo) {

        this.incidentInfo = incidentInfo;
    }

}
