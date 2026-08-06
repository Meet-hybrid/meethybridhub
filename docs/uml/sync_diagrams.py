#!/usr/bin/env python3
"""
sync_diagrams.py — keep the draw.io and Mermaid diagram sets in sync.

The .drawio files are the EDITABLE source of truth (opened in draw.io).
The mermaid/*.md files are the GitHub-native VIEWS (rendered automatically
by github.com). This script keeps the two in sync:

  check    - validate structure, cross-check both formats cover the same
             diagrams, and flag names (classes/tables) missing from either side
  preview  - build docs/uml/preview.html rendering every Mermaid diagram in
             one page (no GitHub needed; open it in any browser)

Usage:
  python3 sync_diagrams.py check [--verbose]
  python3 sync_diagrams.py preview [--out docs/uml/preview.html]

Exit code: 0 = all good, 1 = problems found, 2 = usage error.

No third-party dependencies — pure Python stdlib (xml.etree + re).
"""

from __future__ import annotations

import argparse
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

# ---------------------------------------------------------------------------
# Locations (script lives in docs/uml/)
# ---------------------------------------------------------------------------
UML_DIR = Path(__file__).resolve().parent
MERMAID_DIR = UML_DIR / "mermaid"

# Draw.io class boxes are cells with a swimlane style. ER tables are cells
# with shape=table whose header cell (a tableCell inside the first tableRow)
# carries the table name.
SWIMLANE_MARKER = "swimlane"
TABLE_MARKER = "shape=table"
TABLE_ROW_MARKER = "tableRow"
TABLE_CELL_MARKER = "tableCell"

# A mermaid class is declared as "class X" / "class X {" ; a mermaid ER table
# is declared as "users {" / "users {".
MERMAID_CLASS_RE = re.compile(r"^\s*class\s+([A-Za-z_][A-Za-z0-9_]*)\b", re.MULTILINE)
MERMAID_ER_TABLE_RE = re.compile(r"^\s*([a-z_][a-z0-9_]*)\s*\{", re.MULTILINE)

# Expected file-name convention: "NN-name.ext"
STEM_RE = re.compile(r"^(\d{2})-(.+)$")


# ---------------------------------------------------------------------------
# draw.io parsing
# ---------------------------------------------------------------------------
def parse_drawio(path: Path) -> ET.Element | None:
    """Parse a .drawio file. Returns the <root> element, or None on error."""
    try:
        tree = ET.parse(path)
    except ET.ParseError as e:
        print(f"  ✗ XML error in {path.name}: {e}")
        return None
    root = tree.getroot()
    # draw.io normally wraps the model in <mxfile><diagram>… but a bare
    # <mxGraphModel> root is legal too — accept both.
    model = root.find(".//mxGraphModel")
    if root.tag == "mxGraphModel":
        model = root
    root_cell = model.find("root") if model is not None else None
    if root_cell is None:
        print(f"  ✗ {path.name}: missing mxGraphModel/root structure")
        return None
    return root_cell


def drawio_cells(root: ET.Element) -> list[ET.Element]:
    return list(root.iter("mxCell"))


def drawio_class_names(root: ET.Element) -> set[str]:
    """Class names from swimlane boxes (strip stereotypes/annotations)."""
    names: set[str] = set()
    for cell in drawio_cells(root):
        style = cell.get("style") or ""
        value = (cell.get("value") or "").strip()
        if SWIMLANE_MARKER in style and value:
            first_line = value.split("&#10;")[0].split("\n")[0].strip()
            # Drop stereotypes like «enum», «abstract», and JPA annotations.
            first_line = re.sub(r"^«[^»]*»\s*", "", first_line)
            first_line = re.sub(r"^@\w+\s*", "", first_line)
            # Drop parenthetical notes, e.g. "AppUser  (implements UserDetails)".
            first_line = re.sub(r"\s*\(.*?$", "", first_line).strip()
            if first_line:
                names.add(first_line)
    return names


def drawio_table_names(root: ET.Element) -> set[str]:
    """Table names from shape=table containers (via their header cell)."""
    names: set[str] = set()
    for cell in drawio_cells(root):
        style = cell.get("style") or ""
        if TABLE_MARKER not in style:
            continue
        # Header row = first tableRow child; name lives in its tableCell.
        for child in drawio_cells(root):
            if child.get("parent") != cell.get("id"):
                continue
            if TABLE_ROW_MARKER in (child.get("style") or ""):
                for grandchild in drawio_cells(root):
                    if grandchild.get("parent") != child.get("id"):
                        continue
                    value = (grandchild.get("value") or "").strip()
                    if TABLE_CELL_MARKER in (grandchild.get("style") or "") and value:
                        names.add(value)
                break
    return names


# ---------------------------------------------------------------------------
# Mermaid parsing
# ---------------------------------------------------------------------------
def mermaid_blocks(text: str) -> list[str]:
    """All ```mermaid ... ``` code blocks in a markdown file."""
    return re.findall(r"```mermaid\s*\n(.*?)```", text, re.DOTALL)


def mermaid_class_names(block: str) -> set[str]:
    return set(MERMAID_CLASS_RE.findall(block))


def mermaid_er_table_names(block: str) -> set[str]:
    return set(MERMAID_ER_TABLE_RE.findall(block))


# ---------------------------------------------------------------------------
# check
# ---------------------------------------------------------------------------
def cmd_check(args: argparse.Namespace) -> int:
    problems = 0
    drawio_files = sorted(UML_DIR.glob("*.drawio"))
    md_files = sorted(MERMAID_DIR.glob("*.md"))

    if not drawio_files:
        print("✗ no .drawio files found in docs/uml/")
        problems += 1
    if not md_files:
        print("✗ no mermaid/*.md files found")
        problems += 1

    # -- 1. draw.io structural validity --------------------------------
    print("── draw.io files ──")
    for f in drawio_files:
        root = parse_drawio(f)
        if root is None:
            problems += 1
            continue
        ids = {c.get("id") for c in drawio_cells(root)}
        bad = []
        for c in drawio_cells(root):
            for attr in ("source", "target", "parent"):
                ref = c.get(attr)
                # Cells "0" and "1" are the implicit root cells (id="0"/"1"
                # are present in every file, so the ids set already covers them).
                if ref and ref not in ids:
                    bad.append(f"{attr}={ref}")
        if bad:
            print(f"  ✗ {f.name}: dangling references {bad}")
            problems += 1
        elif args.verbose:
            print(f"  ✓ {f.name}")

    # -- 2. mermaid block extraction -----------------------------------
    print("── mermaid files ──")
    md_by_stem: dict[str, Path] = {}
    for f in md_files:
        text = f.read_text(encoding="utf-8")
        blocks = mermaid_blocks(text)
        if not blocks:
            print(f"  ✗ {f.name}: no ```mermaid block found")
            problems += 1
        elif len(blocks) > 1:
            print(f"  ✗ {f.name}: {len(blocks)} mermaid blocks (expected exactly 1)")
            problems += 1
        elif not blocks[0].strip():
            print(f"  ✗ {f.name}: mermaid block is empty")
            problems += 1
        elif args.verbose:
            print(f"  ✓ {f.name}: 1 mermaid block ({len(blocks[0])} chars)")
        stem = STEM_RE.match(f.stem)
        if stem:
            md_by_stem[stem.group(1)] = f

    # -- 3. coverage: every drawio has a mermaid twin and vice versa ----
    print("── coverage (drawio ↔ mermaid) ──")
    drawio_by_prefix: dict[str, Path] = {}
    for f in drawio_files:
        stem = STEM_RE.match(f.stem)
        if stem:
            drawio_by_prefix[stem.group(1)] = f
        else:
            print(f"  ! {f.name}: name does not match NN-name convention (skipped)")
    for prefix, f in sorted(drawio_by_prefix.items()):
        if prefix not in md_by_stem:
            print(f"  ✗ {f.name}: no matching mermaid/{prefix}-*.md")
            problems += 1
        elif args.verbose:
            print(f"  ✓ {f.name} ↔ {md_by_stem[prefix].name}")
    for prefix, f in sorted(md_by_stem.items()):
        if prefix not in drawio_by_prefix:
            print(f"  ✗ {f.name}: no matching {prefix}-*.drawio")
            problems += 1

    # -- 4. name consistency for class & ER diagrams -------------------
    print("── name consistency (classes / tables) ──")
    for prefix, drawio_f in sorted(drawio_by_prefix.items()):
        md_f = md_by_stem.get(prefix)
        root = parse_drawio(drawio_f)
        if root is None or md_f is None:
            continue
        text = md_f.read_text(encoding="utf-8")
        blocks = mermaid_blocks(text)
        if not blocks:
            continue
        block = blocks[0]

        if "class-diagram" in drawio_f.name:
            d = drawio_class_names(root)
            m = mermaid_class_names(block)
            kind = "classes"
        elif "er-diagram" in drawio_f.name:
            d = drawio_table_names(root)
            m = mermaid_er_table_names(block)
            kind = "tables"
        else:
            continue

        only_drawio = d - m
        only_mermaid = m - d
        if only_drawio or only_mermaid:
            problems += 1
            print(f"  ✗ {drawio_f.name}: {kind} mismatch")
            if only_drawio:
                print(f"      in draw.io only: {sorted(only_drawio)}")
            if only_mermaid:
                print(f"      in mermaid only: {sorted(only_mermaid)}")
        elif args.verbose:
            print(f"  ✓ {drawio_f.name}: {len(d)} {kind} match")

    if problems:
        print(f"\n✗ {problems} problem(s) found")
        return 1
    print("\n✓ all diagrams in sync")
    return 0


# ---------------------------------------------------------------------------
# preview
# ---------------------------------------------------------------------------
def _html_escape(text: str) -> str:
    return (
        text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
    )


def cmd_preview(args: argparse.Namespace) -> int:
    out = Path(args.out)
    md_files = sorted(MERMAID_DIR.glob("*.md"))
    if not md_files:
        print("✗ no mermaid/*.md files to preview")
        return 1

    sections: list[str] = []
    for f in md_files:
        text = f.read_text(encoding="utf-8")
        blocks = mermaid_blocks(text)
        if not blocks:
            continue
        title = f.stem.replace("-", " ").title()
        sections.append(
            f'<section><h2>{title}</h2>'
            f'<pre class="mermaid">{_html_escape(blocks[0])}</pre></section>'
        )

    html = f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>MeethybridHub — UML Diagrams</title>
<style>
  body {{ font-family: system-ui, sans-serif; margin: 2rem auto; max-width: 1200px; padding: 0 1rem; }}
  h1 {{ border-bottom: 2px solid #333; padding-bottom: .4rem; }}
  section {{ margin: 2.5rem 0; }}
  h2 {{ color: #333; }}
  .mermaid {{ display: block; text-align: center; }}
</style>
<script type="module">
  import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.esm.min.mjs';
  mermaid.initialize({{ startOnLoad: true, theme: 'default', securityLevel: 'loose' }});
</script>
</head>
<body>
<h1>MeethybridHub — UML Diagrams (preview)</h1>
<p>Rendered from <code>docs/uml/mermaid/*.md</code> · editable originals: <code>docs/uml/*.drawio</code></p>
{chr(10).join(sections)}
</body>
</html>
"""
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(html, encoding="utf-8")
    print(f"✓ wrote {out} ({len(html)} bytes) — open it in any browser")
    return 0


# ---------------------------------------------------------------------------
# entry point
# ---------------------------------------------------------------------------
def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        prog="sync_diagrams.py",
        description="Keep docs/uml draw.io and Mermaid diagrams in sync.",
    )
    sub = parser.add_subparsers(dest="command", required=True)

    p_check = sub.add_parser("check", help="validate structure and cross-format consistency")
    p_check.add_argument("--verbose", action="store_true", help="print per-file results")
    p_check.set_defaults(func=cmd_check)

    p_preview = sub.add_parser("preview", help="render a single preview.html of all Mermaid diagrams")
    p_preview.add_argument("--out", default=str(UML_DIR / "preview.html"), help="output path")
    p_preview.set_defaults(func=cmd_preview)

    args = parser.parse_args(argv)
    return args.func(args)


if __name__ == "__main__":
    sys.exit(main())
