package com.leidos.xchangecore.core.em.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;

/**
 * Represents a LEITSC incident inside the UICDS Core.
 * 
 * @author Aruna Hau
 * @ssdd
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "LEITSC_INCIDENT")
public class LEITSCIncident
    implements Serializable {

    @SuppressWarnings("unused")
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "LEITTSC_INCIDENT_ID")
    @Field(index = Index.TOKENIZED)
    private String leitscIncidentID;

    @Column(name = "INCIDENT_ID")
    @Field(index = Index.TOKENIZED)
    private String incidentID;

    @Column(name = "INCIDENT_WP_ID")
    @Field(index = Index.TOKENIZED)
    private String incidentWPID;

    /**
     * Gets the leitsc incident id.
     * 
     * @return the leitsc incident id
     * @ssdd
     */
    public String getLeitscIncidentID() {

        return leitscIncidentID;
    }

    /**
     * Sets the leitsc incident id.
     * 
     * @param leitscIncidentID the new leitsc incident id
     * @ssdd
     */
    public void setLeitscIncidentID(String leitscIncidentID) {

        this.leitscIncidentID = leitscIncidentID;
    }

    /**
     * Gets the incident id.
     * 
     * @return the incident id
     * @ssdd
     */
    public String getIncidentID() {

        return incidentID;
    }

    /**
     * Sets the incident id.
     * 
     * @param incidentID the new incident id
     * @ssdd
     */
    public void setIncidentID(String incidentID) {

        this.incidentID = incidentID;
    }

    /**
     * Gets the incident wpid.
     * 
     * @return the incident wpid
     * @ssdd
     */
    public String getIncidentWPID() {

        return incidentWPID;
    }

    /**
     * Sets the incident wpid.
     * 
     * @param incidentWPID the new incident wpid
     * @ssdd
     */
    public void setIncidentWPID(String incidentWPID) {

        this.incidentWPID = incidentWPID;
    }

}
