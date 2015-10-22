/**
 *
 */
package com.leidos.xchangecore.core.em.dao.hb;

import com.leidos.xchangecore.core.dao.hb.GenericHibernateDAO;
import com.leidos.xchangecore.core.em.dao.OrganizationElementDAO;
import com.leidos.xchangecore.core.em.model.OrganizationElement;

public class OrganizationElementDAOHibernate
    extends GenericHibernateDAO<OrganizationElement, String>
    implements OrganizationElementDAO {}