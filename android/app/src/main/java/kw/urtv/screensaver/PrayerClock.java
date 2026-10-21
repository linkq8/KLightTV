package kw.urtv.screensaver;

/** Pure countdown logic: sunrise belongs in the table, never in the next-prayer countdown. */
final class PrayerClock {
    static int[] next(int[] today,int[] tomorrow,int minute) {
        for(int i:new int[]{0,2,3,4,5})
            if(today[i]>minute)return new int[]{i,today[i],today[i]-minute};
        return new int[]{0,tomorrow[0],1440+tomorrow[0]-minute};
    }
    static String ar(String value) {
        StringBuilder out=new StringBuilder();
        for(char c:value.toCharArray())out.append(c>='0'&&c<='9'?(char)('٠'+c-'0'):c);
        return out.toString();
    }
    static String time(int minute) {
        int h=minute/60;
        return ar((h%12==0?12:h%12)+":"+String.format(java.util.Locale.ROOT,"%02d",minute%60))
                +" "+(h<12?"ص":"م");
    }
    static String remaining(int minutes) {
        int h=minutes/60,m=minutes%60;
        String hours=h==0?"":h==1?"ساعة":h==2?"ساعتين":ar(""+h)+(h<=10?" ساعات":" ساعة");
        String mins=m==1?"دقيقة":m==2?"دقيقتين":ar(""+m)+(m>=3&&m<=10?" دقائق":" دقيقة");
        return "بعد "+hours+(m>0||h==0?(h>0?" و ":"")+mins:"");
    }
}
