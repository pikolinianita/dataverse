/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDataverseCommand;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.faces.application.FacesMessage;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import static edu.harvard.iq.dataverse.util.JsfHelper.JH;

/**
 *
 * @author gdurand
 */

@Stateless
@Named
public class DataverseServiceBean {

    private static final Logger logger = Logger.getLogger(DataverseServiceBean.class.getCanonicalName());
    @EJB
    IndexServiceBean indexService;
		
	@Inject
	DataverseSession session;
	
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public Dataverse save(Dataverse dataverse) {
        Dataverse savedDataverse = em.merge(dataverse);
        String indexingResult = indexService.indexDataverse(dataverse);
        logger.info("during dataverse save, indexing result was: " + indexingResult);
        return savedDataverse;
    }

    public Dataverse find(Object pk) {
        return (Dataverse) em.find(Dataverse.class, pk);
    }    
    
    public List<Dataverse> findAll() {
        return em.createQuery("select object(o) from Dataverse as o order by o.name").getResultList();
    }
    
    public List<Dataverse> findByOwnerId(Long ownerId) {
         Query query = em.createQuery("select object(o) from Dataverse as o where o.owner.id =:ownerId order by o.name");
         query.setParameter("ownerId", ownerId);
         return query.getResultList();
    }  
    
    public Dataverse findRootDataverse() {
        return (Dataverse) em.createQuery("select object(o) from Dataverse as o where o.owner.id = null").getSingleResult();
    }    

    public String determineDataversePath(Dataverse dataverse) {
        List<String> dataversePathSegments = new ArrayList();
        indexService.findPathSegments(dataverse, dataversePathSegments);
        StringBuilder dataversePath = new StringBuilder();
        for (String segment : dataversePathSegments) {
            dataversePath.append("/" + segment);
        }
        return dataversePath.toString();
    }
}
