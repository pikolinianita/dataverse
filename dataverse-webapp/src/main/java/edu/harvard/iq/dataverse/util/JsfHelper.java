package edu.harvard.iq.dataverse.util;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for common JSF tasks.
 *
 * @author michael
 */
public class JsfHelper {
    private static final Logger logger = Logger.getLogger(JsfHelper.class.getName());

    public static final JsfHelper JH = new JsfHelper();

    public static void addFlashSuccessMessage(String message) {
        FacesContext.getCurrentInstance().getExternalContext().getFlash().put("successMsg", message);

    }

    public static void addFlashMessage(String message) {
        addFlashSuccessMessage(message);
    }

    public static void addFlashErrorMessage(String message) {
        FacesContext.getCurrentInstance().getExternalContext().getFlash().put("errorMsg", message);
    }

    public static void addFlashInfoMessage(String message) {
        FacesContext.getCurrentInstance().getExternalContext().getFlash().put("infoMsg", message);
    }

    public static void addFlashWarningMessage(String message) {
        FacesContext.getCurrentInstance().getExternalContext().getFlash().put("warningMsg", message);
    }

    public static void addErrorMessage(String componentId, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(componentId,
                                                     new FacesMessage(FacesMessage.SEVERITY_ERROR, summary, detail));
    }

    public void addMessage(FacesMessage.Severity s, String summary, String details) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(s, summary, details));
    }

    public void addMessage(FacesMessage.Severity s, String summary) {
        addMessage(s, summary, "");
    }

    public <T extends Enum<T>> T enumValue(String param, Class<T> enmClass, T defaultValue) {
        if (param == null) {
            return defaultValue;
        }
        param = param.trim();
        try {
            return Enum.valueOf(enmClass, param);
        } catch (IllegalArgumentException iar) {
            logger.log(Level.WARNING, "Illegal value for enum {0}: ''{1}''", new Object[]{enmClass.getName(), param});
            return defaultValue;
        }
    }
}
