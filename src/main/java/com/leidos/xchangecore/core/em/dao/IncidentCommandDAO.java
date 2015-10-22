package com.leidos.xchangecore.core.em.dao;

import com.leidos.xchangecore.core.dao.GenericDAO;
import com.leidos.xchangecore.core.em.model.IncidentCommandStructure;

public interface IncidentCommandDAO
    extends GenericDAO<IncidentCommandStructure, String> {

    // IncidentCommandStructure findByIncidentId(String incident);
}