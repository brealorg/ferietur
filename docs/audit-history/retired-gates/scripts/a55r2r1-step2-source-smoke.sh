#!/usr/bin/env bash
set -u
set -o pipefail
UI="app/src/main/java/app/ferietur/ui/FerieturApp.kt"
fail() { echo "A55R2R1_STEP2_SUCCESSOR_SMOKE=FAIL reason=$1"; exit 1; }
[[ -f "$UI" ]] || fail "FerieturApp_missing"
grep -Fq 'Hvordan skal turen lønnes?' "$UI" || fail "salary_heading_missing"
grep -Fq 'Vanlig lønn følger grunnturnusen. Appen beregner det som kommer i tillegg.' "$UI" || fail "turnus_help_missing"
grep -Fq 'Grunnturnusen holdes utenfor. Arbeidet på turen beregnes separat.' "$UI" || fail "separate_help_missing"
grep -Fq 'Hvem skal betalingsforslaget gjelde?' "$UI" || fail "payment_heading_missing"
grep -Fq 'Arbeidsgiver på turen' "$UI" || fail "stable_employer_title_missing"
grep -Fq 'Regler og beregningsgrunnlag' "$UI" || fail "rules_heading_missing"
! grep -Fq 'Arbeidsgiver på turen følger grunnturnusen.' "$UI" || fail "replacement_collision_regressed"
! grep -Fq 'Hvem er arbeidsgiveren din på turen?' "$UI" || fail "dynamic_employer_heading_still_present"
! grep -Fq 'for det som kommer i tillegg til grunnturnusen.' "$UI" || fail "turnus_dependent_payment_copy_still_present"
! grep -Fq 'for beløpet for turen.' "$UI" || fail "separate_dependent_payment_copy_still_present"
echo "A55R2R1_STEP2_SUCCESSOR_SMOKE=PASS"
