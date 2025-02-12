package smithereen.data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

import smithereen.storage.DatabaseUtils;

public class ViolationReport{

	public int id;
	public int reporterID;
	public TargetType targetType;
	public ContentType contentType;
	public int targetID;
	public int contentID;
	public String comment;
	public int moderatorID;
	public Instant time;
	public Instant actionTime;
	public String serverDomain;

	public static ViolationReport fromResultSet(ResultSet res) throws SQLException{
		ViolationReport r=new ViolationReport();
		r.id=res.getInt("id");
		r.reporterID=res.getInt("reporter_id");
		r.targetType=TargetType.values()[res.getInt("target_type")];
		r.targetID=res.getInt("target_id");
		int contentType=res.getInt("content_type");
		if(!res.wasNull()){
			r.contentType=ContentType.values()[contentType];
			r.contentID=res.getInt("content_id");
		}
		r.comment=res.getString("comment");
		r.moderatorID=res.getInt("moderator_id");
		r.time=DatabaseUtils.getInstant(res, "time");
		r.actionTime=DatabaseUtils.getInstant(res, "action_time");
		r.serverDomain=res.getString("server_domain");
		return r;
	}

	public enum ContentType{
		POST
	}

	public enum TargetType{
		USER,
		GROUP
	}
}
