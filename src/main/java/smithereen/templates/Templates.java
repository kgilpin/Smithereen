package smithereen.templates;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.loader.ClasspathLoader;
import com.mitchellbosecke.pebble.loader.DelegatingLoader;
import com.mitchellbosecke.pebble.template.EvaluationContext;
import com.mitchellbosecke.pebble.template.EvaluationContextImpl;
import com.mitchellbosecke.pebble.template.PebbleTemplate;
import com.mitchellbosecke.pebble.template.Scope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.stream.Collectors;

import smithereen.Config;
import smithereen.Utils;
import smithereen.data.Account;
import smithereen.data.AdminNotifications;
import smithereen.data.BirthdayReminder;
import smithereen.data.EventReminder;
import smithereen.data.SessionInfo;
import smithereen.data.UserNotifications;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.lang.Lang;
import smithereen.storage.NotificationsStorage;
import smithereen.storage.UserStorage;
import spark.Request;
import spark.Response;

public class Templates{
	private static final PebbleEngine desktopEngine=makeEngineInstance("desktop", "common");
	private static final PebbleEngine mobileEngine=makeEngineInstance("mobile", "common");
	private static final PebbleEngine popupEngine=makeEngineInstance("popup");
	private static final Map<String, String> staticHashes=new HashMap<>();

	private static final Logger LOG=LoggerFactory.getLogger(Templates.class);

	static{
		try(InputStreamReader reader=new InputStreamReader(Objects.requireNonNull(Templates.class.getClassLoader().getResourceAsStream("static_file_versions.json")))){
			JsonObject obj=JsonParser.parseReader(reader).getAsJsonObject();
			for(Map.Entry<String, JsonElement> e:obj.entrySet()){
				staticHashes.put(e.getKey(), e.getValue().getAsString());
			}
		}catch(IOException x){
			LOG.error("Error reading static_file_versions.json", x);
		}
	}

	public static String getStaticFileVersion(String name){
		return staticHashes.get(name);
	}

	private static ClasspathLoader makeClasspathLoader(String dir){
		ClasspathLoader loader=new ClasspathLoader();
		loader.setSuffix(".twig");
		loader.setPrefix("templates/"+dir+"/");
		return loader;
	}

	public static PebbleEngine makeEngineInstance(String... dirs){
		return new PebbleEngine.Builder()
				.loader(new DelegatingLoader(Arrays.stream(dirs).map(Templates::makeClasspathLoader).collect(Collectors.toList())))
				.defaultLocale(Locale.US)
				.defaultEscapingStrategy("html")
				.extension(new SmithereenExtension())
				.build();
	}

	public static void addGlobalParamsToTemplate(Request req, RenderedTemplateResponse model){
		JsonObject jsConfig=new JsonObject();
		ZoneId tz=Utils.timeZoneForRequest(req);
		if(req.session(false)!=null){
			SessionInfo info=req.session().attribute("info");
			if(info==null){
				info=new SessionInfo();
				req.session().attribute("info", info);
			}
			Account account=info.account;
			if(account!=null){
				model.with("currentUser", account.user);
				model.with("csrf", info.csrfToken);
				model.with("userPermissions", info.permissions);
				jsConfig.addProperty("csrf", info.csrfToken);
				jsConfig.addProperty("uid", info.account.user.id);
				try{
					UserNotifications notifications=NotificationsStorage.getNotificationsForUser(account.user.id, account.prefs.lastSeenNotificationID);
					model.with("userNotifications", notifications);

					LocalDate today=LocalDate.now(tz);
					BirthdayReminder reminder=UserStorage.getBirthdayReminderForUser(account.user.id, today);
					if(!reminder.userIDs.isEmpty()){
						model.with("birthdayUsers", UserStorage.getByIdAsList(reminder.userIDs));
						model.with("birthdaysAreToday", reminder.day.equals(today));
					}
					EventReminder eventReminder=Utils.context(req).getGroupsController().getUserEventReminder(account.user, tz);
					if(!eventReminder.groupIDs.isEmpty()){
						model.with("eventReminderEvents", Utils.context(req).getGroupsController().getGroupsByIdAsList(eventReminder.groupIDs));
						model.with("eventsAreToday", eventReminder.day.equals(today));
					}
				}catch(SQLException x){
					throw new InternalServerErrorException(x);
				}

				if(info.permissions.serverAccessLevel.ordinal()>=Account.AccessLevel.MODERATOR.ordinal()){
					model.with("serverSignupMode", Config.signupMode);
					model.with("adminNotifications", AdminNotifications.getInstance(req));
				}
			}
		}
		jsConfig.addProperty("timeZone", tz!=null ? tz.getId() : null);
		ArrayList<String> jsLang=new ArrayList<>();
		ArrayList<String> k=req.attribute("jsLang");
		Lang lang=Utils.lang(req);
		jsConfig.addProperty("locale", lang.getLocale().toLanguageTag());
		jsConfig.addProperty("langPluralRulesName", lang.getPluralRulesName());
		if(k!=null){
			for(String key:k){
				jsLang.add("\""+key+"\":"+lang.getAsJS(key));
			}
		}
		for(String key: List.of("error", "ok", "network_error", "close", "cancel")){
			jsLang.add("\""+key+"\":"+lang.getAsJS(key));
		}
		if(req.attribute("mobile")!=null){
			for(String key: List.of("search", "qsearch_hint")){
				jsLang.add("\""+key+"\":"+lang.getAsJS(key));
			}
		}
		model.with("locale", Utils.localeForRequest(req)).with("timeZone", tz!=null ? tz : ZoneId.systemDefault()).with("jsConfig", jsConfig.toString())
				.with("jsLangKeys", "{"+String.join(",", jsLang)+"}")
				.with("staticHashes", staticHashes)
				.with("serverName", Config.getServerDisplayName())
				.with("serverDomain", Config.domain)
				.with("isMobile", req.attribute("mobile")!=null);
	}

	public static PebbleTemplate getTemplate(Request req, String name){
		PebbleEngine engine=desktopEngine;
		if(req.attribute("popup")!=null)
			engine=popupEngine;
		else if(req.attribute("mobile")!=null)
			engine=mobileEngine;
		return engine.getTemplate(name);
	}

	/*package*/ static int asInt(Object o){
		if(o instanceof Integer)
			return (Integer)o;
		if(o instanceof Long)
			return (int)(long)(Long)o;
		throw new IllegalArgumentException("Can't cast "+o+" to int");
	}

	/*package*/ static <T> T getVariableRegardless(EvaluationContext context, String key){
		Object result=context.getVariable(key);
		if(result!=null)
			return (T)result;
		if(context instanceof EvaluationContextImpl contextImpl){
			List<Scope> scopes=contextImpl.getScopeChain().getGlobalScopes();
			for(Scope scope:scopes){
				result=scope.get(key);
				if(result!=null)
					return (T)result;
			}
		}
		return null;
	}

	public static void addJsLangForNewPostForm(Request req){
		Utils.jsLangKey(req,
				"post_form_cw", "post_form_cw_placeholder", "attach_menu_photo", "attach_menu_cw", "attach_menu_poll", "max_file_size_exceeded", "max_attachment_count_exceeded", "remove_attachment",
				// polls
				"create_poll_question", "create_poll_options", "create_poll_add_option", "create_poll_delete_option", "create_poll_multi_choice", "create_poll_anonymous", "create_poll_time_limit", "X_days", "X_hours",
				// graffiti
				"graffiti_clear", "graffiti_undo", "graffiti_clear_confirm", "graffiti_close_confirm", "confirm_title", "graffiti_color", "graffiti_thickness", "graffiti_opacity", "attach"
			);
	}
}
