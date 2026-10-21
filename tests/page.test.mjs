// Behavior tests for Kuwait Lite: network isolation, offline prayers, rendering, saved weather and widget mode.
import { chromium } from 'playwright';
import { readFileSync, mkdirSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
const ROOT=path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const browser=await chromium.launch(process.env.CHROMIUM ? {executablePath:process.env.CHROMIUM} : {});
let failures=0;
function check(name,ok,detail=''){console.log(`${ok?'PASS':'FAIL'} ${name}${ok?'':' '+detail}`); if(!ok) failures++;}
const fixture=JSON.parse(readFileSync(path.join(ROOT,'tests/fixtures/kuwait-2026-10-20.json')));
const weather={current:{temperature_2m:31,weather_code:0},daily:{time:['2026-10-20','2026-10-21','2026-10-22','2026-10-23','2026-10-24'],temperature_2m_max:[33,34,32,31,30],temperature_2m_min:[22,23,22,21,21],weather_code:[0,1,2,0,0]}};
const allowed=new Set(['api.aladhan.com','api.open-meteo.com','air-quality-api.open-meteo.com']);
async function open(file,hash='',{width=1920,height=1080,online=false}={}){
 const ctx=await browser.newContext({viewport:{width,height}}), p=await ctx.newPage(), errors=[], requests=[];
 p.on('pageerror',e=>errors.push(e.message));
 await p.clock.install({time:new Date('2026-10-20T09:30:00Z')});
 await ctx.route(/^https?:/, r=>{
  const u=new URL(r.request().url()); requests.push(u);
  if(!online) return r.abort();
  const body=u.hostname==='api.aladhan.com'?fixture:u.hostname==='api.open-meteo.com'?weather:{current:{pm10:20,dust:10}};
  if(!allowed.has(u.hostname)) return r.abort();
  return r.fulfill({json:body,headers:{'Access-Control-Allow-Origin':'*'}});
 });
 await p.goto('file://'+path.join(ROOT,'dist',file)+'#'+hash);
 await p.waitForFunction(()=>document.documentElement.classList.contains('ready'));
 await p.waitForTimeout(250);
 return {ctx,p,errors,requests};
}
for(const file of ['urtv.html','tv.html']) for(const [width,height] of [[1920,1080],[1280,720],[960,540]]){
 const {ctx,p,errors,requests}=await open(file,'',{width,height});
 const r=await p.evaluate(()=>({city:CONFIG.city,tz:CONFIG.timezone,panel:CONFIG.panel,clock:document.getElementById('ck-h').textContent,rows:document.querySelectorAll('#week-list li').length,body:document.querySelector('.stage').innerText,
  photo:document.querySelector('.photo').style.backgroundImage, overflow:[...document.querySelectorAll('.stage *')].filter(e=>{const b=e.getBoundingClientRect();return b.width&&b.height&&(b.left< -2||b.right>innerWidth+2||b.bottom>innerHeight+2);}).map(e=>e.id||e.className)}));
 const tag=`${file} ${width}x${height}`;
 check(tag+' renders offline without script errors',!errors.length,errors.join(';'));
 check(tag+' Kuwait defaults and Arabic clock',r.city==='مدينة الكويت'&&r.tz==='Asia/Kuwait'&&r.panel==='prayers'&&r.clock==='١٢',JSON.stringify(r));
 check(tag+' offline prayer table has all six times',r.rows===6,r.rows);
 check(tag+' no sports, markets, shows or quotes',!(/المباريات|تاسي|بيتكوين|المسلسلات|آية اليوم|حديث اليوم/.test(r.body)));
 check(tag+' no overflow',r.overflow.length===0,r.overflow.join(','));
 const decoded=await p.evaluate(async()=>{try { const i=new Image(); i.src=BGS[0].u; await i.decode(); return i.naturalWidth>=1920; } catch { return false; }});
 check(tag+' local wallpaper actually decodes',!!r.photo&&decoded);
 check(tag+' only necessary data requests',requests.every(u=>allowed.has(u.hostname)),requests.map(u=>u.href).join(','));
 await ctx.close();
}
// Kuwait method fallback is within a few minutes of the stored independent API reference.
{
 const {ctx,p}=await open('tv.html');
 const actual=await p.evaluate(()=>computePrayers());
 const minutes=s=>{const [h,m]=s.split(':').map(Number);return h*60+m;};
 const deltas=Object.keys(actual).map(k=>[k,Math.abs(minutes(actual[k])-minutes(fixture.data.timings[k]))]);
 check('offline prayer algorithm agrees with Kuwait method 9 (<=3 minutes)',deltas.every(([,d])=>d<=3),JSON.stringify(deltas));
 await ctx.close();
}
for(const panel of ['prayers','forecast','none','matches','shows','verse']){
 const {ctx,p,errors,requests}=await open('tv.html','panel='+panel,{online:true});
 await p.waitForTimeout(200);
 const r=await p.evaluate(()=>({panel:CONFIG.panel,rows:document.querySelectorAll('#week-list li').length,shown:getComputedStyle(document.querySelector('.week')).display!=='none',temp:document.getElementById('wx-temp').textContent}));
 check('panel '+panel+' handles current and removed choices',!errors.length&&(panel==='none'?!r.shown: r.shown&&r.rows===(panel==='forecast'?5:6)),JSON.stringify(r)+errors.join(';'));
 const prayer=requests.find(u=>u.hostname==='api.aladhan.com');
 check('prayer request uses Kuwait method, location, timezone',prayer?.searchParams.get('method')==='9'&&prayer.searchParams.get('timezonestring')==='Asia/Kuwait'&&prayer.searchParams.get('latitude')==='29.3759');
 await ctx.close();
}
for(const panel of ['prayers','forecast','none']){
 const {ctx,p,errors,requests}=await open('tv.html','widget=1&panel='+panel,{width:461,height:300,online:true});
 const r=await p.evaluate(()=>({bg:getComputedStyle(document.body).backgroundColor,hidden:[...document.querySelectorAll('.photo,.top,.clockwrap,.bottom')].every(e=>getComputedStyle(e).display==='none')}));
 check('home card '+panel+' is transparent and isolated',!errors.length&&r.bg==='rgba(0, 0, 0, 0)'&&r.hidden,errors.join(';'));
 check('home card '+panel+' fetches only its data',panel==='none'?requests.length===0:requests.every(u=>panel==='prayers'?u.hostname==='api.aladhan.com':u.hostname.includes('open-meteo.com')));
 await ctx.close();
}
// The native launcher reserves the right-hand top 80px for its clock/date and the area below for apps.
for(const panel of ['prayers','none']) for(const width of [864,720]){
 const {ctx,p,errors,requests}=await open('tv.html','widget=1&home=1&panel='+panel,{width,height:238,online:true});
 await p.waitForFunction(()=>document.getElementById('wx-temp').textContent==='٣١°');
 const r=await p.evaluate(()=>{
  const weather=document.querySelector('.weather').getBoundingClientRect(), card=document.querySelector('.week').getBoundingClientRect();
  const last=document.querySelector('#week-list li:last-child').getBoundingClientRect();
  return {weatherVisible:weather.width>0&&weather.height>0,weatherTop:weather.top,
   weatherBottom:weather.bottom,cardBottom:card.bottom,lastBottom:last.bottom,
   separated:card.width===0||card.right<=weather.left,rows:document.querySelectorAll('#week-list li').length};
 });
 check(`home ${panel} ${width}: weather below native clock, above apps`,!errors.length&&r.weatherVisible&&r.weatherTop>=80&&r.weatherBottom<=238,JSON.stringify(r));
 check(`home ${panel} ${width}: full prayer table fits beside weather`,r.separated&&(panel==='none'||r.rows===6&&r.cardBottom<=238&&r.lastBottom<=r.cardBottom),JSON.stringify(r));
 check(`home ${panel}: fetches current weather with optional prayers`,requests.some(u=>u.hostname==='api.open-meteo.com')&&(panel==='none'||requests.some(u=>u.hostname==='api.aladhan.com')));
 await ctx.close();
}
{
 const {ctx,p,errors,requests}=await open('tv.html','',{online:true});
 await p.waitForFunction(()=>document.getElementById('wx-temp').textContent==='٣١°');
 await p.evaluate(()=>saveSnap());
 await ctx.unrouteAll(); await ctx.route(/^https?:/,r=>r.abort());
 await p.reload(); await p.waitForTimeout(300);
 check('weather survives offline restart',(await p.locator('#wx-temp').innerText())==='٣١°');
 const before=await p.locator('#ck-m').innerText();
 await p.clock.runFor(61000);
 check('minute timer advances clock without script errors',before!==(await p.locator('#ck-m').innerText())&&!errors.length,errors.join(';'));
 await ctx.close();
}
// Malformed prayer responses must not replace valid local times with NaN.
{
 const {ctx,p,errors}=await open('tv.html');
 await ctx.unrouteAll(); await ctx.route('**/timings/**',r=>r.fulfill({json:{data:{timings:{Fajr:'bad'}}}}));
 await p.evaluate(()=>loadPrayers());
 check('invalid API data falls back cleanly',!errors.length&&!(await p.locator('.stage').innerText()).includes('NaN'));
 await ctx.close();
}
await browser.close();
console.log(failures?`${failures} failed`:'all Kuwait page tests passed');
process.exit(failures?1:0);
