package kw.urtv.shared;
/** Shared UI, storage, IPC and import limits. Profiles never cross calculation contexts. */
public final class PrayerCorrections {
 public static final int LIMIT=PrayerLimits.CORRECTION_MINUTES, COUNT=6;
 private PrayerCorrections(){}
 public static int clamp(int n){return Math.max(-LIMIT,Math.min(LIMIT,n));}
 public static int[] parse(String raw){if(raw==null)return new int[COUNT];String[] v=raw.split(",",-1);if(v.length!=COUNT)throw new IllegalArgumentException("Six corrections required");int[] out=new int[COUNT];for(int i=0;i<COUNT;i++){out[i]=Integer.parseInt(v[i]);if(out[i]!=clamp(out[i]))throw new IllegalArgumentException("Correction out of range");}return out;}
 public static String encode(int[] a){if(a==null||a.length!=COUNT)throw new IllegalArgumentException("Six corrections required");StringBuilder s=new StringBuilder();for(int n:a){if(n!=clamp(n))throw new IllegalArgumentException("Correction out of range");if(s.length()>0)s.append(',');s.append(n);}return s.toString();}
 public static String key(Place p){return "adjust:v2:"+p.country+":"+Double.toString(p.lat)+":"+Double.toString(p.lon)+":"+p.zone+":"+p.method+":"+p.hanafi;}
}
