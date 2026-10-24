import {chromium} from 'playwright';
import {spawn} from 'node:child_process';
import {mkdir} from 'node:fs/promises';
import assert from 'node:assert/strict';
const server=spawn('python3',['tools/setup/server.py','--no-browser'],{stdio:['ignore','pipe','inherit']});let browser;
try{
 const url=await new Promise((resolve,reject)=>{const timer=setTimeout(()=>reject(Error('server timeout')),10000);server.stdout.once('data',b=>{clearTimeout(timer);resolve(b.toString().trim());});server.once('error',reject);});
 browser=await chromium.launch({headless:true,...(process.env.CHROMIUM?{executablePath:process.env.CHROMIUM}:{channel:'chrome'})});const page=await browser.newPage({viewport:{width:1440,height:1000}});const errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.route('**/api/cities?*',route=>{const country=new URL(route.request().url()).searchParams.get('country');route.fulfill({json:{results:[{name:country==='SA'?'مكة المكرمة':'القاهرة',country_code:country,latitude:country==='SA'?21.4:30.04,longitude:country==='SA'?39.8:31.23,timezone:country==='SA'?'Asia/Riyadh':'Africa/Cairo'}]}});});
 await page.goto(url);await page.locator('#country').waitFor().catch(async e=>{console.error(errors,await page.locator('body').innerText());throw e;});assert.equal(await page.locator('#country').inputValue(),'KW');await mkdir('outputs/setup-verification',{recursive:true});await page.screenshot({path:'outputs/setup-verification/first-light.png',fullPage:true});
 await page.locator('#country').selectOption('SA');assert(await page.locator('#next').isDisabled());await page.locator('#query').fill('مكة');await page.getByRole('button',{name:'بحث',exact:true}).click();await page.locator('#results button').click();assert.match(await page.locator('#source').textContent(),/أم القرى/);
 await page.locator('#country').selectOption('EG');await page.locator('#query').fill('القاهرة');await page.getByRole('button',{name:'بحث',exact:true}).click();await page.locator('#results button').click();assert.match(await page.locator('#source').textContent(),/الهيئة المصرية/);
 await page.locator('#next').click();await page.locator('#theme [data-value="dark"]').click();await page.locator('#reading [data-value="1"]').click();await page.screenshot({path:'outputs/setup-verification/second-dark.png',fullPage:true});
 await page.setViewportSize({width:390,height:844});await page.screenshot({path:'outputs/setup-verification/mobile.png',fullPage:true});assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
 await page.locator('#next').click();await page.locator('#done').waitFor({state:'visible'});const saved=await page.evaluate(()=>JSON.parse(localStorage.getItem('klighttv-first-setup-v2')));assert.equal(saved.prayer.method,'egypt');assert.equal(saved.appearance.theme,'dark');assert.equal(saved.appearance.reading,1);assert.equal(saved.remote.mode,'original');assert.deepEqual(saved.applications,[]);
 await page.goto(url);await page.locator('#done').waitFor({state:'visible'});assert.equal(await page.locator('#wizard').isVisible(),false);await page.locator('#edit').click();await page.locator('#country').waitFor();assert.equal(await page.locator('#country').inputValue(),'EG');assert.deepEqual(errors,[]);
 const denied=await fetch(new URL('/api/device',url));assert.equal(denied.status,403);console.log('PASS two steps, country/city guard, SA/EG mapping, themes, mobile, local save, skip repeat setup, edit and authentication');
}finally{if(browser)await browser.close();server.kill();}
