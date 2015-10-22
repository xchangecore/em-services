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
 * The OrganizationPosition data model.
 * 
 * @ssdd
 */
@Entity
@Table(name = "ORGANIZATION_POSITION_TYPE")
public class OrganizationPositionType
    implements Serializable {

    private static final long serialVersionUID = 6437778420187683857L;

    @Id
    @Column(name = "ORG_POSITION_ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ROLE_PROFILE_REF")
    @Field(index = Index.TOKENIZED)
    private String roleProfileRef;

    @Column(name = "PERSON_PROFILE_REF")
    @Field(index = Index.TOKENIZED)
    private String personProfileRef;

    /**
     * Instantiates a new organization position type.
     * 
     * @param personProfileRef the person profile ref
     * @param roleProfileRef the role profile ref
     * @ssdd
     */
    public OrganizationPositionType(String personProfileRef, String roleProfileRef) {

        super();
        this.personProfileRef = personProfileRef;
        this.roleProfileRef = roleProfileRef;
    }

    /**
     * Gets the id.
     * 
     * @return the id
     * @ssdd
     */
    public Integer getId() {

        return id;
    }

    /**
     * Sets the id.
     * 
     * @param id the new id
     * @ssdd
     */
    public void setId(Integer id) {

        this.id = id;
    }

    /**
     * Gets the role profile ref.
     * 
     * @return the role profile ref
     * @ssdd
     */
    public String getRoleProfileRef() {

        return roleProfileRef;
    }

    /**
     * Sets the role profile ref.
     * 
     * @param roleProfileRef the new role profile ref
     * @ssdd
     */
    public void setRoleProfileRef(String roleProfileRef) {

        this.roleProfileRef = roleProfileRef;
    }

    /**
     * Gets the person profile ref.
     * 
     * @return the person profile ref
     * @ssdd
     */
    public String getPersonProfileRef() {

        return personProfileRef;
    }

    /**
     * Sets the person profile ref.
     * 
     * @param personProfileRef the new person profile ref
     * @ssdd
     */
    public void setPersonProfileRef(String personProfileRef) {

        this.personProfileRef = personProfileRef;
    }

    /**
     * Instantiates a new organization position type.
     * 
     * @ssdd
     */
    public OrganizationPositionType() {

        super();
    }
}
