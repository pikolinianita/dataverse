package edu.harvard.iq.dataverse.persistence.user;

import edu.harvard.iq.dataverse.common.DateUtil;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

/**
 * @author xyang
 */
@Entity
@Table(indexes = {@Index(columnList = "user_id")})

public class UserNotification implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(nullable = false)
    private AuthenticatedUser user;
    @ManyToOne
    @JoinColumn(nullable = true)
    private AuthenticatedUser requestor;
    private Timestamp sendDate;
    private boolean readNotification;

    @Enumerated
    @Column(nullable = false)
    private NotificationType type;
    private Long objectId;

    @Column(nullable = true)
    private String returnToAuthorReason;

    @Transient
    private boolean displayAsRead;

    @Transient
    String roleString;

    private boolean emailed;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AuthenticatedUser getUser() {
        return user;
    }

    public void setUser(AuthenticatedUser user) {
        this.user = user;
    }

    public AuthenticatedUser getRequestor() {
        return requestor;
    }

    public void setRequestor(AuthenticatedUser requestor) {
        this.requestor = requestor;
    }

    public String getSendDate() {
        return new SimpleDateFormat("MMMM d, yyyy h:mm a z").format(sendDate);
    }

    public void setSendDate(Timestamp sendDate) {
        this.sendDate = sendDate;
    }

    public boolean isReadNotification() {
        return readNotification;
    }

    public void setReadNotification(boolean readNotification) {
        this.readNotification = readNotification;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public Long getObjectId() {
        return objectId;
    }

    public void setObjectId(Long objectId) {
        this.objectId = objectId;
    }

    /**
     * required for {@link NotificationType.RETURNEDDS} only
     * @return provided by user reason for dataset publish rejection
     */
    public String getReturnToAuthorReason() {
        return returnToAuthorReason;
    }

    public void setReturnToAuthorReason(String returnToAuthorReason) {
        this.returnToAuthorReason = returnToAuthorReason;
    }

    @Transient
    private Object theObject;

    public Object getTheObject() {
        return theObject;
    }

    public void setTheObject(Object theObject) {
        this.theObject = theObject;
    }


    public boolean isDisplayAsRead() {
        return displayAsRead;
    }

    public void setDisplayAsRead(boolean displayAsRead) {
        this.displayAsRead = displayAsRead;
    }

    public boolean isEmailed() {
        return emailed;
    }

    public void setEmailed(boolean emailed) {
        this.emailed = emailed;
    }

    public String getRoleString() {
        return roleString;
    }

    public void setRoleString(String roleString) {
        this.roleString = roleString;
    }

    public String getLocaleSendDate() {
        return DateUtil.formatDate(sendDate);
    }
}
