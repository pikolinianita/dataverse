package edu.harvard.iq.dataverse.dataset.datasetversion;

import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.guestbook.GuestbookServiceBean;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import static org.junit.Assert.assertEquals;

@RunWith(Arquillian.class)
@Transactional(TransactionMode.ROLLBACK)
public class DatasetVersionServiceBeanIT extends WebappArquillianDeployment {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    @Inject
    private DatasetVersionServiceBean datasetVersionService;
    @Inject
    private DatasetServiceBean datasetService;
    @Inject
    private GuestbookServiceBean guestbookService;
    @Inject
    private DataverseSession dataverseSession;
    @EJB
    private AuthenticationServiceBean authenticationServiceBean;

    @Before
    public void setUp() {
        dataverseSession.setUser(authenticationServiceBean.getAdminUser());
    }

    @Test
    public void shouldUpdateDatasetVersion() {
        // given
        Dataset dataset = datasetService.find(56L);

        // when
        dataset.setGuestbook(guestbookService.find(2L));
        datasetVersionService.updateDatasetVersion(dataset.getEditVersion(), true);

        // then
        Dataset dbDataset = datasetService.find(56L);
        assertEquals(2L, (long) dbDataset.getGuestbook().getId());
    }
}