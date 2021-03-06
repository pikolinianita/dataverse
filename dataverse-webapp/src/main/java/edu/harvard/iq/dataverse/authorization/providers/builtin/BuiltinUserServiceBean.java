package edu.harvard.iq.dataverse.authorization.providers.builtin;

import edu.harvard.iq.dataverse.passwordreset.PasswordResetException;
import edu.harvard.iq.dataverse.passwordreset.PasswordResetInitResponse;
import edu.harvard.iq.dataverse.passwordreset.PasswordResetServiceBean;
import edu.harvard.iq.dataverse.persistence.user.BuiltinUser;
import edu.harvard.iq.dataverse.persistence.user.PasswordResetData;
import edu.harvard.iq.dataverse.search.index.IndexServiceBean;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author xyang
 */
@Stateless
public class BuiltinUserServiceBean {

    private static final Logger logger = Logger.getLogger(BuiltinUserServiceBean.class.getCanonicalName());

    @EJB
    IndexServiceBean indexService;

    @EJB
    PasswordResetServiceBean passwordResetService;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public String encryptPassword(String plainText) {
        return PasswordEncryption.get().encrypt(plainText);
    }

    public BuiltinUser save(BuiltinUser aUser) {
        /**
         * We throw a proper IllegalArgumentException here because otherwise
         * from the API you get a 500 response and "Can't save user: null".
         */
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        Set<ConstraintViolation<BuiltinUser>> violations = validator.validate(aUser);
        if (violations.size() > 0) {
            StringBuilder sb = new StringBuilder();
            violations.stream().forEach((violation) -> {
                sb.append(" Invalid value: <<<").append(violation.getInvalidValue()).append(">>> for ").append(violation.getPropertyPath()).append(" at ").append(violation.getLeafBean()).append(" - ").append(violation.getMessage());
            });
            throw new IllegalArgumentException("BuiltinUser could not be saved to due constraint violations: " + sb);
        }
        if (aUser.getId() == null) {
            // see that the username is unique
            if (em.createNamedQuery("BuiltinUser.findByUserName")
                    .setParameter("userName", aUser.getUserName()).getResultList().size() > 0) {
                throw new IllegalArgumentException("BuiltinUser with username '" + aUser.getUserName() + "' already exists.");
            }
            em.persist(aUser);
            return aUser;
        } else {
            return em.merge(aUser);
        }
    }

    public BuiltinUser find(Long pk) {
        return em.find(BuiltinUser.class, pk);
    }

    public void removeUser(String userName) {
        final BuiltinUser user = findByUserName(userName);
        if (user != null) {
            em.remove(user);
        }
    }

    public BuiltinUser findByUserName(String userName) {
        try {
            return em.createNamedQuery("BuiltinUser.findByUserName", BuiltinUser.class)
                    .setParameter("userName", userName)
                    .getSingleResult();
        } catch (javax.persistence.NoResultException e) {
            return null;
        } catch (NonUniqueResultException ex) {
            logger.log(Level.WARNING, "multiple accounts found for username {0}", userName);
            return null;
        }
    }

    public List<BuiltinUser> listByUsernamePart(String part) {
        return em.createNamedQuery("BuiltinUser.listByUserNameLike", BuiltinUser.class)
                .setParameter("userNameLike", "%" + part + "%")
                .getResultList();
    }

    public List<BuiltinUser> findAll() {
        return em.createNamedQuery("BuiltinUser.findAll", BuiltinUser.class).getResultList();
    }

    public String requestPasswordUpgradeLink(BuiltinUser aUser) throws PasswordResetException {
        PasswordResetInitResponse prir = passwordResetService.requestPasswordReset(aUser, false, PasswordResetData.Reason.UPGRADE_REQUIRED);
        return "passwordreset.xhtml?token=" + prir.getPasswordResetData().getToken() + "&faces-redirect=true";
    }

    public String requestPasswordComplianceLink(BuiltinUser aUser) throws PasswordResetException {
        PasswordResetInitResponse prir = passwordResetService.requestPasswordReset(aUser, false, PasswordResetData.Reason.NON_COMPLIANT_PASSWORD);
        return "passwordreset.xhtml?token=" + prir.getPasswordResetData().getToken() + "&faces-redirect=true";
    }
}
