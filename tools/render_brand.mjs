// Code-native brand artwork; no generated UI or screenshot overlays.
// Run from repository root: node tools/render_brand.mjs (requires Playwright).
import { chromium } from 'playwright';
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
const mark=readFileSync('.github/brand/mark.svg','utf8');
const font=readFileSync('android/launcher/src/main/assets/fonts/Thmanyah-Regular.otf').toString('base64');
const browser=await chromium.launch(process.env.CHROMIUM ? {executablePath:process.env.CHROMIUM} : {});
for(const [module,subtitle] of [['launcher','الرئيسية · الكويت'],['app','شاشة التوقف · الكويت']]){
 const svg=`<svg xmlns="http://www.w3.org/2000/svg" width="480" height="270" viewBox="0 0 480 270"><style>@font-face{font-family:T;src:url(data:font/otf;base64,${font})}text{font-family:T,sans-serif}</style><rect width="480" height="270" fill="#091610"/><svg x="42" y="70" width="124" height="124" viewBox="0 0 512 512">${mark.replace(/^<svg[^>]*>/,'').replace(/<\/svg>\s*$/,'')}</svg><text x="188" y="137" fill="#f6efe0" font-size="43" font-weight="500">KLightTV</text><text x="310" y="170" fill="#dcb86f" font-size="18" text-anchor="middle" direction="rtl">${subtitle}</text></svg>`;
 writeFileSync(`.github/brand/${module}-banner.svg`,svg);
 for(const [density,width,height] of [['xhdpi',320,180],['xxhdpi',480,270]]){
  const page=await browser.newPage({viewport:{width,height},deviceScaleFactor:1});
  await page.setContent(`<html><style>body{margin:0}svg{width:100vw;height:100vh}</style>${svg}</html>`);
  await page.evaluate(()=>document.fonts.ready);
  await page.screenshot({path:`android/${module}/src/main/res/drawable-${density}/banner.png`});await page.close();
 }
}
await browser.close();
console.log('Rendered both KLightTV Android banners');
