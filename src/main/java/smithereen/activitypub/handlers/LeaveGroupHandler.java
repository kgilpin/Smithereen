package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.activities.Leave;
import smithereen.data.ForeignUser;
import smithereen.data.Group;
import smithereen.storage.GroupStorage;

public class LeaveGroupHandler extends ActivityTypeHandler<ForeignUser, Leave, Group>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Leave activity, Group group) throws SQLException{
		group.ensureLocal();
		Group.MembershipState state=GroupStorage.getUserMembershipState(group.id, actor.id);
		if(state!=Group.MembershipState.MEMBER && state!=Group.MembershipState.TENTATIVE_MEMBER && state!=Group.MembershipState.REQUESTED){
			return;
		}
		GroupStorage.leaveGroup(group, actor.id, state==Group.MembershipState.TENTATIVE_MEMBER, state!=Group.MembershipState.REQUESTED);
		context.appContext.getActivityPubWorker().sendRemoveUserFromGroupActivity(actor, group, state==Group.MembershipState.TENTATIVE_MEMBER);
	}
}
