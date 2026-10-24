#!/usr/bin/env python3
"""Check published-offset policy, Khiran seasonal calculation and low-memory decisions."""
from pathlib import Path
import subprocess,json,tempfile,os
r=Path(__file__).resolve().parents[1];java=Path(os.environ.get('JAVA_HOME','/opt/homebrew/opt/openjdk@17'))/'bin'
fixture=json.loads((r/'tests/fixtures/regions-method9.json').read_text())
lines=[]
for c in fixture['cases']:
 d,m,y=c['date'].split('-');expected=[int(t[:2])*60+int(t[3:5]) for t in c['times'].values()]
 lines.append('check(LocalDate.of('+','.join(str(int(x)) for x in (y,m,d))+'),'+str(c['lat'])+','+str(c['lon'])+',new int[]{'+','.join(map(str,expected))+'});')
with tempfile.TemporaryDirectory() as folder:
 harness=Path(folder)/'RegionCheck.java';harness.write_text('''package kw.urtv.home;
import kw.urtv.shared.*;import java.time.LocalDate;import java.util.Arrays;
public class RegionCheck {
 static void yes(boolean b,String message){if(!b)throw new AssertionError(message);}
 static void check(LocalDate d,double lat,double lon,int[] expected){int[] a=PrayerTimes.calculate(d,lat,lon);for(int i=0;i<6;i++)yes(Math.abs(a[i]-expected[i])<=1,"Seasonal API reference "+d+" prayer "+i+" got "+a[i]+" expected "+expected[i]);}
 public static void main(String[] args){
 int[] base={270,360,690,890,1035,1110},calc={265,355,685,885,1030,1105},none=new int[6];
 for(RegionCatalog.Region r:RegionCatalog.ALL){RegionSchedule.Result out=RegionSchedule.resolve(r.lat,r.lon,base,calc,none);yes(out.verified==(r.offset!=null),"source provenance "+r.name);for(int i=0;i<6;i++)yes(out.times[i]==(r.offset==null?calc[i]:base[i]+r.offset),"offset once "+r.name);yes(!out.adjusted,"default adjustment");}
 RegionSchedule.Result khiran=RegionSchedule.resolve(28.663,48.379,base,calc,none);yes(!khiran.verified&&khiran.source.equals("محسوبة"),"Khiran must not inherit official city attribution");
 int[] correction={2,0,-1,0,0,0};RegionSchedule.Result a=RegionSchedule.resolve(30.0422,47.7221,base,calc,correction);yes(a.times[0]==274&&a.times[2]==691&&a.adjusted,"regional + manual once");yes(base[0]==270&&calc[0]==265,"inputs immutable");
 RegionSchedule.Result missing=RegionSchedule.resolve(30.0422,47.7221,null,calc,none);yes(!missing.verified&&missing.times[0]==265,"missing city record must use actual local calculation, no double offset");
 yes(!RegionSchedule.resolve(28.7,48.4,base,calc,none).verified,"unknown location calculated");
 try{RegionSchedule.resolve(28.663,48.379,base,calc,new int[]{31,0,0,0,0,0});throw new AssertionError("range");}catch(IllegalArgumentException good){}
 yes(HardwarePolicy.useLite("auto",false,2L*1024*1024*1024,256,false),"2GB auto lite");
 yes(!HardwarePolicy.useLite("auto",false,4L*1024*1024*1024,256,false),"4GB auto normal");
 yes(HardwarePolicy.useLite("auto",true,4L*1024*1024*1024,256,false),"system low-RAM flag");
 yes(HardwarePolicy.useLite("normal",false,4L*1024*1024*1024,64,false),"unsafe heap override guarded");
 yes(HardwarePolicy.useLite("normal",false,4L*1024*1024*1024,256,true),"memory pressure guard");
 yes(!HardwarePolicy.shouldTrim(20)&&HardwarePolicy.shouldTrim(10)&&HardwarePolicy.shouldTrim(15)&&HardwarePolicy.shouldTrim(40),"hiding UI alone is not memory pressure");
 yes(HardwarePolicy.imageEdge(true,64)==1280&&HardwarePolicy.imageEdge(true,256)==1920&&HardwarePolicy.imageEdge(false,256)==3840,"allocation limits");
'''+'\n'.join(lines)+'''
 for(int n=0;n<366;n++){LocalDate d=LocalDate.of(2028,1,1).plusDays(n);for(RegionCatalog.Region region:RegionCatalog.ALL){int[] times=PrayerTimes.calculate(d,region.lat,region.lon);for(int i=1;i<6;i++)yes(times[i]>times[i-1],"seasonal ordering");}}
 System.out.println("PASS regions:13 presets, published offsets, per-prayer corrections, calculated Khiran,8 seasonal references, leap year and hardware policy");}}
''')
 sources=[r/'android/launcher/src/main/java/kw/urtv/home/PrayerTimes.java',harness]+[r/f'android/shared/src/main/java/kw/urtv/shared/{n}.java' for n in ['RegionCatalog','RegionSchedule','HardwarePolicy','PrayerLimits']]
 subprocess.run([str(java/'javac'),'-d',folder,*map(str,sources)],check=True)
 subprocess.run([str(java/'java'),'-cp',folder,'kw.urtv.home.RegionCheck'],check=True)
