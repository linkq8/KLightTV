"""Legacy mutation guards and Git Bash path normalization, without a TV."""
import base64, hashlib, os, random, re, sys, tempfile, types

FS = {}
stats = {"drop": 0, "lost": 0}

def run(cmd):
    out = []
    for part in [c.strip() for c in cmd.split(";")]:
        m = re.fullmatch(r"echo -n '([^']*)' >> (\S+)", part)
        if m: FS[m.group(2)] = FS.get(m.group(2), "") + m.group(1); continue
        m = re.fullmatch(r"wc -c < (\S+)", part)
        if m: out.append(str(len(FS.get(m.group(1), "")))); continue
        m = re.fullmatch(r"\[ -f (\S+) \] && wc -c < \S+ \|\| echo 0", part)
        if m: out.append(str(len(FS[m.group(1)])) if m.group(1) in FS else "0"); continue
        m = re.fullmatch(r"head -c (\d+) (\S+) > \S+ && mv \S+ \S+", part)
        if m: FS[m.group(2)] = FS[m.group(2)][:int(m.group(1))]; continue
        m = re.fullmatch(r"rm -f (.+)", part)
        if m:
            for f in m.group(1).split(): FS.pop(f, None)
            continue
        m = re.fullmatch(r"\[ -f (\S+) \] && base64 -d \S+ > (\S+) && rm -f \S+", part)
        if m:
            if m.group(1) in FS: FS[m.group(2)] = base64.b64decode(FS[m.group(1)]); FS.pop(m.group(1))
            continue
        m = re.fullmatch(r"\[ -f (\S+) \] && echo y", part)
        if m: out.append("y" if m.group(1) in FS else ""); continue
        m = re.fullmatch(r"\[ -d '(\S+)' \] && echo y", part)
        if m: out.append("y" if m.group(1) == "/data/local/tmp" else ""); continue
        m = re.fullmatch(r"df -h (\S+) \| tail -1", part)
        if m: out.append(m.group(1) + " 4.0G 3.0G 1.0G 75%"); continue
        m = re.fullmatch(r"md5sum (\S+)", part)
        if m:
            v = FS[m.group(1)]
            out.append(hashlib.md5(v if isinstance(v, bytes) else v.encode()).hexdigest() + "  " + m.group(1)); continue
        raise SystemExit("fake: unknown command " + part)
    return "\n".join(out)

class Dev:
    def __init__(self, *a, **k): pass
    def connect(self, **k):
        if random.random() < 0.1: stats["drop"] += 1; raise ConnectionError("refused")
    def shell(self, cmd, read_timeout_s=0):
        if random.random() < 0.08: stats["drop"] += 1; raise ConnectionResetError("dropped before")
        r = run(cmd)
        if random.random() < 0.08: stats["lost"] += 1; raise TimeoutError("reply lost")   # ran, reply lost
        return r
    def close(self): pass

for name in ["adb_shell", "adb_shell.adb_device", "adb_shell.auth", "adb_shell.auth.keygen", "adb_shell.auth.sign_pythonrsa"]:
    sys.modules[name] = types.ModuleType(name)
sys.modules["adb_shell.adb_device"].AdbDeviceTcp = Dev
sys.modules["adb_shell.auth.keygen"].keygen = lambda p: None
sys.modules["adb_shell.auth.sign_pythonrsa"].PythonRSASigner = lambda a, b: None
os.environ["TV_HOST"] = "1.2.3.4"; os.environ["TV_TRIES"] = "6"
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "optimize"))
import tv
tv.time.sleep = lambda s: None
tv.signer = lambda: None

# The legacy transport is now intentionally read-only. Upload tests must verify
# rejection, rather than bypass the safety gate to exercise obsolete writes.
with tempfile.TemporaryDirectory() as folder:
    src=os.path.join(folder,"x.apk")
    with open(src,"wb") as f:f.write(b"fake apk")
    for action in [lambda:tv.TV().shell("rm -f /data/local/tmp/x.apk"),
                   lambda:tv.TV().call("push",src,"/data/local/tmp/x.apk"),
                   lambda:tv.push_b64(tv.TV(),src,"/data/local/tmp/x.apk")]:
        try:action();raise AssertionError("legacy mutation permitted")
        except ValueError:pass
assert not FS,"blocked upload touched fake device"
bad=[]
print("PASS legacy shell, push and chunked upload blocked before device writes")

# Git Bash on Windows rewrites /data/local/tmp/x into its own folder before Python sees it: tv.py cuts it back off
tv.is_git_folder = lambda f: f in ("C:/Program Files/Git", "D:/data/Git")
real_name, tv.os.name = tv.os.name, "nt"
paths = {
    "C:/Program Files/Git/data/local/tmp/x.apk": "/data/local/tmp/x.apk",
    "C:\\Program Files\\Git\\sdcard\\shot.png": "/sdcard/shot.png",
    "D:/data/Git/data/local/tmp/x.apk": "/data/local/tmp/x.apk",   # a Git folder that itself has /data/ in it
    "C:/Users/me/data/x.apk": "C:/Users/me/data/x.apk",             # a real Windows path stays as it is
    "/data/local/tmp/x.apk": "/data/local/tmp/x.apk",
    "pm install -r /data/local/tmp/x.apk": "pm install -r /data/local/tmp/x.apk",
}
wrong = {k: tv.tv_path(k) for k, v in paths.items() if tv.tv_path(k) != v}
tv.os.name = real_name
print(f"Git Bash paths: {len(paths) - len(wrong)}/{len(paths)} put back" + (f": wrong {wrong}" if wrong else ""))
sys.exit(1 if bad or wrong else 0)
