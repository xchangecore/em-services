package com.leidos.xchangecore.core.em.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.CollectionOfElements;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;

/**
 * The IncidentCommandStructure data model.
 * 
 * @ssdd
 */
@Entity
@Table(name = "INCIDENT_COMMAND_STRUCTURE")
public class IncidentCommandStructure {

    @Id
    @Column(name = "ICS_ID")
    private String id;

    @CollectionOfElements
    private Set<String> incidents = new HashSet<String>();

    @Column(name = "OPERATION_PERIOD")
    @Field(index = Index.TOKENIZED)
    private String operationPeriod;

    @Field(index = Index.TOKENIZED)
    private String type;

    @OneToOne(targetEntity = OrganizationElement.class)
    private OrganizationElement organization;

    public IncidentCommandStructure() {

        this.id = UUID.randomUUID().toString();
    }

    /**
     * Gets the id.
     * 
     * @return the id
     * @ssdd
     */
    public String getId() {

        return this.id;
    }

    /**
     * Sets the id.
     * 
     * @param id the new id
     * @ssdd
     */
    public void setId(String id) {

        this.id = id;
    }

    /**
     * Gets the operation period.
     * 
     * @return the operation period
     * @ssdd
     */
    public String getOperationPeriod() {

        return operationPeriod;
    }

    /**
     * Sets the operation period.
     * 
     * @param operationPeriod the new operation period
     * @ssdd
     */
    public void setOperationPeriod(String operationPeriod) {

        this.operationPeriod = operationPeriod;
    }

    /**
     * Gets the organization.
     * 
     * @return the organization
     * @ssdd
     */
    public OrganizationElement getOrganization() {

        return organization;
    }

    /**
     * Sets the organization.
     * 
     * @param organization the new organization
     * @ssdd
     */
    public void setOrganization(OrganizationElement organization) {

        this.organization = organization;
    }

    /**
     * Gets the incidents.
     * 
     * @return the incidents
     * @ssdd
     */
    public Set<String> getIncidents() {

        return this.incidents;
    }

    /**
     * Sets the incidents.
     * 
     * @param incidents the new incidents
     * @ssdd
     */
    public void setIncidents(Set<String> incidents) {

        this.incidents = incidents;
    }

    /**
     * Adds the incident.
     * 
     * @param incidentID the incident id
     * @ssdd
     */
    public void addIncident(String incidentID) {

        this.incidents.add(incidentID);
    }

    /**
     * Sets the type.
     * 
     * @param type the new type
     * @ssdd
     */
    public void setType(String type) {

        this.type = type;
    }

    /**
     * Gets the type.
     * 
     * @return the type
     * @ssdd
     */
    public String getType() {

        return type;
    }

    /**
     * Gets the product id.
     * 
     * @return the product id
     * @ssdd
     */
    public String getProductID() {

        return this.type + "-" + this.id;
    }
}
