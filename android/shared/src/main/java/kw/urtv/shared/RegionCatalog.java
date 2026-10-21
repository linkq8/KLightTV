package kw.urtv.shared;

/** Published calendar offsets are distinct from coordinate-based calculations. */
public final class RegionCatalog {
    public static final String OFFSET_SOURCE="https://awgatalsalat.com/calendar/10.jpg";
    public static final class Region {
        public final String name;public final double lat,lon;public final Integer offset;
        Region(String n,double a,double o,Integer d){name=n;lat=a;lon=o;offset=d;}
        public String description(){return offset==null?"حساب بالإحداثيات · لا فرق منشور موثق":"فرق الرزنامة عن العاصمة: "+(offset>0?"+":"")+offset+" دقيقة";}
    }
    public static final Region[] ALL={
        new Region("مدينة الكويت",29.3759,47.9774,0),
        new Region("الأحمدي",29.07694,48.08389,0),
        new Region("الوفرة",28.63917,47.93056,0),
        new Region("الخيران",28.663,48.379,null),
        new Region("العبدلي",30.0422,47.7221,2),
        new Region("الجهراء",29.3375,47.6581,2),
        new Region("السالمي",29.10167,46.68222,6),
        new Region("النويصيب",28.5419,48.4181,-2),
        new Region("فيلكا",29.4428,48.2797,-2),
        new Region("بوبيان",29.75,48.25,-1),
        new Region("كبر",29.073,48.492,-2),
        new Region("قاروه",28.817,48.777,-3),
        new Region("أم المرادم",28.679,48.653,-3)
    };
    public static Region find(double lat,double lon){for(Region r:ALL)if(Math.abs(lat-r.lat)<.001&&Math.abs(lon-r.lon)<.001)return r;return null;}
    public static boolean usesCityRecord(double lat,double lon){Region r=find(lat,lon);return r!=null&&r.offset!=null;}
    private RegionCatalog(){}
}
