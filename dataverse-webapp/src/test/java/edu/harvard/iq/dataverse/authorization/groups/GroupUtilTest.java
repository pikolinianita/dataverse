package edu.harvard.iq.dataverse.authorization.groups;

import edu.harvard.iq.dataverse.persistence.group.AllUsers;
import edu.harvard.iq.dataverse.persistence.group.AuthenticatedUsers;
import edu.harvard.iq.dataverse.persistence.group.Group;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import org.junit.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class GroupUtilTest {

    @Test
    public void testGetAllIdentifiersForUserWithGroups() {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser();
        authenticatedUser.setUserIdentifier("pete");
        Set<Group> groups = new LinkedHashSet<>();
        groups.add(AuthenticatedUsers.get());
        groups.add(AllUsers.get());
        String expected = "'@pete', ':authenticated-users', ':AllUsers'";
        String actual = GroupUtil.getAllIdentifiersForUser(authenticatedUser, groups);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetAllIdentifiersForUserWithNoGroups() {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser();
        authenticatedUser.setUserIdentifier("pete");
        Set<Group> groups = new LinkedHashSet<>();
        String expected = "'@pete'";
        String actual = GroupUtil.getAllIdentifiersForUser(authenticatedUser, groups);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetAllIdentifiersForUserNulls() {
        new GroupUtil(); // just boosting our coverage :)
        AuthenticatedUser authenticatedUser = new AuthenticatedUser();
        authenticatedUser.setUserIdentifier("pete");
        assertEquals(null, GroupUtil.getAllIdentifiersForUser(null, null));
        assertEquals("'@pete'", GroupUtil.getAllIdentifiersForUser(authenticatedUser, null));
    }

}
