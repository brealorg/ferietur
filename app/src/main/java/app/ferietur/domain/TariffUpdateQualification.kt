package app.ferietur.domain

/**
 * Failure produced by the final control-plane qualification gate.
 *
 * Qualification means that an update is eligible for a later, separately
 * qualified runtime-registration step. It does not mutate or activate any
 * runtime tariff catalog.
 */
sealed interface TariffUpdateQualificationIssue {
    data object ManifestNotActive :
        TariffUpdateQualificationIssue

    data class ManifestValidation(
        val issue: TariffUpdateValidationIssue,
    ) : TariffUpdateQualificationIssue

    data class ComponentCoherence(
        val issue: TariffUpdateCoherenceIssue,
    ) : TariffUpdateQualificationIssue
}

sealed interface TariffUpdateQualificationResult {
    /**
     * The manifest passed every currently defined control-plane gate.
     *
     * This is an approval to proceed to runtime registration work, not proof
     * that runtime registration has happened.
     */
    data class Qualified(
        val manifest: TariffUpdateManifest,
        val components: List<TariffUpdateRegisteredComponent>,
    ) : TariffUpdateQualificationResult

    data class Rejected(
        val manifest: TariffUpdateManifest,
        val issues: List<TariffUpdateQualificationIssue>,
    ) : TariffUpdateQualificationResult
}

/**
 * Final A5 control-plane gate before any future runtime registration.
 *
 * An update qualifies only when:
 *  - it is explicitly ACTIVE;
 *  - A5A1 structural/provenance validation passes;
 *  - A5A2 component identity/package/date coherence passes.
 *
 * No runtime catalog is read through or mutated by this gate.
 */
object TariffUpdateQualifiedActivationGate {

    fun qualify(
        manifest: TariffUpdateManifest,
        registry: TariffUpdateComponentRegistry,
    ): TariffUpdateQualificationResult {
        val issues =
            mutableListOf<TariffUpdateQualificationIssue>()

        if (
            manifest.activation !=
            TariffUpdateActivation.ACTIVE
        ) {
            issues +=
                TariffUpdateQualificationIssue.ManifestNotActive
        }

        TariffUpdateManifestValidator
            .validate(manifest)
            .forEach { issue ->
                issues +=
                    TariffUpdateQualificationIssue
                        .ManifestValidation(issue)
            }

        TariffUpdateComponentCoherenceValidator
            .validate(manifest, registry)
            .forEach { issue ->
                issues +=
                    TariffUpdateQualificationIssue
                        .ComponentCoherence(issue)
            }

        if (issues.isNotEmpty()) {
            return TariffUpdateQualificationResult.Rejected(
                manifest = manifest,
                issues = issues.toList(),
            )
        }

        val components = manifest.components.map { reference ->
            requireNotNull(
                registry.component(
                    reference.kind,
                    reference.id,
                ),
            ) {
                "Koherensvalidatoren godkjente en komponent som " +
                    "ikke finnes: ${reference.kind}/${reference.id}"
            }
        }

        return TariffUpdateQualificationResult.Qualified(
            manifest = manifest,
            components = components,
        )
    }

    fun requireQualified(
        manifest: TariffUpdateManifest,
        registry: TariffUpdateComponentRegistry,
    ): TariffUpdateQualificationResult.Qualified =
        when (
            val result = qualify(
                manifest,
                registry,
            )
        ) {
            is TariffUpdateQualificationResult.Qualified ->
                result

            is TariffUpdateQualificationResult.Rejected ->
                throw IllegalArgumentException(
                    "Tariffoppdateringen ${manifest.updateId} er ikke " +
                        "kvalifisert for runtime-registrering: " +
                        result.issues.joinToString("; ") {
                            qualificationIssueText(it)
                        },
                )
        }

    private fun qualificationIssueText(
        issue: TariffUpdateQualificationIssue,
    ): String =
        when (issue) {
            TariffUpdateQualificationIssue.ManifestNotActive ->
                "MANIFEST_NOT_ACTIVE"

            is TariffUpdateQualificationIssue.ManifestValidation ->
                "MANIFEST_${issue.issue.code}"

            is TariffUpdateQualificationIssue.ComponentCoherence ->
                "COHERENCE_${issue.issue.code}"
        }
}
