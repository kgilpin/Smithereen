package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.activities.Block;
import smithereen.data.ForeignUser;
import smithereen.data.FriendshipStatus;
import smithereen.data.User;
import smithereen.data.feed.NewsfeedEntry;
import smithereen.storage.NewsfeedStorage;
import smithereen.storage.UserStorage;

public class PersonBlockPersonHandler extends ActivityTypeHandler<ForeignUser, Block, User>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Block activity, User object) throws SQLException{
		object.ensureLocal();
		FriendshipStatus status=UserStorage.getFriendshipStatus(object.id, actor.id);
		UserStorage.blockUser(actor.id, object.id);
		if(status==FriendshipStatus.FRIENDS){
			context.appContext.getActivityPubWorker().sendRemoveFromFriendsCollectionActivity(object, actor);
			context.appContext.getNewsfeedController().deleteFriendsFeedEntry(object, actor.id, NewsfeedEntry.Type.ADD_FRIEND);
		}
	}
}
