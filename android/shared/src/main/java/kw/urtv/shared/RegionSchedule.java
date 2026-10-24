package kw.urtv.shared;

/** Pure shared policy: apply exactly one published offset, then explicit per-prayer corrections. */
public final class RegionSchedule {
    public static final class Result {
        public final int[] times;public final boolean verified,adjusted;public final String source;
        Result(int[] t,boolean v,boolean a,String s){times=t;verified=v;adjusted=a;source=s;}
    }
    public static Result resolve(double lat,double lon,int[] city,int[] calculated,int[] corrections){
        RegionCatalog.Region r=RegionCatalog.find(lat,lon);
        boolean verified=r!=null&&r.offset!=null&&city!=null;
        int[] result=(verified?city:calculated).clone();
        if(result.length!=6||corrections.length!=6)throw new IllegalArgumentException("Six times required");
        boolean changed=false;
        for(int i=0;i<6;i++){
            if(corrections[i]<-PrayerLimits.CORRECTION_MINUTES||corrections[i]>PrayerLimits.CORRECTION_MINUTES)throw new IllegalArgumentException("Correction out of range");
            result[i]+=(verified?r.offset:0)+corrections[i];changed|=corrections[i]!=0;
            if(result[i]<0||result[i]>=1440||(i>0&&result[i]<=result[i-1]))throw new IllegalArgumentException("Invalid schedule");
        }
        String source=verified?(r.offset==0?"العجيري":"العجيري "+(r.offset>0?"+":"")+r.offset+"د"):"محسوبة";
        return new Result(result,verified,changed,source+(changed?" · معدّلة":""));
    }
    private RegionSchedule(){}
}
