#!/usr/bin/env bash
# ICON02: semantic gate for the luggage-tag launcher icon. Stand-alone; supersedes the retired
# ICON01 gate for the facts that changed (colours, motif, safe-zone scaling).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

fail() {
    echo "ICON02_CONTRACT=FAIL reason=$1"
    exit 1
}

COLORS='app/src/main/res/values/launcher_colors.xml'
FG='app/src/main/res/drawable/ic_launcher_foreground.xml'
MONO='app/src/main/res/drawable/ic_launcher_monochrome.xml'

for f in "$COLORS" "$FG" "$MONO" docs/ICON02_LUGGAGE_TAG.md; do
    [ -f "$f" ] || fail "missing_${f//[^A-Za-z0-9]/_}"
done

grep -Fq '<color name="launcher_icon_background">#F9C66B</color>' "$COLORS" || fail 'oslo_yellow_background_not_exact'
grep -Fq '<color name="launcher_icon_foreground">#2A2859</color>' "$COLORS" || fail 'oslo_dark_blue_suitcase_not_exact'
grep -Fq '<color name="launcher_icon_tag">#6FE9FF</color>' "$COLORS" || fail 'oslo_blue_tag_not_exact'
grep -Fq '<color name="launcher_icon_tag_string">#F8F0DD</color>' "$COLORS" || fail 'oslo_light_beige_string_not_exact'

for marker in 'android:name="suitcase_body"' 'android:name="luggage_tag"' 'android:name="luggage_tag_label"' 'android:name="luggage_tag_hole"'; do
    grep -Fq "$marker" "$FG" || fail "foreground_marker_missing_${marker//[^A-Za-z0-9]/_}"
done
if grep -q 'route_\|_pin' "$FG" "$MONO"; then fail 'icon01_route_or_pin_still_present'; fi

grep -Fq 'android:name="monochrome_body_with_tag_cutout"' "$MONO" || fail 'monochrome_tag_cutout_missing'
grep -Fq 'android:fillType="evenOdd"' "$MONO" || fail 'monochrome_evenodd_missing'

# Safe zone: both layers must carry the same scaling group.
for f in "$FG" "$MONO"; do
    grep -Fq 'android:scaleX="0.85"' "$f" || fail "safe_zone_scale_missing_${f//[^A-Za-z0-9]/_}"
    grep -Fq 'android:translateY="-3.5"' "$f" || fail "safe_zone_centering_missing_${f//[^A-Za-z0-9]/_}"
done

# BRAND01: the in-app mark and the PDF mark reuse the launcher artwork instead of redrawing it.
MARK='app/src/main/res/drawable/ic_ferietur_mark.xml'
PDFMARK='app/src/main/java/app/ferietur/export/PdfBrandMark.kt'
UI='app/src/main/java/app/ferietur/ui/FerieturApp.kt'
[ -f "$MARK" ] || fail 'in_app_mark_missing'
[ -f "$PDFMARK" ] || fail 'pdf_mark_missing'
python3 - "$FG" "$MARK" <<'PY' || fail 'in_app_mark_paths_differ_from_launcher_foreground'
import re, sys
def paths(f):
    text = open(f, encoding='utf-8').read()
    return {n: d for n, d in re.findall(r'android:name="([^"]+)"[^>]*?android:pathData="([^"]+)"', text, re.S)}
fg, mark = paths(sys.argv[1]), paths(sys.argv[2])
sys.exit(0 if fg and all(mark.get(name) == data for name, data in fg.items()) else 1)
PY
grep -Fq 'painterResource(R.drawable.ic_ferietur_mark)' "$UI" || fail 'in_app_mark_not_used'
if grep -Fq 'OsloIdentityShapes' "$UI"; then fail 'oslo_pattern_shapes_still_in_home_header'; fi
grep -Fq 'PdfBrandMark.draw(' app/src/main/java/app/ferietur/export/PdfExporter.kt || fail 'pdf_header_mark_not_drawn'
grep -Fq 'canvas.rotate(14f, 67f, 40f)' "$PDFMARK" || fail 'pdf_mark_tag_rotation_differs_from_drawable'
grep -Fq 'canvas.scale(0.85f, 0.85f, 54f, 57.5f)' "$PDFMARK" || fail 'pdf_mark_scale_differs_from_drawable'

# No raster launcher assets may appear next to the vectors.
if find app/src/main/res -path '*mipmap*' \( -name '*.png' -o -name '*.webp' \) | grep -q .; then
    fail 'raster_launcher_asset_present'
fi

printf '%s\n' \
  'ICON02_BACKGROUND=OSLO_YELLOW_F9C66B' \
  'ICON02_SUITCASE=OSLO_DARK_BLUE_2A2859' \
  'ICON02_LUGGAGE_TAG=OSLO_BLUE_6FE9FF' \
  'ICON02_SAFE_ZONE_SCALE=0.85' \
  'ICON02_MONOCHROME_TAG_CUTOUT=PASS' \
  'BRAND01_IN_APP_AND_PDF_MARK=PASS' \
  'ICON02_CONTRACT=PASS'
