package smithereen.data;

import java.util.HashMap;

import smithereen.activitypub.objects.ActivityPubObject;

/**
 * A "helper" kind of object passed around everywhere to help determine what a user
 * can and cannot do. Comes from a logged in <code>SessionInfo</code>
 */
public class UserPermissions{
	public int userID;
	public HashMap<Integer, Group.AdminLevel> managedGroups=new HashMap<>();
	public Account.AccessLevel serverAccessLevel;
	public boolean canInviteNewUsers;

	public UserPermissions(Account account){
		userID=account.user.id;
		serverAccessLevel=account.accessLevel;
	}

	public boolean canDeletePost(Post post){
		// Moderators can delete any local posts
		if(post.local && serverAccessLevel.ordinal()>=Account.AccessLevel.MODERATOR.ordinal())
			return true;
		// Users can always delete their own posts
		if(post.user.id==userID)
			return true;

		// Group moderators can delete any post in their group
		if(post.isGroupOwner())
			return managedGroups.containsKey(((Group) post.owner).id);

		// Users can delete any post on their own wall
		return post.owner instanceof User && ((User)post.owner).id==userID;
	}

	public boolean canEditPost(Post post){
		return post.user.id==userID && System.currentTimeMillis()-post.published.toEpochMilli()<24*3600_000L;
	}

	public boolean canEditGroup(Group group){
		return managedGroups.getOrDefault(group.id, Group.AdminLevel.REGULAR).isAtLeast(Group.AdminLevel.ADMIN);
	}

	public boolean canManageGroup(Group group){
		return managedGroups.getOrDefault(group.id, Group.AdminLevel.REGULAR).isAtLeast(Group.AdminLevel.MODERATOR);
	}

	public boolean canReport(ActivityPubObject obj){
		if(obj instanceof User u){
			return u.id!=userID;
		}else if(obj instanceof Group g){
			return managedGroups.getOrDefault(g.id, Group.AdminLevel.REGULAR)!=Group.AdminLevel.OWNER;
		}else if(obj instanceof Post p){
			return p.user.id!=userID;
		}else{
			return false;
		}
	}
}
