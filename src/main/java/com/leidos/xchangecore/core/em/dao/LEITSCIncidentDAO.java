package com.leidos.xchangecore.core.em.dao;

import com.leidos.xchangecore.core.dao.GenericDAO;
import com.leidos.xchangecore.core.em.model.LEITSCIncident;

public interface LEITSCIncidentDAO
extends GenericDAO<LEITSCIncident, Integer> {

    public LEITSCIncident findByIncident(String incidentID);

    public LEITSCIncident findByLEITSCIncident(String leitscIncidentID);
}
