package edu.harvard.iq.dataverse.persistence.group;

import edu.harvard.iq.dataverse.persistence.user.RoleAssigneeDisplayInfo;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;

import java.io.Serializable;

/**
 * Convenience base class for implementing groups that apply to the entire Dataverse
 * installation, and are persisted to the DB.
 *
 * @author michael
 */
@NamedQueries({
        @NamedQuery(name = "PersistedGlobalGroup.persistedGroupAlias",
                query = "SELECT g FROM PersistedGlobalGroup g WHERE g.persistedGroupAlias=:persistedGroupAlias")
})
@Entity
@Table(indexes = {@Index(columnList = "dtype")})
public abstract class PersistedGlobalGroup implements Group, Serializable {

    @Id
    @GeneratedValue
    private Long id;

    /**
     * A unique alias within the Dataverse system installation.
     */
    @Column(unique = true)
    private String persistedGroupAlias;
    private String displayName;
    private String description;

    @Transient
    private String groupProviderAlias;
    
    public PersistedGlobalGroup() {
    }
    
    public PersistedGlobalGroup(String groupProviderAlias) {
        this.groupProviderAlias = groupProviderAlias;
    }
    
    public void setPersistedGroupAlias(String alias) {
        this.persistedGroupAlias = alias;
    }

    public String getPersistedGroupAlias() {
        return persistedGroupAlias;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getAlias() {
        return groupProviderAlias + Group.PATH_SEPARATOR + persistedGroupAlias;
    }

    public void setDisplayName(String name) {
        this.displayName = name;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public RoleAssigneeDisplayInfo getDisplayInfo() {
        return new RoleAssigneeDisplayInfo(displayName, null);
    }

    @Override
    public String getIdentifier() {
        return Group.IDENTIFIER_PREFIX + getAlias();
    }

    @Override
    public String toString() {
        return "[PersistedGlobalGroup " + getIdentifier() + "]";
    }


}
