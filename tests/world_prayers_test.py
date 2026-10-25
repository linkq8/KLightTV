"""Exercise the production offline engine, with tiny Android/storage-free model stubs."""
import pathlib,tempfile,subprocess
root=pathlib.Path(__file__).resolve().parents[1]
jar=next((pathlib.Path.home()/'.gradle/caches/modules-2/files-2.1/com.batoulapps.adhan/adhan/1.2.1').rglob('*.jar'))
jdk=pathlib.Path('/opt/homebrew/opt/openjdk@17/bin')
with tempfile.TemporaryDirectory() as tmp:
 p=pathlib.Path(tmp)
 (p/'Place.java').write_text('''package kw.urtv.shared;import java.time.*;public class Place{String method,zone,country="SA";double lat,lon;boolean hanafi,needsConfirmation;Place(String m,double a,double o,String z){method=m;lat=a;lon=o;zone=z;}ZoneId zoneId(){return ZoneId.of(zone);}boolean officialEligible(){return false;}String label(){return method;}}''')
 (p/'CalendarData.java').write_text('''package kw.urtv.shared;import java.time.*;public class CalendarData{static class Day{int[] times;}Day get(LocalDate d){return null;}RegionSchedule.Result resolve(LocalDate d,double a,double o,int[] c,int[] t){return null;}}''')
 (p/'RegionSchedule.java').write_text('''package kw.urtv.shared;class RegionSchedule{static class Result{int[] times;Result(int[] t,boolean v,boolean a,String s){times=t;}}}''')
 (p/'WorldTest.java').write_text('''package kw.urtv.shared;import java.time.*;import java.util.*;public class WorldTest{static void ok(boolean b,String m){if(!b)throw new AssertionError(m);}public static void main(String[] args){
 Place cairo=new Place("EGYPTIAN",30.0444,31.2357,"Africa/Cairo");int[] actual=WorldPrayers.calculate(LocalDate.of(2026,10,21),cairo),official={334,421,760,952,1098,1176};System.out.println("Cairo "+Arrays.toString(actual));for(int i=0;i<6;i++)ok(Math.abs(actual[i]-official[i])<=3,"Egypt Survey published fixture "+i);
 Place[] places={cairo,new Place("KUWAIT",29.3759,47.9774,"Asia/Kuwait"),new Place("UMM_AL_QURA",24.7136,46.6753,"Asia/Riyadh"),new Place("MUSLIM_WORLD_LEAGUE",51.5085,-.1257,"Europe/London"),new Place("NORTH_AMERICA",40.7143,-74.006,"America/New_York"),new Place("MUSLIM_WORLD_LEAGUE",-33.8688,151.2093,"Australia/Sydney")};for(Place city:places)for(LocalDate date:new LocalDate[]{LocalDate.of(2026,3,8),LocalDate.of(2026,6,21),LocalDate.of(2026,10,22),LocalDate.of(2026,12,21)}){int[] t=WorldPrayers.calculate(date,city);for(int i=0;i<6;i++)ok(t[i]>=0&&t[i]<1440&&(i==0||t[i]>t[i-1]),city.zone+date.toString());}
 Place north=new Place("MUSLIM_WORLD_LEAGUE",69.65,18.96,"Europe/Oslo");ok(WorldPrayers.resolve(new CalendarData(),LocalDate.of(2026,6,21),north,new int[6]).times==null,"polar night/day must not invent midnight prayers");
 int[] before=WorldPrayers.calculate(LocalDate.of(2026,10,22),cairo);cairo.hanafi=true;int[] after=WorldPrayers.calculate(LocalDate.of(2026,10,22),cairo);ok(after[3]>before[3],"Hanafi Asr");System.out.println("PASS six cities across seasons, Cairo official fixture ±3min, Hanafi Asr, polar unavailable");}}''')
 subprocess.run([str(jdk/'javac'),'-cp',str(jar),'-d',tmp,*map(str,p.glob('*.java')),*[str(root/f'android/shared/src/main/java/kw/urtv/shared/{n}.java') for n in ['WorldPrayers','PrayerCorrections','PrayerLimits']]],check=True)
 subprocess.run([str(jdk/'java'),'-cp',tmp+':'+str(jar),'kw.urtv.shared.WorldTest'],check=True)
