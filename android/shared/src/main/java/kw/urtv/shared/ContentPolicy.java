package kw.urtv.shared;
import java.text.Normalizer;
import java.util.*;
/** Applied at ingestion and again before display, including previously saved selections. */
public final class ContentPolicy {
 private ContentPolicy(){}
 private static final Set<String> TEAMS=new HashSet<>(Arrays.asList("461","5218","8329","13083","19255","9577","20970","8327","22311","611","5945","2983","21517","524","5269"));
 public static boolean country(String code){return code!=null&&code.matches("[A-Z]{2}")&&!code.equals("IL");}
 public static boolean text(String value){if(value==null)return true;String n=Normalizer.normalize(value,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).replace("ـ","");return !(n.contains("israel")||n.contains("إسرائيل")||n.contains("اسراييل")||n.contains("اسرائيل")||n.contains("ישראל")||n.contains("isr.")||n.contains("isr-")||n.equals("isr"));}
 public static boolean team(String id,String metadata){return id!=null&&id.matches("[0-9]+")&&!TEAMS.contains(id)&&text(metadata);}
}
