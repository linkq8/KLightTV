"""Source parser validation and shipped Ramadan archive completeness."""
import datetime,importlib.util,json,pathlib,unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('calendar_import',ROOT/'tools/fetch_calendar.py');module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
def payload(times,date='2026-03-01'):
    html='<input id="datepicker" value="'+date+'"><p>Kuwait</p><p>Date</p><p>12 Ramadan 1447</p><div class="ls-prayer">'
    html+=''.join('<div class="item-prayer prayer_time" data-time="'+t+':00"></div>' for t in times)
    return json.dumps({'html':html})
class CalendarTests(unittest.TestCase):
    def test_valid_record_and_wrong_date(self):
        raw=payload(['05:05','06:25','12:02','15:14','17:39','18:57'])
        self.assertEqual(len(module.parse(raw,'2026-03-01')['times']),6)
        with self.assertRaises(ValueError):module.parse(raw,'2027-03-01')
    def test_missing_duplicate_invalid_or_out_of_order(self):
        for times in [[],['12:00']*6,['05:99','06:25','12:02','15:14','17:39','18:57'],['05:05','06:25','12:02','15:14','18:57','17:39']]:
            with self.assertRaises(ValueError):module.parse(payload(times),'2026-03-01')
    def test_shipped_archive(self):
        data=json.loads((ROOT/'assets/calendar/alojeiri.json').read_text())
        days=sorted((date,record) for date,record in data['days'].items() if '\u0631\u0645\u0636\u0627\u0646' in record['hijri'] and '1447' in record['hijri'])
        self.assertEqual(len(days),30)
        first=datetime.date.fromisoformat(days[0][0])
        for n,(date,record) in enumerate(days):
            self.assertEqual(datetime.date.fromisoformat(date),first+datetime.timedelta(days=n))
            self.assertEqual(int(record['hijri'].split()[0]),n+1)
            self.assertEqual(len(record['times']),6)
        self.assertFalse(any('1448' in r['hijri'] and '\u0631\u0645\u0636\u0627\u0646' in r['hijri'] for r in data['days'].values()))
if __name__=='__main__':unittest.main()
