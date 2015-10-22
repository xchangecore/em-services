package com.leidos.xchangecore.core.em.dao;

import java.util.List;

import com.leidos.xchangecore.core.dao.GenericDAO;
import com.leidos.xchangecore.core.em.model.Incident;

/**
 * IncidentDAO
 *
 * @author created: package: com.saic.dctd.uicds.core.dao
 */
public interface IncidentDAO
    extends GenericDAO<Incident, Integer> {

    public void delete(String incidentId, boolean isDelete);

    @Override
    public List<Incident> findAll();

    public List<Incident> findAllClosedIncident();

    public Incident findByIncidentID(String incidentId);

    public boolean isActive(String incidentId);
}
