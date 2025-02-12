package smithereen.data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

import smithereen.Utils;
import smithereen.storage.DatabaseUtils;

public class SignupInvitation{
	public int id;
	public String code;
	public Instant createdAt;
	public String email;
	public int signupsRemaining;
	public int ownerID;

	public boolean noAddFriend, fromRequest;
	public String firstName, lastName;

	public static SignupInvitation fromResultSet(ResultSet res) throws SQLException{
		SignupInvitation inv=new SignupInvitation();
		inv.id=res.getInt("id");
		inv.code=Utils.byteArrayToHexString(res.getBytes("code"));
		inv.createdAt=DatabaseUtils.getInstant(res, "created");
		inv.email=res.getString("email");
		inv.signupsRemaining=res.getInt("signups_remaining");
		inv.ownerID=res.getInt("owner_id");
		String _extra=res.getString("extra");
		if(_extra!=null){
			ExtraInfo extra=Utils.gson.fromJson(_extra, ExtraInfo.class);
			inv.noAddFriend=extra.noAddFriend;
			inv.firstName=extra.firstName;
			inv.lastName=extra.lastName;
			inv.fromRequest=extra.fromRequest;
		}
		return inv;
	}

	@Override
	public String toString(){
		return "SignupInvitation{"+
				"id="+id+
				", code='"+code+'\''+
				", createdAt="+createdAt+
				", email='"+email+'\''+
				", signupsRemaining="+signupsRemaining+
				", ownerID="+ownerID+
				", noAddFriend="+noAddFriend+
				", fromRequest="+fromRequest+
				", firstName='"+firstName+'\''+
				", lastName='"+lastName+'\''+
				'}';
	}

	public static String getExtra(boolean noAddFriend, String firstName, String lastName, boolean fromRequest){
		return Utils.gson.toJson(new ExtraInfo(noAddFriend, firstName, lastName, fromRequest));
	}

	private static class ExtraInfo{
		public boolean noAddFriend;
		public String firstName;
		public String lastName;
		public boolean fromRequest;

		public ExtraInfo(){

		}

		public ExtraInfo(boolean noAddFriend, String firstName, String lastName, boolean fromRequest){
			this.noAddFriend=noAddFriend;
			this.firstName=firstName;
			this.lastName=lastName;
			this.fromRequest=fromRequest;
		}
	}
}
