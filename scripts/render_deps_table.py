#!/usr/bin/env python3
"""جدول Markdown تغییرات وابستگی‌ها را از گزارش JSON می‌سازد (برای بدنه PR)."""

from __future__ import annotations

import json
import sys


def main() -> int:
    if len(sys.argv) < 2:
        print("usage: render_deps_table.py <report.json>", file=sys.stderr)
        return 2
    try:
        with open(sys.argv[1], encoding="utf-8") as handle:
            data = json.load(handle)
    except (OSError, json.JSONDecodeError) as exc:
        print(f"⚠️ گزارش وابستگی‌ها خوانده نشد: {exc}", file=sys.stderr)
        return 1

    for item in data.get("updates", []):
        print(f"| {item['kind']} | `{item['name']}` | `{item['old']}` | `{item['new']}` |")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
