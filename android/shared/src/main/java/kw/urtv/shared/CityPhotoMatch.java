package kw.urtv.shared;
/** Same country AND a curated city anchor; names and neighboring cities never imply a match. */
public final class CityPhotoMatch {
 public static boolean matches(String country,double lat,double lon,String target,double targetLat,double targetLon,double radius){
  if(country==null||!country.equals(target)||!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180||radius<=0)return false;
  double a=Math.toRadians(lat),b=Math.toRadians(targetLat),dl=b-a,dx=Math.toRadians(targetLon-lon);
  double h=Math.sin(dl/2)*Math.sin(dl/2)+Math.cos(a)*Math.cos(b)*Math.sin(dx/2)*Math.sin(dx/2);
  return 6371*2*Math.asin(Math.sqrt(Math.min(1,h)))<=radius;
 }
 public static boolean nameMatches(String name,String... aliases){
  if(name==null)return false;String normalized=normalize(name);for(String alias:aliases)if(normalized.equals(normalize(alias)))return true;return false;
 }
 private static String normalize(String s){return java.text.Normalizer.normalize(s,java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(java.util.Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]","");}
 private CityPhotoMatch(){}
}
