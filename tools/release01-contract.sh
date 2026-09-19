#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

fail() {
    echo "RELEASE01_CONTRACT=FAIL reason=$1"
    exit 1
}

for f in \
    README.md \
    SOURCE-SHA256SUMS.txt \
    docs/ADR01_DEPENDENCY_DECISIONS.md \
    docs/RELEASE01_CANONICAL_RELEASE.md \
    app/src/main/res/values/launcher_colors.xml \
    app/src/main/res/drawable/ic_launcher_foreground.xml \
    app/src/main/res/drawable/ic_launcher_monochrome.xml \
    app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml \
    app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml \
    app/src/main/res/mipmap-anydpi-v33/ic_launcher.xml \
    app/src/main/res/mipmap-anydpi-v33/ic_launcher_round.xml \
    tools/generate-source-sha256.sh \
    tools/verify-source-sha256.sh
 do
    [[ -f "$f" ]] || fail "missing_${f//\//_}"
 done

MANIFEST='app/src/main/AndroidManifest.xml'
GRADLE='app/build.gradle.kts'

# GATE03: README is now a public product page and the version moves with each release.
# The former assertions on internal README status wording (CA-012/CA-010/schema v6/PLAY01) and
# on a pinned versionCode/versionName were retired; release/current.env records versions.
grep -qx '# Ferietur' README.md || fail 'README_not_canonical'
! grep -Fq 'A5.4R1' README.md || fail 'README_stale_A54R1'

grep -Fq 'android:icon="@mipmap/ic_launcher"' "$MANIFEST" || fail 'application_icon_missing'
grep -Fq 'android:roundIcon="@mipmap/ic_launcher_round"' "$MANIFEST" || fail 'round_icon_missing'
grep -Fq '<monochrome android:drawable="@drawable/ic_launcher_monochrome" />' app/src/main/res/mipmap-anydpi-v33/ic_launcher.xml || fail 'monochrome_launcher_missing'
grep -Fq '#F9C66B' app/src/main/res/values/launcher_colors.xml || fail 'Oslo_yellow_launcher_background_missing'
grep -Fq '#2A2859' app/src/main/res/values/launcher_colors.xml || fail 'Oslo_dark_blue_launcher_foreground_missing'

grep -Fq 'implementation("androidx.core:core-ktx:1.18.0")' "$GRADLE" || fail 'core_ktx_1_18_direct_alignment_missing'
! grep -Fq 'androidx.core:core-ktx:1.17.0' "$GRADLE" || fail 'stale_core_ktx_1_17_remains'
grep -Fq 'material3:1.5.0-alpha26' "$GRADLE" || fail 'Material3_alpha_override_missing'
grep -Fq 'Material3 alpha — CA-016' docs/ADR01_DEPENDENCY_DECISIONS.md || fail 'CA016_ADR_missing'
grep -Fq 'App Bundle — CA-014' docs/ADR01_DEPENDENCY_DECISIONS.md || fail 'CA014_distribution_decision_missing'

"$ROOT/tools/verify-source-sha256.sh"

printf '%s\n' \
  'CA012_STAGE_B_CANONICAL_README=PASS' \
  'CA012_STAGE_B_SOURCE_MANIFEST=PASS' \
  'CA013_ADAPTIVE_LAUNCHER_ICON=PASS' \
  'CA013_ROUND_ICON=PASS' \
  'CA013_MONOCHROME_ICON=PASS' \
  'CA015_CORE_KTX_DIRECT_ALIGNMENT=PASS' \
  'CA016_MATERIAL3_ALPHA_DECISION=RECORDED' \
  'CA014_DIRECT_APK_SCOPE=RECORDED' \
  'RELEASE01_CONTRACT=PASS'
