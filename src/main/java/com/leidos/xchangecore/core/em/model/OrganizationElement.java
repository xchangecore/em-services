package com.leidos.xchangecore.core.em.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CollectionOfElements;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;

/**
 * The OrganizationElement data model.
 * 
 * @ssdd
 */
@Entity
@Table(name = "ORGANIZATION_ELEMENT")
public class OrganizationElement
    implements Serializable {

    private static final long serialVersionUID = -8391269321023161271L;

    @Id
    @Column(name = "ORGANIZATION_ID")
    private String id;

    @Column(name = "ORGANIZATION_NAME")
    @Field(index = Index.TOKENIZED)
    private String organizationName;

    @Column(name = "ORGANIZATION_TYPE")
    @Field(index = Index.TOKENIZED)
    private String organizationType;

    @OneToOne(targetEntity = OrganizationPositionType.class, cascade = CascadeType.ALL)
    @Cascade({
        org.hibernate.annotations.CascadeType.DELETE_ORPHAN
    })
    private OrganizationPositionType personInCharge;

    @Column(name = "PARENT_ORG_ID")
    @Field(index = Index.TOKENIZED)
    private String parentID;

    @OneToMany(targetEntity = OrganizationPositionType.class, cascade = CascadeType.ALL)
    @Cascade({
        org.hibernate.annotations.CascadeType.DELETE_ORPHAN
    })
    private Set<OrganizationPositionType> staffs = new HashSet<OrganizationPositionType>();

    @CollectionOfElements
    private Set<String> organizations = new HashSet<String>();

    @CollectionOfElements
    private Set<String> coordinatings = new HashSet<String>();

    public OrganizationElement() {

        super();
        this.id = UUID.randomUUID().toString();
    }

    /**
     * Gets the id.
     * 
     * @return the id
     * @ssdd
     */
    public String getId() {

        return id;
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
     * Gets the organization name.
     * 
     * @return the organization name
     * @ssdd
     */
    public String getOrganizationName() {

        return organizationName;
    }

    /**
     * Sets the organization name.
     * 
     * @param organizationName the new organization name
     * @ssdd
     */
    public void setOrganizationName(String organizationName) {

        this.organizationName = organizationName;
    }

    /**
     * Gets the organization type.
     * 
     * @return the organization type
     * @ssdd
     */
    public String getOrganizationType() {

        return organizationType;
    }

    /**
     * Sets the organization type.
     * 
     * @param organizationType the new organization type
     * @ssdd
     */
    public void setOrganizationType(String organizationType) {

        this.organizationType = organizationType;
    }

    /**
     * Gets the person in charge.
     * 
     * @return the person in charge
     * @ssdd
     */
    public OrganizationPositionType getPersonInCharge() {

        return personInCharge;
    }

    /**
     * Sets the person in charge.
     * 
     * @param personInCharge the new person in charge
     * @ssdd
     */
    public void setPersonInCharge(OrganizationPositionType personInCharge) {

        this.personInCharge = personInCharge;
    }

    /**
     * Gets the parent id.
     * 
     * @return the parent id
     * @ssdd
     */
    public String getParentID() {

        return parentID;
    }

    /**
     * Sets the parent id.
     * 
     * @param parentID the new parent id
     * @ssdd
     */
    public void setParentID(String parentID) {

        this.parentID = parentID;
    }

    /**
     * Gets the staffs.
     * 
     * @return the staffs
     * @ssdd
     */
    public Set<OrganizationPositionType> getStaffs() {

        return this.staffs;
    }

    /**
     * Sets the staffs.
     * 
     * @param staffs the new staffs
     * @ssdd
     */
    public void setStaffs(Set<OrganizationPositionType> staffs) {

        this.staffs = staffs;
    }

    /**
     * Gets the coordinatings.
     * 
     * @return the coordinatings
     * @ssdd
     */
    public Set<String> getCoordinatings() {

        return this.coordinatings;
    }

    /**
     * Gets the organizations.
     * 
     * @return the organizations
     * @ssdd
     */
    public Set<String> getOrganizations() {

        return organizations;
    }

    /**
     * Sets the organizations.
     * 
     * @param organizations the new organizations
     * @ssdd
     */
    public void setOrganizations(Set<String> organizations) {

        this.organizations = organizations;
    }

    /**
     * Sets the coordinatings.
     * 
     * @param coordinatings the new coordinatings
     * @ssdd
     */
    public void setCoordinatings(Set<String> coordinatings) {

        this.coordinatings = coordinatings;
    }

    /**
     * Removes the organization.
     * 
     * @param orgID the org id
     * @ssdd
     */
    public void removeOrganization(String orgID) {

        if (coordinatings.contains(orgID))
            coordinatings.remove(orgID);
        else if (organizations.contains(orgID))
            organizations.remove(orgID);
    }

    /**
     * Adds the organization.
     * 
     * @param organizationID the organization id
     * @ssdd
     */
    public void addOrganization(String organizationID) {

        organizations.add(organizationID);
    }
}
