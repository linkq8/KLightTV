#!/usr/bin/env python3
"""Check both native prayer engines and the screensaver's next-prayer countdown."""
import json, os, pathlib, subprocess, tempfile
root = pathlib.Path(__file__).resolve().parents[1]
java = pathlib.Path(os.environ['JAVA_HOME']) / 'bin' if os.environ.get('JAVA_HOME') else None
fixture = json.loads((root / 'tests/fixtures/kuwait-2026-10-20.json').read_text())['data']['timings']
for module, package in [('launcher', 'home'), ('app', 'screensaver')]:
    source = root / f'android/{module}/src/main/java/kw/urtv/{package}'
    with tempfile.TemporaryDirectory() as folder:
        harness = pathlib.Path(folder) / 'NativePrayerCheck.java'
        extra = '''
   int[] today={270,360,690,890,1035,1110},tomorrow={277,367,691,891,1036,1111};
   int[] before=PrayerClock.next(today,tomorrow,260);
   if(before[0]!=0||before[2]!=10)throw new AssertionError("Before Fajr");
   int[] sunrise=PrayerClock.next(today,tomorrow,300);
   if(sunrise[0]!=2||sunrise[2]!=390)throw new AssertionError("Sunrise is not a prayer");
   int[] at=PrayerClock.next(today,tomorrow,690);
   if(at[0]!=3)throw new AssertionError("At prayer minute");
   int[] night=PrayerClock.next(today,tomorrow,1200);
   if(night[0]!=0||night[1]!=277||night[2]!=517)throw new AssertionError("Tomorrow's Fajr");
   if(!PrayerClock.time(0).equals("١٢:٠٠ ص")||!PrayerClock.time(720).equals("١٢:٠٠ م"))throw new AssertionError("Noon/midnight");
   if(!PrayerClock.remaining(62).equals("بعد ساعة و دقيقتين"))throw new AssertionError("Arabic countdown");
   if(!PrayerClock.remaining(120).equals("بعد ساعتين"))throw new AssertionError("Whole hours");
''' if package == 'screensaver' else ''
        harness.write_text('package kw.urtv.' + package + ''';
import java.time.LocalDate;
public class NativePrayerCheck {
 public static void main(String[] args) {
  for(String day:new String[]{"2026-10-20","2026-12-31","2027-01-01","2028-02-29"}) {
   int[] times=PrayerTimes.calculate(LocalDate.parse(day),29.3759,47.9774);
   for(int i=0;i<times.length;i++) {
    if(times[i]<0||times[i]>=1440)throw new AssertionError(day);
    if(i>0&&times[i]<=times[i-1])throw new AssertionError("Order "+day);
    if(day.equals("2026-10-20"))System.out.println(PrayerTimes.KEYS[i]+"="+times[i]);
   }
  }
  for(int n=0;n<366;n++) {
   int[] times=PrayerTimes.calculate(LocalDate.of(2028,1,1).plusDays(n),29.3759,47.9774);
   for(int i=1;i<times.length;i++)if(times[i]<=times[i-1])throw new AssertionError("Seasonal order");
  }
''' + extra + '\n }\n}\n')
        sources = [source / 'PrayerTimes.java', harness]
        if package == 'screensaver': sources.append(source / 'PrayerClock.java')
        subprocess.run([str(java / 'javac') if java else 'javac', '-d', folder, *map(str,sources)],check=True)
        result = subprocess.run([str(java / 'java') if java else 'java', '-cp', folder, f'kw.urtv.{package}.NativePrayerCheck'],check=True,capture_output=True,text=True)
        for line in result.stdout.splitlines():
            key, value = line.split('=')
            h, m = map(int, fixture[key].split(':'))
            assert abs(int(value) - h * 60 - m) <= 3, (key, value, fixture[key])
    print(f'PASS {package}: Kuwait fallback, reference, ordering, leap year and date boundaries')
print('PASS native next-prayer countdown: sunrise excluded, next-day Fajr, noon/midnight and Arabic units')
