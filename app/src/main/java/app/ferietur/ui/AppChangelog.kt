package app.ferietur.ui

internal enum class ChangeSeverity {
    NORMAL,
    IMPORTANT,
    CALCULATION_CHANGE,
}

internal data class AppChange(
    val severity: ChangeSeverity,
    val title: String,
    val detail: String,
)

internal data class AppReleaseNotes(
    val versionCode: Int,
    val versionName: String,
    val intro: String,
    val changes: List<AppChange>,
)

internal enum class UpdatePromptAction {
    NONE,
    ACKNOWLEDGE_SILENTLY,
    SHOW_CHANGELOG,
}

internal object AppChangelog {
    val releases: List<AppReleaseNotes> = listOf(
        AppReleaseNotes(
            versionCode = 55,
            versionName = "0.6.1",
            intro =
                "Denne versjonen endrer ingen beregninger. Den gjør appen tryggere og mindre, " +
                    "og tydeliggjør hvordan klokkeslett tolkes.",
            changes = listOf(
                AppChange(
                    severity = ChangeSeverity.IMPORTANT,
                    title = "Alle klokkeslett er norsk tid",
                    detail =
                        "Ferietur regner med klokkeslettene slik du registrerer dem. Er reisemålet " +
                            "i en annen tidssone, regner du om til norsk tid. Krysser en arbeidsperiode " +
                            "overgangen til sommertid eller normaltid, viser kontrollen nå registrert " +
                            "og faktisk varighet.",
                ),
                AppChange(
                    severity = ChangeSeverity.NORMAL,
                    title = "Tryggere import og lagring",
                    detail =
                        "Sikkerhetskopier kontrolleres strengere før de importeres, og en forbigående " +
                            "lesefeil kan ikke lenger føre til at en tur erstattes av en eldre kopi.",
                ),
                AppChange(
                    severity = ChangeSeverity.NORMAL,
                    title = "Ryddigere PDF-eksport",
                    detail =
                        "Eksporterte PDF-er fjernes fra appens mellomlager etter ett døgn, og deling " +
                            "til andre apper er mer robust.",
                ),
                AppChange(
                    severity = ChangeSeverity.NORMAL,
                    title = "Mindre app og åpen kildekode",
                    detail =
                        "Appen er betydelig mindre å laste ned. Ferietur er nå fri programvare under " +
                            "GPL-3.0, slik at beregningene kan etterprøves av alle.",
                ),
            ),
        ),
        AppReleaseNotes(
            versionCode = 54,
            versionName = "0.6.0",
            intro =
                "Beregningen er forbedret i denne versjonen. Kontroller planbasis, " +
                    "arbeid/reise og lønnsopplysninger før du bruker resultatet.",
            changes = listOf(
                AppChange(
                    severity = ChangeSeverity.CALCULATION_CHANGE,
                    title = "Ny planbasis for ferieopphold",
                    detail =
                        "Ferietur skiller nå mellom vanlig grunnturnus og en egen arbeidsplan " +
                            "fastsatt av arbeidsgiver. Valget brukes som sammenligningsgrunnlag.",
                ),
                AppChange(
                    severity = ChangeSeverity.CALCULATION_CHANGE,
                    title = "Mer presis arbeidstid og reise",
                    detail =
                        "Reise skiller mellom på vakt og ikke på vakt, hvilende nattevakt " +
                            "håndteres separat, og arbeid utenfor avtalt tid vurderes mot riktig planbasis.",
                ),
                AppChange(
                    severity = ChangeSeverity.IMPORTANT,
                    title = "Tydeligere kontroll og dokumentasjon",
                    detail =
                        "Arbeid utover avtalt tid vises tydelig. Ferdigstilte beregninger og PDF-er " +
                            "lagrer planbasis og eventuell arbeidsgiverplan for senere kontroll.",
                ),
                AppChange(
                    severity = ChangeSeverity.NORMAL,
                    title = "Lokal sikkerhetskopi",
                    detail =
                        "Alle turer kan eksporteres til én lokal .ferietur-fil og gjenopprettes " +
                            "på enheten.",
                ),
            ),
        ),
    )

    fun releasesAfter(
        lastAcknowledgedVersionCode: Int,
        currentVersionCode: Int,
    ): List<AppReleaseNotes> =
        releases
            .filter { release ->
                release.versionCode > lastAcknowledgedVersionCode &&
                    release.versionCode <= currentVersionCode
            }
            .sortedBy { it.versionCode }

    fun releaseForVersion(versionCode: Int): AppReleaseNotes? =
        releases.singleOrNull { it.versionCode == versionCode }

    fun updatePromptAction(
        disclaimerAcknowledgementVersion: Int,
        lastAcknowledgedVersionCode: Int,
        currentVersionCode: Int,
    ): UpdatePromptAction {
        if (lastAcknowledgedVersionCode >= currentVersionCode) {
            return UpdatePromptAction.NONE
        }

        // Existing Ferietur users have already passed the mandatory disclaimer.
        // On a true fresh install both acknowledgement values start at zero:
        // seed the current version without showing release notes as an "update".
        if (disclaimerAcknowledgementVersion <= 0) {
            return UpdatePromptAction.ACKNOWLEDGE_SILENTLY
        }

        return if (
            releasesAfter(
                lastAcknowledgedVersionCode = lastAcknowledgedVersionCode,
                currentVersionCode = currentVersionCode,
            ).isNotEmpty()
        ) {
            UpdatePromptAction.SHOW_CHANGELOG
        } else {
            UpdatePromptAction.NONE
        }
    }
}
