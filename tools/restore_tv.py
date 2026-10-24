#!/usr/bin/env python3
"""Restore a reviewed, removed original TV host from the system partition only."""
import argparse
import device_safety as safety
from install import ADB


def restore(adb, audit_path, review_path):
    audit = safety.read(audit_path)
    evidence = audit.get('tv_recovery', {})
    operation = dict(kind='restore-tv-package', package='com.smartdevice.livetv',
                     evidence_sha256=safety.digest(evidence))
    record = safety.gate(adb.shell, adb.serial, audit_path, review_path,
                         [operation], recovery=True)
    print('Mandatory verification record: ' + str(record), flush=True)
    try:
        output = adb.shell('cmd', 'package', 'install-existing', '--user', '0', operation['package'])
        after = safety.tv_recovery_evidence(adb.shell, audit['identity'])
        if after != dict(evidence, installed=True):
            raise RuntimeError('Unexpected restoration state; stop and inspect. ' + output)
        safety.finish(record, [dict(ok=True, operation=operation, output=output)])
    except Exception as error:
        safety.finish(record, [dict(ok=False, operation=operation, error=str(error))])
        raise
    print('Original TV host restored. Functional verification is still pending.')
    return record


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--adb', default='adb')
    parser.add_argument('--serial', required=True)
    parser.add_argument('--audit', required=True)
    parser.add_argument('--review', required=True)
    args = parser.parse_args()
    restore(ADB(args.adb, args.serial), args.audit, args.review)
