#!/usr/bin/env python3
"""Import date-validated public Al-Ojeiri records; never synthesize missing times."""
import argparse,datetime,json,re,subprocess,time,pathlib
URL='https://www.alojeiri.com/ar/ojeiri-calendar'
def parse(payload,day):
    h=json.loads(payload)['html']
    if not re.search(r'id="datepicker"\s+value="'+re.escape(day)+'"',h):raise ValueError('date mismatch')
    times=re.findall(r'class="item-prayer prayer_time"\s+data-time="(\d\d:\d\d):\d\d"',h)
    if len(times)!=6:raise ValueError('official times unavailable')
    if any(not re.fullmatch(r'(?:[01][0-9]|2[0-3]):[0-5][0-9]',t) for t in times):raise ValueError('invalid time')
    values=[int(t[:2])*60+int(t[3:]) for t in times]
    if not (180<=values[0]<=420 and 600<=values[2]<=840 and 900<=values[4]<=1260 and values==sorted(set(values))):raise ValueError('invalid prayer sequence')
    labels=re.findall(r'<p>(.*?)</p>',h.split('class="ls-prayer')[0])
    return dict(times=times,hijri=labels[2].strip() if len(labels)>2 else '')
def main():
    p=argparse.ArgumentParser();p.add_argument('start');p.add_argument('end');p.add_argument('--output',default='assets/calendar/alojeiri.json');a=p.parse_args()
    path=pathlib.Path(a.output)
    data=json.loads(path.read_text()) if path.exists() else dict(source=URL,city='مدينة الكويت',country_id=1,days={})
    day=datetime.date.fromisoformat(a.start);end=datetime.date.fromisoformat(a.end)
    while day<=end:
        key=day.isoformat()
        if key not in data['days']:
            try:
                body=subprocess.check_output(['curl','-fsSL','--max-time','15','-H','X-Requested-With: XMLHttpRequest',URL+'?country_id=1&date='+key],text=True)
                data['days'][key]=parse(body,key)
                data['retrieved']=datetime.datetime.now(datetime.timezone.utc).date().isoformat();path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
            except Exception as e:print(key,str(e),flush=True)
            time.sleep(.12)
        day+=datetime.timedelta(days=1)
    print('Verified records:',len(data['days']),flush=True)
if __name__=='__main__':main()
