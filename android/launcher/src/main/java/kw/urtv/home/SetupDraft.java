package kw.urtv.home;
import kw.urtv.shared.*;import org.json.*;
/** Unsaved setup: one required city, shared by prayer and weather unless explicitly separated. */
public final class SetupDraft {
 private final String originalDisplay,originalPlace;
 public String language,country,surface,theme,focusColor;public boolean prayers,weather,advanced,separateWeather;public int step;
 public Place prayerPlace,weatherPlace;
 public SetupDraft(String display,Place saved){
  try{JSONObject o=new JSONObject(display);originalDisplay=o.toString();originalPlace=saved.json();language="en".equals(o.optString("setupLanguage"))?"en":"ar";theme=o.optString("theme","dark");focusColor=o.optString("focusColor","blue");surface="focus".equals(o.optString("surface"))?"focus":"scenic";country=o.optString("setupCountry",saved.needsConfirmation?"":saved.country);if(!ContentPolicy.country(country))country="";prayers=o.optBoolean("prayersEnabled",true);weather=o.optBoolean("weatherEnabled",true);prayerPlace=saved.needsConfirmation?null:saved;weatherPlace=o.has("weatherPlace")&&!o.isNull("weatherPlace")?Place.parse(o.getJSONObject("weatherPlace").toString()):prayerPlace;separateWeather=weatherPlace!=null&&prayerPlace!=null&&!sameCity(weatherPlace,prayerPlace);}catch(Exception e){throw new IllegalArgumentException(e);}
 }
 private static boolean sameCity(Place a,Place b){return a.country.equals(b.country)&&a.lat==b.lat&&a.lon==b.lon&&a.zone.equals(b.zone);}
 public boolean focused(){return "focus".equals(surface);}
 public void country(String value){if(!ContentPolicy.country(value)||!java.util.Arrays.asList(java.util.Locale.getISOCountries()).contains(value))throw new IllegalArgumentException("Invalid country");country=value;}
 public void city(Place p){if(p==null||p.needsConfirmation||!p.country.equals(country))throw new IllegalArgumentException("city");prayerPlace=p;if(!separateWeather)weatherPlace=p;}
 public String payload(){try{
  if(!java.util.Arrays.asList(java.util.Locale.getISOCountries()).contains(country)||!ContentPolicy.country(country))throw new IllegalArgumentException("country");
  if(prayerPlace==null||prayerPlace.needsConfirmation||!country.equals(prayerPlace.country))throw new IllegalArgumentException("city");
  Place forecast=separateWeather?weatherPlace:prayerPlace;
  if(weather&&!focused()&&(forecast==null||forecast.needsConfirmation))throw new IllegalArgumentException("weather-location");
  JSONObject display=new JSONObject(originalDisplay).put("setupLanguage",language).put("setupCountry",country).put("surface",surface).put("theme",theme).put("focusColor",focusColor).put("latin",focused()).put("prayersEnabled",prayers).put("weatherEnabled",weather);
  if(forecast!=null)display.put("weatherPlace",new JSONObject(forecast.json()));
  return new JSONObject().put("display",display).put("place",new JSONObject(prayerPlace.json())).toString();
 }catch(IllegalArgumentException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
 public String json(){try{return new JSONObject().put("originalDisplay",originalDisplay).put("originalPlace",originalPlace).put("language",language).put("country",country).put("surface",surface).put("theme",theme).put("focusColor",focusColor).put("prayers",prayers).put("weather",weather).put("separateWeather",separateWeather).put("advanced",advanced).put("step",step).put("prayerPlace",prayerPlace==null?JSONObject.NULL:new JSONObject(prayerPlace.json())).put("weatherPlace",weatherPlace==null?JSONObject.NULL:new JSONObject(weatherPlace.json())).toString();}catch(Exception e){throw new IllegalStateException(e);}}
 public static SetupDraft restore(String raw){try{JSONObject j=new JSONObject(raw);SetupDraft d=new SetupDraft(j.getString("originalDisplay"),Place.parse(j.getString("originalPlace")));d.language=j.getString("language");d.country=j.getString("country");d.surface=j.getString("surface");d.theme=j.getString("theme");d.focusColor=j.getString("focusColor");d.prayers=j.getBoolean("prayers");d.weather=j.getBoolean("weather");d.separateWeather=j.getBoolean("separateWeather");d.advanced=j.getBoolean("advanced");d.step=j.getInt("step");d.prayerPlace=j.isNull("prayerPlace")?null:Place.parse(j.getJSONObject("prayerPlace").toString());d.weatherPlace=j.isNull("weatherPlace")?null:Place.parse(j.getJSONObject("weatherPlace").toString());return d;}catch(Exception e){throw new IllegalArgumentException(e);}}
}
