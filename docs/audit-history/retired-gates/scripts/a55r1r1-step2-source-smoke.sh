#!/usr/bin/env bash
set -u
set -o pipefail
UI="app/src/main/java/app/ferietur/ui/FerieturApp.kt"
fail() { echo "A55R1R1_STEP2_SUCCESSOR_SMOKE=FAIL reason=$1"; exit 1; }
[[ -f "$UI" ]] || fail "FerieturApp_missing"
grep -Fq 'Hvordan skal turen lønnes?' "$UI" || fail "section1_heading_missing"
grep -Fq 'Vanlig turnus beholdes' "$UI" || fail "turnus_option_missing"
grep -Fq 'Grunnturnusen brukes som sammenligningsgrunnlag.' "$UI" || fail "turnus_summary_missing"
grep -Fq 'Hele turen beregnes separat' "$UI" || fail "separate_option_missing"
grep -Fq 'Grunnturnusen inngår ikke i beregningen.' "$UI" || fail "separate_summary_missing"
grep -Fq 'Hvem skal betalingsforslaget gjelde?' "$UI" || fail "payment_heading_missing"
grep -Fq 'Om betalingsforslaget' "$UI" || fail "payment_info_missing"
grep -Fq 'Regler og beregningsgrunnlag' "$UI" || fail "rules_heading_missing"
! grep -Fq 'Hva skjer med den vanlige lønnen din?' "$UI" || fail "old_section1_heading_still_present"
! grep -Fq 'Hvem skal betalingsforslaget settes opp mot?' "$UI" || fail "old_payment_heading_still_present"
! grep -Fq 'Arbeidsgiver: Oslo kommune' "$UI" || fail "old_employer_heading_still_present"
! grep -Fq 'Svar på hvordan turen faktisk skal løses.' "$UI" || fail "old_intro_still_present"
echo "A55R1R1_STEP2_SUCCESSOR_SMOKE=PASS"
