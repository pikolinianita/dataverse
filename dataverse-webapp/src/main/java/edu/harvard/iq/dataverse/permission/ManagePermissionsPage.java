package edu.harvard.iq.dataverse.permission;

import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.authorization.DataverseRolePermissionHelper;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.notification.UserNotificationService;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.group.AuthenticatedUsers;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignee;
import edu.harvard.iq.dataverse.persistence.user.RoleAssigneeDisplayInfo;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;
import edu.harvard.iq.dataverse.util.JsfHelper;
import edu.harvard.iq.dataverse.util.StringUtil;
import io.vavr.control.Try;
import org.apache.commons.lang.StringEscapeUtils;
import javax.faces.view.ViewScoped;

import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;
import javax.inject.Named;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.harvard.iq.dataverse.util.JsfHelper.JH;

/**
 * @author gdurand
 */
@ViewScoped
@Named
public class ManagePermissionsPage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(ManagePermissionsPage.class.getCanonicalName());

    @EJB
    DvObjectServiceBean dvObjectService;
    @EJB
    DataverseRoleServiceBean roleService;
    @EJB
    RoleAssigneeServiceBean roleAssigneeService;
    @EJB
    PermissionServiceBean permissionService;
    @EJB
    UserNotificationService userNotificationService;
    @Inject
    DataverseRequestServiceBean dvRequestService;
    @Inject
    PermissionsWrapper permissionsWrapper;
    @Inject
    private DatasetDao datasetDao;
    @Inject
    private ManagePermissionsService managePermissionsService;



    @Inject
    DataverseSession session;

    private DataverseRolePermissionHelper dataverseRolePermissionHelper;
    private List<DataverseRole> roleList;

    DvObject dvObject = new Dataverse(); // by default we use a Dataverse, but this will be overridden in init by the findById

    public DvObject getDvObject() {
        return dvObject;
    }

    public void setDvObject(DvObject dvObject) {
        this.dvObject = dvObject;
        /*
        SEK 09/15/2016 - may need to do something here if permissions are transmitted/inherited from dataverse to dataverse
        */

        /*if (dvObject instanceof DvObjectContainer) {
         inheritAssignments = !((DvObjectContainer) dvObject).isPermissionRoot();
         }*/
    }

    public String init() {
        //@todo deal with any kind of dvObject
        if (dvObject.getId() != null) {
            dvObject = dvObjectService.findDvObject(dvObject.getId());
        }

        // check if dvObject exists and user has permission
        if (dvObject == null) {
            return permissionsWrapper.notFound();
        }

        // for dataFiles, check the perms on its owning dataset
        DvObject checkPermissionsdvObject = dvObject instanceof DataFile ? dvObject.getOwner() : dvObject;

        if (!isAllowedToManageDataverseOrDataset(checkPermissionsdvObject)) {
            return permissionsWrapper.notAuthorized();
        }

        if(dvObject instanceof Dataset || dvObject instanceof DataFile) {
            Dataset dataset = dvObject instanceof Dataset ? (Dataset) dvObject : ((DataFile) dvObject).getOwner();
            if (datasetDao.isInReview(dataset)
                    && !(permissionsWrapper.canIssuePublishDatasetCommand(dataset)
                    && permissionsWrapper.canManageDatasetOrMinorDatasetPermissions(dvRequestService.getDataverseRequest().getUser(), dataset))) {
                return permissionsWrapper.notAuthorized();
            }
        }

        // initialize the configure settings
        if (dvObject instanceof Dataverse) {
            initAccessSettings();
        }
        roleList = roleService.findAll();
        roleAssignments = initRoleAssignments();
        dataverseRolePermissionHelper = new DataverseRolePermissionHelper(roleList);
        return "";
    }

    private boolean isAllowedToManageDataverseOrDataset(DvObject checkPermissionsdvObject) {
        return (checkPermissionsdvObject instanceof Dataverse &&
                permissionService.on(checkPermissionsdvObject).has(Permission.ManageDataversePermissions)) ||
                permissionService.on(checkPermissionsdvObject).has(Permission.ManageDatasetPermissions) ||
                permissionService.on(checkPermissionsdvObject).has(Permission.ManageMinorDatasetPermissions);
    }

    /*
     main page - role assignment table
     */

    // used by remove Role Assignment
    private RoleAssignment selectedRoleAssignment;

    public RoleAssignment getSelectedRoleAssignment() {
        return selectedRoleAssignment;
    }

    public void setSelectedRoleAssignment(RoleAssignment selectedRoleAssignment) {
        this.selectedRoleAssignment = selectedRoleAssignment;
    }

    private List<RoleAssignmentRow> roleAssignments;

    public List<RoleAssignmentRow> getRoleAssignments() {
        return roleAssignments;
    }

    public void setRoleAssignments(List<RoleAssignmentRow> roleAssignments) {
        this.roleAssignments = roleAssignments;
    }

    public List<RoleAssignmentRow> initRoleAssignments() {

        List<RoleAssignmentRow> raList = null;
        if (dvObject != null && dvObject.getId() != null) {
            Set<RoleAssignment> ras = roleService.rolesAssignments(dvObject);
            raList = new ArrayList<>(ras.size());
            for (RoleAssignment roleAssignment : ras) {
                // for files, only show role assignments which can download
                if (!(dvObject instanceof DataFile) || roleAssignment.getRole().permissions().contains(Permission.DownloadFile)) {
                    RoleAssignee roleAssignee = roleAssigneeService.getRoleAssignee(roleAssignment.getAssigneeIdentifier());
                    if (roleAssignee != null) {
                        raList.add(new RoleAssignmentRow(roleAssignment, roleAssignee.getDisplayInfo()));
                    } else {
                        logger.info("Could not find role assignee based on role assignment id " + roleAssignment.getId());
                    }
                }
            }
        }
        return raList;
    }

    public void removeRoleAssignment() {
        removeRoleAssignment(selectedRoleAssignment);

        if (dvObject instanceof Dataverse) {
            initAccessSettings(); // in case the revoke was for the AuthenticatedUsers group
        }
        roleAssignments = initRoleAssignments();
        showAssignmentMessages();
    }

    /*
     main page - roles table
     */

    public List<DataverseRole> getRoles() {
        if (dvObject != null && dvObject.getId() != null) {
            return roleService.findByOwnerId(dvObject.getId());
        }
        return new ArrayList<>();
    }

    public void createNewRole(ActionEvent e) {
        setRole(new DataverseRole());
        role.setOwner(dvObject);
    }

    public void cloneRole(String roleId) {
        DataverseRole clonedRole = new DataverseRole();
        clonedRole.setOwner(dvObject);

        DataverseRole originalRole = roleService.find(Long.parseLong(roleId));
        clonedRole.addPermissions(originalRole.permissions());
        setRole(clonedRole);
    }

    public void editRole(String roleId) {
        setRole(roleService.find(Long.parseLong(roleId)));
    }

    /*
    ============================================================================
     edit configuration dialog // only for dataverse version of page
    ============================================================================
     */

    private String authenticatedUsersContributorRoleAlias = null;
    private String defaultContributorRoleAlias = DataverseRole.EDITOR;

    public String getAuthenticatedUsersContributorRoleAlias() {
        return authenticatedUsersContributorRoleAlias;
    }

    public void setAuthenticatedUsersContributorRoleAlias(String authenticatedUsersContributorRoleAlias) {
        this.authenticatedUsersContributorRoleAlias = authenticatedUsersContributorRoleAlias;
    }

    public String getDefaultContributorRoleAlias() {
        return defaultContributorRoleAlias;
    }

    public Boolean isCustomDefaultContributorRole() {
        if (defaultContributorRoleAlias == null) {
            initAccessSettings();
        }
        return !(defaultContributorRoleAlias.equals(DataverseRole.EDITOR) ||
                defaultContributorRoleAlias.equals(DataverseRole.CURATOR) ||
                defaultContributorRoleAlias.equals(DataverseRole.DEPOSITOR));
    }

    public String getCustomDefaultContributorRoleName() {
        if (dvObject instanceof Dataverse && isCustomDefaultContributorRole()) {
            return defaultContributorRoleAlias.equals(DataverseRole.NONE) ? BundleUtil.getStringFromBundle("permission.default.contributor.role.none.name") : roleService.findCustomRoleByAliasAndOwner(defaultContributorRoleAlias, dvObject.getId()).getName();
        } else {
            return "";
        }
    }

    public String getCustomDefaultContributorRoleAlias() {
        if (dvObject instanceof Dataverse && isCustomDefaultContributorRole()) {
            return defaultContributorRoleAlias.equals(DataverseRole.NONE) ? DataverseRole.NONE : roleService.findCustomRoleByAliasAndOwner(defaultContributorRoleAlias, dvObject.getId()).getAlias();
        } else {
            return "";
        }
    }

    public void setCustomDefaultContributorRoleAlias(String dummy) {
        //dummy method for interface
    }

    public void setCustomDefaultContributorRoleName(String dummy) {
        //dummy method for interface
    }

    public String getCustomDefaultContributorRoleDescription() {
        if (dvObject instanceof Dataverse && isCustomDefaultContributorRole()) {
            return defaultContributorRoleAlias.equals(DataverseRole.NONE) ? BundleUtil.getStringFromBundle("permission.default.contributor.role.none.decription") : roleService.findCustomRoleByAliasAndOwner(defaultContributorRoleAlias, dvObject.getId()).getDescription();
        } else {
            return "";
        }
    }

    public void setCustomDefaultContributorRoleDescription(String dummy) {
        //dummy method for interface
    }

    public void setDefaultContributorRoleAlias(String defaultContributorRoleAlias) {
        this.defaultContributorRoleAlias = defaultContributorRoleAlias;
    }

    public void initAccessSettings() {
        if (dvObject instanceof Dataverse) {
            authenticatedUsersContributorRoleAlias = "";

            List<RoleAssignment> aUsersRoleAssignments = roleService.directRoleAssignments(AuthenticatedUsers.get(), dvObject);
            for (RoleAssignment roleAssignment : aUsersRoleAssignments) {
                String roleAlias = roleAssignment.getRole().getAlias();
                authenticatedUsersContributorRoleAlias = roleAlias;
                break;
                // @todo handle case where more than one role has been assigned to the AutenticatedUsers group!
            }

            defaultContributorRoleAlias = ((Dataverse) dvObject).getDefaultContributorRole() == null ? DataverseRole.NONE : ((Dataverse) dvObject).getDefaultContributorRole().getAlias();
        }
    }


    public void saveConfiguration(ActionEvent e) {
        // Set role (if any) for authenticatedUsers
        DataverseRole roleToAssign = null;
        List<String> contributorRoles = Arrays.asList(DataverseRole.FULL_CONTRIBUTOR, DataverseRole.DV_CONTRIBUTOR, DataverseRole.DS_CONTRIBUTOR);

        if (!StringUtil.isEmpty(authenticatedUsersContributorRoleAlias)) {
            roleToAssign = roleService.findBuiltinRoleByAlias(authenticatedUsersContributorRoleAlias);
        }

        // then, check current contributor role
        List<RoleAssignment> aUsersRoleAssignments = roleService.directRoleAssignments(AuthenticatedUsers.get(), dvObject);
        for (RoleAssignment roleAssignment : aUsersRoleAssignments) {
            DataverseRole currentRole = roleAssignment.getRole();
            if (contributorRoles.contains(currentRole.getAlias())) {
                if (currentRole.equals(roleToAssign)) {
                    roleToAssign = null; // found the role, so no need to assign
                } else {
                    removeRoleAssignment(roleAssignment);
                }
            }
        }
        // finally, assign role, if new
        if (roleToAssign != null) {
            assignRole(AuthenticatedUsers.get(), roleToAssign);
        }

        // set dataverse default contributor role
        if (dvObject instanceof Dataverse) {
            Dataverse dv = (Dataverse) dvObject;
            DataverseRole defaultRole = roleService.findBuiltinRoleByAlias(defaultContributorRoleAlias);
            if (!defaultRole.equals(dv.getDefaultContributorRole())) {
                Try.of(() -> managePermissionsService.setDataverseDefaultContributorRole(defaultRole, dv))
                        .onSuccess(dataverse -> JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("permission.defaultPermissionDataverseUpdated")))
                        .onFailure(this::handleSetDataverseDefaultContributorRoleFailure)
                ;
            }
        }
        roleAssignments = initRoleAssignments();
        showConfigureMessages();
    }

    /*
   ============================================================================
     assign roles dialog
   ============================================================================
     */
    private List<RoleAssignee> roleAssignSelectedRoleAssignees;
    private Long selectedRoleId;

    public List<RoleAssignee> getRoleAssignSelectedRoleAssignees() {
        return roleAssignSelectedRoleAssignees;
    }

    public void setRoleAssignSelectedRoleAssignees(List<RoleAssignee> selectedRoleAssignees) {
        this.roleAssignSelectedRoleAssignees = selectedRoleAssignees;
    }

    public Long getSelectedRoleId() {
        return selectedRoleId;
    }

    public void setSelectedRoleId(Long selectedRoleId) {
        this.selectedRoleId = selectedRoleId;
    }

    public void initAssigneeDialog(ActionEvent ae) {
        roleAssignSelectedRoleAssignees = new LinkedList<>();
        selectedRoleId = null;
        showNoMessages();
    }

    public List<RoleAssignee> completeRoleAssignee(String query) {
        return roleAssigneeService.filterRoleAssignees(query, dvObject, roleAssignSelectedRoleAssignees);
    }

    public List<DataverseRole> getAvailableRoles() {
        List<DataverseRole> roles = new LinkedList<>();
        if (dvObject != null && dvObject.getId() != null) {

            if (dvObject instanceof Dataverse) {
                roles.addAll(roleService.availableRoles(dvObject.getId()));

            } else if (dvObject instanceof Dataset) {
                // don't show roles that only have Dataverse level permissions
                // current the available roles for a dataset are gotten from its parent
                for (DataverseRole role : roleService.availableRoles(dvObject.getOwner().getId())) {
                    for (Permission permission : role.permissions()) {
                        if (permission.appliesTo(Dataset.class) || permission.appliesTo(DataFile.class)) {
                            if (isHasPermission(Permission.ManageMinorDatasetPermissions)
                                    && isAllowedToManageRole(role) || isHasPermission(Permission.ManageDatasetPermissions)) {
                                roles.add(role);
                            }
                            break;
                        }
                    }
                }

            } else if (dvObject instanceof DataFile) {
                roles.add(roleService.findBuiltinRoleByAlias(DataverseRole.FILE_DOWNLOADER));
            }

            Collections.sort(roles, DataverseRole.CMP_BY_NAME);
        }
        return roles;
    }

    public DataverseRole getAssignedRole() {
        if (selectedRoleId != null) {
            return roleService.find(selectedRoleId);
        }
        return null;
    }

    public void assignRole(ActionEvent evt) {
        logger.info("Got to assignRole");
        List<RoleAssignee> selectedRoleAssigneesList = getRoleAssignSelectedRoleAssignees();
        if (selectedRoleAssigneesList == null) {
            logger.info("** SELECTED role asignees is null");
            selectedRoleAssigneesList = new LinkedList<>();
        }
        for (RoleAssignee roleAssignee : selectedRoleAssigneesList) {
            assignRole(roleAssignee, roleService.find(selectedRoleId));
        }
        roleAssignments = initRoleAssignments();
    }

    private void assignRole(RoleAssignee ra, DataverseRole r) {
        List<String> args = Arrays.asList(
                r.getName(),
                ra.getDisplayInfo().getTitle(),
                StringEscapeUtils.escapeHtml(dvObject.getDisplayName())
        );

        Try.of(() -> managePermissionsService.assignRoleWithNotification(r, ra, dvObject))
                .onSuccess(roleAssignment -> JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("permission.roleAssignedToFor", args)))
                .onFailure(throwable -> handleAssignRoleFailure(throwable, args));

        showAssignmentMessages();
    }

    private boolean isAllowedToManageRole(DataverseRole role) {
        return DataverseRolePermissionHelper.getRolesAllowedToBeAssignedByManageMinorDatasetPermissions().contains(role.getAlias());
    }

    private boolean isHasPermission(Permission manageMinorDatasetPermissions) {
        return permissionService.userOn(this.session.getUser(), this.dvObject)
                .has(manageMinorDatasetPermissions);
    }

    /*
    ============================================================================
     edit role dialog
    ============================================================================
    */
    private DataverseRole role = new DataverseRole();
    private List<String> selectedPermissions;

    public DataverseRole getRole() {
        return role;
    }

    public void setRole(DataverseRole role) {
        this.role = role;
        selectedPermissions = new LinkedList<>();
        if (role != null) {
            for (Permission p : role.permissions()) {
                selectedPermissions.add(p.name());
            }
        }
    }

    public List<String> getSelectedPermissions() {
        return selectedPermissions;
    }

    public void setSelectedPermissions(List<String> selectedPermissions) {
        this.selectedPermissions = selectedPermissions;
    }

    public List<Permission> getPermissions() {
        return Arrays.asList(Permission.values());
    }

    public void updateRole(ActionEvent e) {
        // @todo currently only works for Dataverse since CreateRoleCommand only takes a dataverse
        // we need to decide if we want roles at the dataset level or not
        if (dvObject instanceof Dataverse) {
            role.clearPermissions();
            for (String pmsnStr : getSelectedPermissions()) {
                role.addPermission(Permission.valueOf(pmsnStr));
            }
            Try.of(() -> managePermissionsService.saveOrUpdateRole(role))
                    .onSuccess(this::setRole)
                    .onSuccess(modifiedRole -> {
                        String roleState = role.getId() != null ? BundleUtil.getStringFromBundle("permission.updated") : BundleUtil.getStringFromBundle("permission.created");
                        JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("permission.roleWas", Collections.singletonList(roleState)));
                    })
                    .onFailure(this::handleUpdateRoleFailure);
        }

        showRoleMessages();
    }

    /*
    ============================================================================
    Internal methods
    ============================================================================
    */

    private boolean renderConfigureMessages = false;
    private boolean renderAssignmentMessages = false;
    private boolean renderRoleMessages = false;

    private void showNoMessages() {
        renderConfigureMessages = false;
        renderAssignmentMessages = false;
        renderRoleMessages = false;
    }

    private void showConfigureMessages() {
        renderConfigureMessages = true;
        renderAssignmentMessages = false;
        renderRoleMessages = false;
    }

    private void showAssignmentMessages() {
        renderConfigureMessages = false;
        renderAssignmentMessages = true;
        renderRoleMessages = false;
    }

    private void showRoleMessages() {
        renderConfigureMessages = false;
        renderAssignmentMessages = false;
        renderRoleMessages = true;
    }

    public Boolean getRenderConfigureMessages() {
        return renderConfigureMessages;
    }

    public void setRenderConfigureMessages(Boolean renderConfigureMessages) {
        this.renderConfigureMessages = renderConfigureMessages;
    }

    public Boolean getRenderAssignmentMessages() {
        return renderAssignmentMessages;
    }

    public void setRenderAssignmentMessages(Boolean renderAssignmentMessages) {
        this.renderAssignmentMessages = renderAssignmentMessages;
    }

    public Boolean getRenderRoleMessages() {
        return renderRoleMessages;
    }

    public void setRenderRoleMessages(Boolean renderRoleMessages) {
        this.renderRoleMessages = renderRoleMessages;
    }

    // inner class used for display of role assignments
    public static class RoleAssignmentRow {

        private final RoleAssigneeDisplayInfo assigneeDisplayInfo;
        private final RoleAssignment ra;

        RoleAssignmentRow(RoleAssignment anRa, RoleAssigneeDisplayInfo disInf) {
            ra = anRa;
            assigneeDisplayInfo = disInf;
        }

        public RoleAssignment getRoleAssignment() {
            return ra;
        }

        public RoleAssigneeDisplayInfo getAssigneeDisplayInfo() {
            return assigneeDisplayInfo;
        }

        public DataverseRole getRole() {
            return ra.getRole();
        }

        public String getRoleName() {
            return getRole().getName();
        }


        public DvObject getDefinitionPoint() {
            return ra.getDefinitionPoint();
        }

        public String getAssignedDvName() {
            return ra.getDefinitionPoint().getDisplayName();
        }

        public Long getId() {
            return ra.getId();
        }

    }

    // -------------------- PRIVATE ---------------------
    private void removeRoleAssignment(RoleAssignment ra) {
        Try.run(() -> managePermissionsService.removeRoleAssignmentWithNotification(ra))
                .onSuccess(Void -> {
                    JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("permission.roleWasRemoved",
                            Arrays.asList(ra.getRole().getName(),
                                    roleAssigneeService.getRoleAssignee(ra.getAssigneeIdentifier()).getDisplayInfo().getTitle())));
                })
                .onFailure(this::handleRemoveRoleAssignmentFailure);
    }

    private void handleRemoveRoleAssignmentFailure(Throwable throwable) {
        if(throwable instanceof PermissionException) {
            JH.addMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("permission.roleNotAbleToBeRemoved"),
                    BundleUtil.getStringFromBundle("permission.permissionsMissing",
                            Collections.singletonList(((PermissionException) throwable).getRequiredPermissions().toString())));
        } else if (throwable instanceof CommandException) {
            JH.addMessage(FacesMessage.SEVERITY_FATAL, BundleUtil.getStringFromBundle("permission.roleNotAbleToBeRemoved"));
            logger.log(Level.SEVERE, "Error removing role assignment: " + throwable.getMessage(), throwable);
        }
    }

    private void handleUpdateRoleFailure(Throwable throwable) {
        if (throwable instanceof PermissionException) {
            JH.addMessage(FacesMessage.SEVERITY_ERROR,
                    BundleUtil.getStringFromBundle("permission.roleNotSaved"),
                    BundleUtil.getStringFromBundle("permission.permissionsMissing",
                            Collections.singletonList(((PermissionException) throwable).getRequiredPermissions().toString())));
        } else if (throwable instanceof CommandException) {
            JH.addMessage(FacesMessage.SEVERITY_FATAL, BundleUtil.getStringFromBundle("permission.roleNotSaved"));
            logger.log(Level.SEVERE, "Error saving role: " + throwable.getMessage(), throwable);
        }
    }

    private void handleAssignRoleFailure(Throwable throwable, List<String> messageDetails) {
        if (throwable instanceof PermissionException) {
            JH.addMessage(FacesMessage.SEVERITY_ERROR,
                    BundleUtil.getStringFromBundle("permission.roleNotAbleToBeAssigned"),
                    BundleUtil.getStringFromBundle("permission.permissionsMissing",
                            Collections.singletonList(((PermissionException) throwable).getRequiredPermissions().toString())));

        } else if (throwable instanceof CommandException) {
            String message = BundleUtil.getStringFromBundle("permission.roleNotAssignedFor", messageDetails);
            JsfHelper.addFlashErrorMessage(message);
            logger.log(Level.SEVERE, "Error assiging role: " + throwable.getMessage(), throwable);
        }
    }

    private void handleSetDataverseDefaultContributorRoleFailure(Throwable throwable) {
        if(throwable instanceof PermissionException) {
            JH.addMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("permission.CannotAssigntDefaultPermissions"),
                    BundleUtil.getStringFromBundle("permission.permissionsMissing",
                            Collections.singletonList(((PermissionException) throwable).getRequiredPermissions().toString())));
        } else if (throwable instanceof CommandException) {
            JH.addMessage(FacesMessage.SEVERITY_FATAL, BundleUtil.getStringFromBundle("permission.CannotAssigntDefaultPermissions"));
            logger.log(Level.SEVERE, "Error assigning default permissions: " + throwable.getMessage(), throwable);
        }
    }
}
