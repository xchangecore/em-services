/**
 *
 */
package com.leidos.xchangecore.core.em.dao.hb;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.springframework.transaction.annotation.Transactional;

import com.leidos.xchangecore.core.dao.hb.GenericHibernateDAO;
import com.leidos.xchangecore.core.em.dao.IncidentDAO;
import com.leidos.xchangecore.core.em.model.Incident;

/**
 * IncidentDAOHibernate
 *
 * @author created: package: com.saic.dctd.uicds.core.dao.hb
 */
@Transactional
public class IncidentDAOHibernate
    extends GenericHibernateDAO<Incident, Integer>
    implements IncidentDAO {

    @Override
    public void delete(String incidentId, boolean isDelete) {

        final Incident incident = findByIncidentID(incidentId);

        if (incident == null)
            return;

        if (isDelete == true)
            makeTransient(incident);
        else {
            incident.setActive(false);
            makePersistent(incident);
        }
    }

    @Override
    public List<Incident> findAll() {

        return findAllIncidents(true);
    }

    @Override
    public List<Incident> findAllClosedIncident() {

        return findAllIncidents(false);
    }

    private List<Incident> findAllIncidents(boolean isActive) {

        final List<Incident> incidents = super.findAll();
        List<Incident> returnIncidents = null;
        if (incidents.size() >= 0) {
            returnIncidents = new ArrayList<Incident>();
            for (final Incident incident : incidents)
                // this is a little bit of tricky.
                // if isActive is true means we will return all the incidents
                // it doesn't matter it's active/inactive
                // if isActive is false then we will only return the status is inActive
                if (isActive == true || incident.isActive() == false)
                    returnIncidents.add(incident);
        }

        return returnIncidents;

    }

    @Override
    public Incident findByIncidentID(String incidentId) {

        final List<Incident> results = findByCriteria(Restrictions.eq("incidentId", incidentId));
        if (!results.isEmpty())
            return results.get(0);
        return null;
    }

    @Override
    public boolean isActive(String incidentId) {

        final Incident incident = findByIncidentID(incidentId);
        if (incident == null)
            return true;

        return incident.isActive();
    }
}