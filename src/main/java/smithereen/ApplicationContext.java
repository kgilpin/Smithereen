package smithereen;

import smithereen.activitypub.ActivityPubWorker;
import smithereen.controllers.FriendsController;
import smithereen.controllers.GroupsController;
import smithereen.controllers.ModerationController;
import smithereen.controllers.NewsfeedController;
import smithereen.controllers.NotificationsController;
import smithereen.controllers.ObjectLinkResolver;
import smithereen.controllers.PrivacyController;
import smithereen.controllers.StatsController;
import smithereen.controllers.UserInteractionsController;
import smithereen.controllers.UsersController;
import smithereen.controllers.WallController;

public class ApplicationContext{
	private final WallController wallController;
	private final GroupsController groupsController;
	private final UsersController usersController;
	private final UserInteractionsController userInteractionsController;
	private final FriendsController friendsController;
	private final ObjectLinkResolver objectLinkResolver;
	private final ActivityPubWorker activityPubWorker;
	private final PrivacyController privacyController;
	private final NewsfeedController newsfeedController;
	private final NotificationsController notificationsController;
	private final ModerationController moderationController;
	private final StatsController statsController;

	public ApplicationContext(){
		wallController=new WallController(this);
		groupsController=new GroupsController(this);
		usersController=new UsersController(this);
		userInteractionsController=new UserInteractionsController(this);
		friendsController=new FriendsController(this);
		objectLinkResolver=new ObjectLinkResolver(this);
		activityPubWorker=new ActivityPubWorker(this);
		privacyController=new PrivacyController(this);
		newsfeedController=new NewsfeedController(this);
		notificationsController=new NotificationsController(this);
		moderationController=new ModerationController(this);
		statsController=new StatsController(this);
	}

	public WallController getWallController(){
		return wallController;
	}

	public GroupsController getGroupsController(){
		return groupsController;
	}

	public UsersController getUsersController(){
		return usersController;
	}

	public UserInteractionsController getUserInteractionsController(){
		return userInteractionsController;
	}

	public FriendsController getFriendsController(){
		return friendsController;
	}

	public ObjectLinkResolver getObjectLinkResolver(){
		return objectLinkResolver;
	}

	public ActivityPubWorker getActivityPubWorker(){
		return activityPubWorker;
	}

	public PrivacyController getPrivacyController(){
		return privacyController;
	}

	public NewsfeedController getNewsfeedController(){
		return newsfeedController;
	}

	public NotificationsController getNotificationsController(){
		return notificationsController;
	}

	public ModerationController getModerationController(){
		return moderationController;
	}

	public StatsController getStatsController(){
		return statsController;
	}
}
