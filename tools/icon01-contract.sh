#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

fail() {
    echo "ICON01_CONTRACT=FAIL reason=$1"
    exit 1
}

COLORS='app/src/main/res/values/launcher_colors.xml'
FG='app/src/main/res/drawable/ic_launcher_foreground.xml'
MONO='app/src/main/res/drawable/ic_launcher_monochrome.xml'

for f in \
    "$COLORS" \
    "$FG" \
    "$MONO" \
    app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml \
    app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml \
    app/src/main/res/mipmap-anydpi-v33/ic_launcher.xml \
    app/src/main/res/mipmap-anydpi-v33/ic_launcher_round.xml \
    docs/ICON01_LAUNCHER_POLISH.md
 do
    [[ -f "$f" ]] || fail "missing_${f//\//_}"
 done

grep -Fq '<color name="launcher_icon_background">#2A2859</color>' "$COLORS" || fail 'dark_blue_background_not_exact'
grep -Fq '<color name="launcher_icon_foreground">#F9C66B</color>' "$COLORS" || fail 'Oslo_yellow_foreground_not_exact'

for marker in \
    'android:name="suitcase_handle"' \
    'android:name="suitcase_left_shell"' \
    'android:name="suitcase_right_shell"' \
    'android:name="suitcase_body"' \
    'android:name="route_start"' \
    'android:name="route_dash_1"' \
    'android:name="route_dash_2"' \
    'android:name="route_dash_3"' \
    'android:name="route_pin"' \
    'android:name="route_pin_hole"'
 do
    grep -Fq "$marker" "$FG" || fail "foreground_marker_missing_${marker//[^A-Za-z0-9]/_}"
 done

[[ "$(grep -Fc 'android:strokeLineCap="round"' "$FG")" -eq 3 ]] || fail 'route_dash_count_not_three'
grep -Fq 'android:name="monochrome_body_with_pin_cutout"' "$MONO" || fail 'monochrome_pin_cutout_missing'
grep -Fq 'android:fillType="evenOdd"' "$MONO" || fail 'monochrome_evenodd_missing'
grep -Fq '<monochrome android:drawable="@drawable/ic_launcher_monochrome" />' app/src/main/res/mipmap-anydpi-v33/ic_launcher.xml || fail 'v33_monochrome_wiring_missing'
grep -Fq '<monochrome android:drawable="@drawable/ic_launcher_monochrome" />' app/src/main/res/mipmap-anydpi-v33/ic_launcher_round.xml || fail 'v33_round_monochrome_wiring_missing'

grep -Fq 'versionCode = 50' app/build.gradle.kts || fail 'versionCode_changed'
grep -Fq 'versionName = "0.5.4-rc1"' app/build.gradle.kts || fail 'versionName_changed'
grep -Fq 'route from a start point to a location pin' docs/ICON01_LAUNCHER_POLISH.md || fail 'design_decision_not_documented'
grep -Fq 'deterministic Android `VectorDrawable` resources' docs/ICON01_LAUNCHER_POLISH.md || fail 'vector_asset_decision_not_documented'

printf '%s\n' \
  'ICON01_BACKGROUND=OSLO_DARK_BLUE_2A2859' \
  'ICON01_FOREGROUND=OSLO_YELLOW_F9C66B' \
  'ICON01_SUITCASE_ROUTE_PIN=PASS' \
  'ICON01_MONOCHROME_PIN_SILHOUETTE=PASS' \
  'ICON01_VERSION_IDENTITY=RC1_UNCHANGED' \
  'ICON01_CONTRACT=PASS'
