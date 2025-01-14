package smithereen.activitypub.handlers;

import java.net.URI;
import java.sql.SQLException;
import java.util.List;

import smithereen.Config;
import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.activities.Flag;
import smithereen.data.ForeignUser;
import smithereen.data.User;
import smithereen.exceptions.BadRequestException;

public class FlagHandler extends ActivityTypeHandler<Actor, Flag, ActivityPubObject>{
	@Override
	public void handle(ActivityHandlerContext context, Actor actor, Flag activity, ActivityPubObject object) throws SQLException{
		User reporter;
		if(actor instanceof ForeignUser fu && !fu.isServiceActor){
			if(fu.id==0){
				context.appContext.getObjectLinkResolver().storeOrUpdateRemoteObject(fu);
			}
			reporter=fu;
		}else{
			reporter=null;
		}

		for(URI uri:activity.object){
			if(!Config.isLocal(uri))
				throw new BadRequestException("Only local URIs are supported in Flag.object. This URI is not local: "+uri);
		}
		if(activity.object.size()<1)
			throw new BadRequestException("Flag.object must contain at least one URI");

		Actor reportedActor=null;
		ActivityPubObject reportedContent=null;
		List<ActivityPubObject> objects=activity.object.stream().map(uri->context.appContext.getObjectLinkResolver().resolve(uri, ActivityPubObject.class, false, false, false)).toList();
		for(ActivityPubObject obj:objects){
			if(obj instanceof Actor a){
				if(reportedActor==null)
					reportedActor=a;
			}else if(reportedContent==null){
				reportedContent=obj;
			}
		}
		if(reportedActor==null)
			throw new BadRequestException("None of the URIs in Flag.object point to an Actor");

		context.appContext.getModerationController().createViolationReport(reporter, reportedActor, reportedContent, activity.content, actor.domain);
	}
}
