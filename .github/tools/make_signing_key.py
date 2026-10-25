#!/usr/bin/env python3
"""Make the apps' signing key once, on your own computer, for the repository's secrets.

The key signs both APKs. Existing devices require the same key for updates.
Key material is saved with owner-only permissions outside the repository and never printed.
Install cryptography, then run this tool locally. If gh is signed in it uploads repository
secrets through stdin; otherwise use the private files via GitHub Settings > Secrets.
Never replace an existing signing key just to resolve a build or installation error.
"""
import base64
import datetime
import os
import secrets
import shutil
import subprocess
import sys
from pathlib import Path

from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.primitives.serialization import pkcs12
from cryptography.x509.oid import NameOID

ALIAS = b"urtv"   # android/*/build.gradle signs with this alias
REPO = "linkq8/KLightTV"


def make(password):
    key = rsa.generate_private_key(public_exponent=65537, key_size=4096)
    name = x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, "UrTV"), x509.NameAttribute(NameOID.ORGANIZATION_NAME, "UrTV")])
    now = datetime.datetime.now(datetime.timezone.utc)
    cert = (x509.CertificateBuilder().subject_name(name).issuer_name(name).public_key(key.public_key())
            .serial_number(x509.random_serial_number()).not_valid_before(now)
            .not_valid_after(now + datetime.timedelta(days=365 * 100))   # Android wants it valid long after the last update
            .sign(key, hashes.SHA256()))
    return pkcs12.serialize_key_and_certificates(ALIAS, key, cert, None,
                                                 serialization.BestAvailableEncryption(password.encode()))


def main():
    out = Path.home() / "urtv-signing-key.p12"
    if out.exists() or out.with_name("urtv-signing-key-password.txt").exists() or out.with_name("urtv-keystore-base64.txt").exists():
        sys.exit(f"{out} already exists: that is your key. Use it, or move it away first to make a new one.")
    password = secrets.token_urlsafe(30)
    data = make(password)
    def private_write(path, content):
        # O_EXCL refuses existing files and symlinks; mode is private from the first write.
        fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
        with os.fdopen(fd, "wb") as stream:
            stream.write(content)
    private_write(out, data)
    private_write(out.with_name("urtv-signing-key-password.txt"), (password + "\n").encode())
    print(f"Key saved to {out}, its password to {Path.home() / 'urtv-signing-key-password.txt'}")
    print("Keep both somewhere safe (a password manager, a USB stick); never commit them.\n")
    secret = {"ANDROID_KEYSTORE_B64": base64.b64encode(data).decode(), "ANDROID_KEYSTORE_PASSWORD": password}
    gh = gh_ready()
    if gh:
        for name, value in secret.items():   # the value goes in on stdin, never on the command line
            subprocess.run([gh, "secret", "set", name, "--repo", REPO], input=value, text=True, check=True)
        print(f"Both secrets are now in {REPO}. The next build signs with this key.")
        return
    encoded = out.with_name("urtv-keystore-base64.txt")
    private_write(encoded, (secret["ANDROID_KEYSTORE_B64"] + "\n").encode())
    print("GitHub CLI is not signed in. Add the two Actions secrets using the private local files.")
    print(f"Base64 file: {encoded}; password file: {out.with_name('urtv-signing-key-password.txt')}")
    print("Secret values are never written to stdout or logs.")


def gh_ready():
    """GitHub CLI's path if it is installed and signed in, else None."""
    gh = shutil.which("gh")
    return gh if gh and subprocess.run([gh, "auth", "status"], capture_output=True).returncode == 0 else None


if __name__ == "__main__":
    main()
