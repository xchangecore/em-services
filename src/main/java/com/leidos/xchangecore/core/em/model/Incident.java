package com.leidos.xchangecore.core.em.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;

/**
 * Represents an incident inside the Core.
 *
 * @author topher
 * @ssdd
 *
 */
@Entity
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Lob
    @Field(index = Index.TOKENIZED)
    private String summary;

    // this is the same as the interest groupID returned from the
    // createINterestGroup() call
    @Field(index = Index.TOKENIZED)
    private String incidentId;

    @Field(index = Index.TOKENIZED)
    private String workProductID;

    @Field(index = Index.TOKENIZED)
    private double latitude;

    @Field(index = Index.TOKENIZED)
    private double longitude;

    private boolean active = true;

    /**
     * Construct a new incident.
     *
     * @ssdd
     */
    public Incident() {

        this(null);
    }

    /**
     * Construct a new incident with the provided incident ID.
     *
     * @param incidentID the incident id
     * @ssdd
     */
    public Incident(String incidentID) {

        setIncidentId(incidentID);
    }

    /**
     * Return the primary key of the incident.
     *
     * @return the id
     *
     * @ssdd
     */
    public Integer getId() {

        return id;
    }

    /**
     * Gets the incident id.
     *
     * @return the incident id
     * @ssdd
     */
    public String getIncidentId() {

        return incidentId;
    }

    /**
     * Gets the latitude.
     *
     * @return the latitude
     * @ssdd
     */
    public double getLatitude() {

        return latitude;
    }

    /**
     * Gets the longitude.
     *
     * @return the longitude
     * @ssdd
     */
    public double getLongitude() {

        return longitude;
    }

    /**
     * Return the summary of the incident.
     *
     * @return the summary
     *
     * @ssdd
     */
    public String getSummary() {

        return summary;
    }

    /**
     * Gets the work product id.
     *
     * @return the work product id global identifier for this incident
     * @ssdd
     */
    public String getWorkProductID() {

        return workProductID;
    }

    /**
     * Checks if is active.
     *
     * @return true, if is active
     * @ssdd
     */
    public boolean isActive() {

        return active;
    }

    /**
     * Sets the active.
     *
     * @param active the new active
     * @ssdd
     */
    public void setActive(boolean active) {

        this.active = active;
    }

    /**
     * Set the primary key of the incident.
     *
     * @param id
     * @ssdd
     */
    public void setId(Integer id) {

        this.id = id;
    }

    /**
     * Sets the incident id.
     *
     * @param incidentID the new incident id
     * @ssdd
     */
    public void setIncidentId(String incidentID) {

        incidentId = incidentID;
    }

    /**
     * Sets the latitude.
     *
     * @param latitude the new latitude
     * @ssdd
     */
    public void setLatitude(double latitude) {

        this.latitude = latitude;
    }

    /**
     * Sets the longitude.
     *
     * @param longitude the new longitude
     * @ssdd
     */
    public void setLongitude(double longitude) {

        this.longitude = longitude;
    }

    /**
     * Set the summary of the incident.
     *
     * @param summary
     * @ssdd
     */
    public void setSummary(String summary) {

        this.summary = summary;
    }

    /**
     * Sets the work product id.
     *
     * @param workProductID the new work product id global identifier for this incident
     * @ssdd
     */
    public void setWorkProductID(String workProductID) {

        this.workProductID = workProductID;
    }

}
