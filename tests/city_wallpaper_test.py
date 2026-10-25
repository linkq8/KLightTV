"""Run the real geographic matcher; validate curated metadata and image-package size policy."""
import json,pathlib,subprocess,tempfile,os,urllib.parse
root=pathlib.Path(__file__).resolve().parents[1]
catalog=json.loads((root/'android/shared/src/main/assets/city-wallpapers.json').read_text())
assert {x['id'] for x in catalog}=={'kuwait','riyadh','dubai','doha'}
for x in catalog:
 assert x['author'] and x['source'].startswith('https://') and x['license']
 assert x['width']>=3840 and x['height']>=2160
 assert urllib.parse.urlparse(x['url']).hostname in {'upload.wikimedia.org','raw.githubusercontent.com'}
 assert 0<x['radiusKm']<=35
for module in ['app','launcher']:
 assert "from('../../assets/backgrounds')" not in (root/f'android/{module}/build.gradle').read_text()
java='''import kw.urtv.shared.CityPhotoMatch;public class CityMatchTest {
static void test(boolean ok){if(!ok)throw new AssertionError();}
public static void main(String[] args){
test(CityPhotoMatch.matches("SA",24.7136,46.6753,"SA",24.7136,46.6753,35));
test(CityPhotoMatch.matches("SA",24.70,46.68,"SA",24.7136,46.6753,35));
test(!CityPhotoMatch.matches("KW",24.7136,46.6753,"SA",24.7136,46.6753,35));
test(!CityPhotoMatch.matches("SA",26.3592,43.9818,"SA",24.7136,46.6753,35));
test(!CityPhotoMatch.matches("KW",28.65,48.36,"KW",29.3759,47.9774,22));
test(!CityPhotoMatch.matches("SA",Double.NaN,46.6753,"SA",24.7136,46.6753,35));
test(!CityPhotoMatch.matches(null,24.7136,46.6753,"SA",24.7136,46.6753,35));
test(CityPhotoMatch.nameMatches("Ar Riyāḑ","Ar Riyad"));
test(CityPhotoMatch.nameMatches("مدينة الكويت","مدينة الكويت","Kuwait City"));
test(!CityPhotoMatch.nameMatches("Sharjah","Dubai","Dubayy"));
test(!CityPhotoMatch.nameMatches("Lusail","Doha","Ad Dawhah"));
System.out.println("PASS 11 real city-match cases; curated metadata and no bundled photo packs");}}
'''
jdk=pathlib.Path(os.environ['JAVA_HOME'])/'bin' if os.environ.get('JAVA_HOME') else None
with tempfile.TemporaryDirectory() as d:
 p=pathlib.Path(d);(p/'CityMatchTest.java').write_text(java)
 subprocess.run([str(jdk/'javac') if jdk else 'javac','-d',d,str(p/'CityMatchTest.java'),str(root/'android/shared/src/main/java/kw/urtv/shared/CityPhotoMatch.java')],check=True)
 subprocess.run([str(jdk/'java') if jdk else 'java','-cp',d,'CityMatchTest'],check=True)
