package edu.harvard.iq.dataverse;

import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.util.List;

/**
 *
 * @author michael
 */
@Stateless
@Named
public class MetadataBlockDao {
    
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    // -------------------- LOGIC --------------------

    public MetadataBlock save(MetadataBlock mdb) {
       return em.merge(mdb);
    }
    
    public List<MetadataBlock> listMetadataBlocks() {
        return em.createNamedQuery("MetadataBlock.listAll", MetadataBlock.class).getResultList();
    }
    
    public MetadataBlock findById( Long id ) {
        return em.find(MetadataBlock.class, id);
    }
    
    public MetadataBlock findByName( String name ) {
        try {
            return em.createNamedQuery("MetadataBlock.findByName", MetadataBlock.class)
                        .setParameter("name", name)
                        .getSingleResult();
        } catch ( NoResultException nre ) {
            return null;
        }
    }
}