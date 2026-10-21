#!/usr/bin/env python3
"""Start the checklist for any explicitly selected authorized Android device."""
import os,pathlib,shutil,subprocess,sys

def main():
    root=pathlib.Path(__file__).resolve().parent
    candidates=[os.environ.get('ADB'),shutil.which('adb'),str(pathlib.Path.home()/'Library/Android/sdk/platform-tools/adb'),str(pathlib.Path.home()/'AppData/Local/Android/Sdk/platform-tools/adb.exe')]
    adb=next((p for p in candidates if p and pathlib.Path(p).is_file()),None)
    if not adb:raise SystemExit('Install Android SDK Platform-Tools and add adb to PATH (or set ADB).')
    result=subprocess.run([adb,'devices'],capture_output=True,text=True,check=True)
    devices=[line.split()[0] for line in result.stdout.splitlines()[1:] if len(line.split())==2 and line.split()[1]=='device']
    print('Kuwait TV Manager / إدارة التطبيقات — Android TV / Google TV')
    for n,serial in enumerate(devices,1):print(f'{n}. {serial}')
    choice=input('Select device number, or enter TV IP:port / اختر جهازًا أو أدخل عنوان التلفزيون: ').strip()
    if choice.isdigit() and 1<=int(choice)<=len(devices):serial=devices[int(choice)-1]
    else:
        import ipaddress
        host,sep,port=choice.partition(':')
        try:ipaddress.IPv4Address(host);port=int(port) if sep else 5555;assert 1<=port<=65535
        except (ValueError,AssertionError):raise SystemExit('Invalid IPv4 address or port.')
        serial=f'{host}:{port}'
        print(subprocess.run([adb,'connect',serial],capture_output=True,text=True,check=True).stdout)
        input('Allow debugging on the TV, then press Enter / وافق على الاتصال ثم اضغط Enter: ')
    subprocess.run([adb,'-s',serial,'get-state'],check=True)
    # No mutations: the browser requires an explicit reviewed selection first.
    subprocess.run([sys.executable,str(root/'server.py'),'--adb',adb,'--serial',serial],check=True)

if __name__=='__main__':main()
