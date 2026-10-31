"""Inventory literal tr(ru, en[, vi]) calls without interpreting user content.

Run with Python 3 from the repository root. This is a source audit, not a Kotlin
parser: computed arguments are excluded; Settings enum fields are counted separately.
"""
import json
from pathlib import Path
import re

STRING = r'"(?:\\.|[^"\\])*"'
EXPR = rf'{STRING}(?:\s*\+\s*{STRING})*'
CALL = re.compile(rf'\btr\(\s*({EXPR})\s*,\s*({EXPR})\s*(,\s*{EXPR})?')
PAIR = re.compile(rf'^\s*({STRING}) to ({STRING}),?$', re.MULTILINE)
ENUM = re.compile(r'^\s+[A-Z][A-Z_]*\((.*)\),?$', re.MULTILINE)


def literal(expression):
    return "".join(json.loads(token) for token in re.findall(STRING, expression))


def inventory():
    root = Path("app/src/main/java")
    catalog_path = root / "com/akira/island/core/VietnameseStrings.kt"
    pairs = [
        (literal(m[1]), literal(m[2]))
        for m in PAIR.finditer(catalog_path.read_text(encoding="utf-8"))
    ] if catalog_path.exists() else []
    catalog = dict(pairs)
    assert len(catalog) == len(pairs), "Duplicate exact-English catalog key"
    static, dynamic, explicit, missing = [], [], [], []
    for path in sorted(root.rglob("*.kt")):
        if path.name in {"Lang.kt", "VietnameseStrings.kt"}:
            continue
        source = path.read_text(encoding="utf-8")
        for match in CALL.finditer(source):
            en = literal(match[2])
            entry = (str(path), source.count("\n", 0, match.start()) + 1, en)
            if match[3]:
                explicit.append(entry)
            elif re.search(r'\$(?:[A-Za-z_]|\{)', en):
                dynamic.append(entry)
            else:
                static.append(entry)
                if en not in catalog:
                    missing.append(entry)
    return catalog, static, dynamic, explicit, missing


if __name__ == "__main__":
    catalog, static, dynamic, explicit, missing = inventory()
    settings = Path("app/src/main/java/com/akira/island/data/Settings.kt").read_text(encoding="utf-8")
    enum_labels = [
        literal(token)
        for match in ENUM.finditer(settings)
        for token in re.findall(STRING, match[1])[1::2]
    ]
    print(f"Catalog: {len(catalog)} exact English keys")
    print(f"Catalog values: {sum(en != vi for en, vi in catalog.items())} localized, "
          f"{sum(en == vi for en, vi in catalog.items())} intentionally identical")
    print(f"Static two-argument calls: {len(static)}, distinct keys: {len({e[2] for e in static})}")
    print(f"Covered static calls: {len(static) - len(missing)}; English fallback calls: {len(missing)}")
    print(f"Covered distinct static keys: {len({e[2] for e in static}) - len({e[2] for e in missing})}")
    print(f"Settings constructor labels/hints: {len(enum_labels)}, covered: {sum(e in catalog for e in enum_labels)}")
    print(f"Explicit three-argument calls: {len(explicit)}")
    print(f"Interpolated two-argument calls: {len(dynamic)}")
    for label, entries in (("MISSING", missing), ("DYNAMIC", dynamic)):
        for path, line, en in entries:
            print(f"{label} {path}:{line}: {en!r}")
    for label in enum_labels:
        if label not in catalog:
            print(f"MISSING ENUM {label!r}")
