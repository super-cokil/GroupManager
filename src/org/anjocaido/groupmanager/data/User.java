/*
 *  GroupManager - A plug-in for Spigot/Bukkit based Minecraft servers.
 *  Copyright (C) 2020  ElgarL
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.anjocaido.groupmanager.data;

//import com.sun.org.apache.bcel.internal.generic.AALOAD;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.dataholder.WorldDataHolder;
import org.anjocaido.groupmanager.events.GMUserEvent.Action;
import org.anjocaido.groupmanager.localization.Messages;
import org.anjocaido.groupmanager.utils.BukkitWrapper;
import org.bukkit.entity.Player;

/**
 * 
 * @author gabrielcouto/ElgarL
 */
public class User extends DataUnit implements Cloneable {

	/**
     *
     */
	private String group = null;
	private final List<String> subGroups = Collections.synchronizedList(new ArrayList<String>());
	/**
	 * This one holds the fields in INFO node. like prefix = 'c' or build =
	 * false
	 */
	private UserVariables variables = new UserVariables(this);

	/**
	 * 
	 * @param name
	 */
	public User(WorldDataHolder source, String name) {

		super(source, name);
		this.group = source.getDefaultGroup().getName();
	}

	/**
	 * 
	 * @return User clone
	 */
	@Override
	public User clone() {

		User clone = new User(getDataSource(), this.getLastName());
		clone.group = this.group;
		
		// Clone all subgroups.
		clone.subGroups.addAll(this.subGroupListStringCopy());
		
		// Clone permissions
		for (String perm : this.getPermissionList()) {
			clone.addPermission(perm);
		}
		// Clone timed permissions.
		for (Entry<String, Long> perm : this.getTimedPermissions().entrySet()) {
			clone.addTimedPermission(perm.getKey(), perm.getValue());
		}
		// clone.variables = this.variables.clone();
		// clone.flagAsChanged();
		return clone;
	}

	/**
	 * Use this to deliver a user from one WorldDataHolder to another
	 * 
	 * @param dataSource
	 * @return null if given dataSource already contains the same user
	 */
	public User clone(WorldDataHolder dataSource) {

		if (dataSource.isUserDeclared(this.getUUID())) {
			return null;
		}
		
		User clone = dataSource.createUser(this.getUUID());
		
		if (dataSource.getGroup(group) == null) {
			clone.setGroup(dataSource.getDefaultGroup());
		} else {
			clone.setGroup(dataSource.getGroup(this.getGroupName()));
		}
		
		// Clone all subgroups.
		clone.subGroups.addAll(this.subGroupListStringCopy());
				
		// Clone permissions
		for (String perm : this.getPermissionList()) {
			clone.addPermission(perm);
		}
		
		// Clone timed permissions.
		for (Entry<String, Long> perm : this.getTimedPermissions().entrySet()) {
			clone.addTimedPermission(perm.getKey(), perm.getValue());
		}
		
		clone.variables = this.variables.clone(this);
		clone.flagAsChanged();
		return clone;
	}
	
	public User clone(String uUID, String CurrentName) {

		User clone = this.getDataSource().createUser(uUID);
		
		clone.setLastName(CurrentName);
		
		// Set the group silently.
		clone.setGroup(this.getDataSource().getGroup(this.getGroupName()), false);
		
		// Clone all subgroups.
		clone.subGroups.addAll(this.subGroupListStringCopy());
		
		// Clone permissions
		for (String perm : this.getPermissionList()) {
			clone.addPermission(perm);
		}
		
		// Clone timed permissions.
		for (Entry<String, Long> perm : this.getTimedPermissions().entrySet()) {
			clone.addTimedPermission(perm.getKey(), perm.getValue());
		}
		
		clone.variables = this.variables.clone(this);
		clone.flagAsChanged();
		
		return clone;
	}

	/**
	 * Gets the main group this user is a member of.
	 * 
	 * @return the group.
	 */
	public Group getGroup() {

		Group result = getDataSource().getGroup(group);
		if (result == null) {
			this.setGroup(getDataSource().getDefaultGroup());
			result = getDataSource().getDefaultGroup();
		}
		return result;
	}

	/**
	 * Gets the main group name this user is a member of.
	 * 
	 * @return the group name.
	 */
	public String getGroupName() {

		Group result = getDataSource().getGroup(group);
		if (result == null) {
			group = getDataSource().getDefaultGroup().getName();
		}
		return group;
	}
	
	/**
	 * Place holder to let people know to stop using this method.
	 * 
	 * @deprecated use {@link #getLastName()} and {@link #getUUID()}.
	 * @return a string containing the players last known name.
	 */
	@Deprecated
	public String getName() {
		
		return this.getLastName();
		
	}

	/**
	 * @param group the group to set
	 */
	public void setGroup(Group group) {

		setGroup(group, true);
	}

	/**
	 * @param group the group to set
	 * @param updatePerms if we are to trigger a superperms update.
	 * 
	 */
	public void setGroup(Group group, Boolean updatePerms) {

		if (!this.getDataSource().groupExists(group.getName())) {
			getDataSource().addGroup(group);
		}
		group = getDataSource().getGroup(group.getName());
		String oldGroup = this.group;
		this.group = group.getName();
		flagAsChanged();
		if (GroupManager.isLoaded()) {
			if (!GroupManager.getBukkitPermissions().isPlayer_join() && (updatePerms))
				GroupManager.getBukkitPermissions().updatePlayer(getBukkitPlayer());

			// Do we notify of the group change?
			String defaultGroupName = getDataSource().getDefaultGroup().getName();
			// if we were not in the default group
			// or we were in the default group and the move is to a different
			// group.
			boolean notify = (!oldGroup.equalsIgnoreCase(defaultGroupName)) || ((oldGroup.equalsIgnoreCase(defaultGroupName)) && (!this.group.equalsIgnoreCase(defaultGroupName)));

			if (notify)
				GroupManager.notify(this.getLastName(), String.format(Messages.getString("MOVED_TO_GROUP"), group.getName(), this.getDataSource().getName()));

			if (updatePerms)
				GroupManager.getGMEventHandler().callEvent(this, Action.USER_GROUP_CHANGED);
		}
	}

	public boolean addSubGroup(Group subGroup) {

		// Don't allow adding a subgroup if it's already set as the primary.
		if (this.group.equalsIgnoreCase(subGroup.getName())) {
			return false;
		}
		// User already has this subgroup
		if (containsSubGroup(subGroup))
			return false;

		// If the group doesn't exists add it
		if (!this.getDataSource().groupExists(subGroup.getName())) {
			getDataSource().addGroup(subGroup);
		}

		subGroups.add(subGroup.getName());
		flagAsChanged();
		if (GroupManager.isLoaded()) {
			if (!GroupManager.getBukkitPermissions().isPlayer_join())
				GroupManager.getBukkitPermissions().updatePlayer(getBukkitPlayer());
			GroupManager.getGMEventHandler().callEvent(this, Action.USER_SUBGROUP_CHANGED);
		}
		return true;

		//subGroup = getDataSource().getGroup(subGroup.getName());
		//removeSubGroup(subGroup);
		//subGroups.add(subGroup.getName());
	}

	public int subGroupsSize() {

		return subGroups.size();
	}

	public boolean isSubGroupsEmpty() {

		return subGroups.isEmpty();
	}

	public boolean containsSubGroup(Group subGroup) {

		return subGroups.contains(subGroup.getName());
	}

	public boolean removeSubGroup(Group subGroup) {

		try {
			if (subGroups.remove(subGroup.getName())) {
				flagAsChanged();
				if (GroupManager.isLoaded())
					if (!GroupManager.getBukkitPermissions().isPlayer_join())
						GroupManager.getBukkitPermissions().updatePlayer(getBukkitPlayer());
				GroupManager.getGMEventHandler().callEvent(this, Action.USER_SUBGROUP_CHANGED);
				return true;
			}
		} catch (Exception e) {
		}
		return false;
	}

	/**
	 * Returns a new array of the Sub-Groups attached to this user.
	 * 
	 * @return List of sub-groups.
	 */
	public ArrayList<Group> subGroupListCopy() {

		ArrayList<Group> val = new ArrayList<Group>();
		synchronized(subGroups) {
			for (String gstr : subGroups) {
				Group g = getDataSource().getGroup(gstr);
				if (g == null) {
					removeSubGroup(g);
					continue;
				}
				val.add(g);
			}
		}
		return val;
	}

	/**
	 * Compiles a list of Sub-Group Names attached to this user.
	 * 
	 * @return	List of sub-group names.
	 */
	public ArrayList<String> subGroupListStringCopy() {
		synchronized(subGroups) {
			return new ArrayList<String>(subGroups);
		}
	}

	/**
	 * @return the variables
	 */
	public UserVariables getVariables() {

		return variables;
	}

	/**
	 * 
	 * @param varList
	 */
	public void setVariables(Map<String, Object> varList) {

		//UserVariables temp = new UserVariables(this, varList);
		variables.clearVars();
		for (String key : varList.keySet()) {
			variables.addVar(key, varList.get(key));
		}
		flagAsChanged();
		if (GroupManager.isLoaded()) {
			//if (!GroupManager.BukkitPermissions.isPlayer_join())
			//	GroupManager.BukkitPermissions.updatePlayer(this.getName());
			GroupManager.getGMEventHandler().callEvent(this, Action.USER_INFO_CHANGED);
		}
	}

	@Deprecated
	public User updatePlayer(Player player) {

		return this;
	}

	/**
	 * Returns a Player object (if online), or null.
	 * 
	 * @return Player object or null.
	 */
	public Player getBukkitPlayer() {
		
		return BukkitWrapper.getInstance().getPlayer(getLastName());
	}
	
	/**
	 * Is this player currently Online.
	 * 
	 * @return
	 */
	public boolean isOnline() {
		
		return getBukkitPlayer() != null;
	}
}
