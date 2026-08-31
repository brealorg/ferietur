package app.ferietur.domain

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TariffUpdateQualificationTest {

    private val tariffPackage =
        FerieturTariffs.dok25_2026_2028

    private val source =
        TariffUpdateSourceDocument(
            id = "salary-source",
            label = "Syntetisk kontrollkilde",
            sourceUri =
                "https://example.invalid/salary.pdf",
            publishedOn =
                LocalDate.of(2026, 4, 20),
            sha256 = "b".repeat(64),
        )

    private val verification =
        TariffUpdateVerification(
            verifiedAt =
                LocalDateTime.of(
                    2026,
                    4,
                    25,
                    12,
                    0,
                ),
            sourceHashesVerified = true,
            notes = "Syntetisk A5A3-test.",
        )

    private fun manifest(
        activation: TariffUpdateActivation =
            TariffUpdateActivation.ACTIVE,
        rulesetVersion: String =
            tariffPackage.rulesetVersion,
        componentId: String =
            OsloSalaryTable2026.tableId,
        sourceHash: String =
            source.sha256,
    ): TariffUpdateManifest {
        val effectiveSource =
            source.copy(sha256 = sourceHash)

        return TariffUpdateManifest(
            updateId = "qualified-current-salary",
            tariffPackageId =
                tariffPackage.id,
            rulesetVersion =
                rulesetVersion,
            effectiveFrom =
                OsloSalaryTable2026.effectiveFromDate,
            effectiveThrough =
                OsloSalaryTable2026.verifiedThroughDate,
            components = listOf(
                TariffUpdateComponentRef(
                    kind =
                        TariffUpdateComponentKind
                            .SALARY_TABLE,
                    id = componentId,
                    sourceDocumentIds =
                        listOf(effectiveSource.id),
                ),
            ),
            sourceDocuments =
                listOf(effectiveSource),
            verification = verification,
            activation = activation,
        )
    }

    @Test
    fun fullyValidatedActiveManifestQualifies() {
        val manifest = manifest()

        val result =
            TariffUpdateQualifiedActivationGate.qualify(
                manifest,
                FerieturTariffUpdateComponents
                    .currentRuntime,
            )

        assertTrue(
            result is
                TariffUpdateQualificationResult
                    .Qualified,
        )

        val qualified =
            result as
                TariffUpdateQualificationResult
                    .Qualified

        assertEquals(
            manifest,
            qualified.manifest,
        )

        assertEquals(
            listOf(OsloSalaryTable2026.tableId),
            qualified.components.map { it.id },
        )
    }

    @Test
    fun candidateNeverQualifiesForRuntimeRegistration() {
        val candidate = manifest(
            activation =
                TariffUpdateActivation.CANDIDATE,
        )

        val result =
            TariffUpdateQualifiedActivationGate.qualify(
                candidate,
                FerieturTariffUpdateComponents
                    .currentRuntime,
            )

        assertTrue(
            result is
                TariffUpdateQualificationResult
                    .Rejected,
        )

        val rejected =
            result as
                TariffUpdateQualificationResult
                    .Rejected

        assertTrue(
            rejected.issues.any {
                it ==
                    TariffUpdateQualificationIssue
                        .ManifestNotActive
            },
        )
    }

    @Test
    fun invalidSourceFingerprintPreventsQualification() {
        val invalid = manifest(
            sourceHash = "not-a-sha256",
        )

        val result =
            TariffUpdateQualifiedActivationGate.qualify(
                invalid,
                FerieturTariffUpdateComponents
                    .currentRuntime,
            )

        assertTrue(
            result is
                TariffUpdateQualificationResult
                    .Rejected,
        )

        val rejected =
            result as
                TariffUpdateQualificationResult
                    .Rejected

        assertTrue(
            rejected.issues.any { issue ->
                issue is
                    TariffUpdateQualificationIssue
                        .ManifestValidation &&
                    issue.issue.code ==
                    TariffUpdateValidationCode
                        .INVALID_SOURCE_SHA256
            },
        )
    }

    @Test
    fun wrongRulesetPreventsQualification() {
        val invalid = manifest(
            rulesetVersion = "wrong-ruleset",
        )

        val result =
            TariffUpdateQualifiedActivationGate.qualify(
                invalid,
                FerieturTariffUpdateComponents
                    .currentRuntime,
            )

        assertTrue(
            result is
                TariffUpdateQualificationResult
                    .Rejected,
        )

        val rejected =
            result as
                TariffUpdateQualificationResult
                    .Rejected

        assertTrue(
            rejected.issues.any { issue ->
                issue is
                    TariffUpdateQualificationIssue
                        .ComponentCoherence &&
                    issue.issue.code ==
                    TariffUpdateCoherenceCode
                        .RULESET_VERSION_MISMATCH
            },
        )
    }

    @Test
    fun unknownComponentPreventsQualification() {
        val invalid = manifest(
            componentId = "missing-salary-table",
        )

        val result =
            TariffUpdateQualifiedActivationGate.qualify(
                invalid,
                FerieturTariffUpdateComponents
                    .currentRuntime,
            )

        assertTrue(
            result is
                TariffUpdateQualificationResult
                    .Rejected,
        )

        val rejected =
            result as
                TariffUpdateQualificationResult
                    .Rejected

        assertTrue(
            rejected.issues.any { issue ->
                issue is
                    TariffUpdateQualificationIssue
                        .ComponentCoherence &&
                    issue.issue.code ==
                    TariffUpdateCoherenceCode
                        .UNKNOWN_COMPONENT
            },
        )
    }

    @Test
    fun requireQualifiedReturnsTypedQualifiedResult() {
        val result =
            TariffUpdateQualifiedActivationGate
                .requireQualified(
                    manifest(),
                    FerieturTariffUpdateComponents
                        .currentRuntime,
                )

        assertEquals(
            OsloSalaryTable2026.tableId,
            result.components.single().id,
        )
    }

    @Test
    fun requireQualifiedFailsClosedForCandidate() {
        assertThrows(
            IllegalArgumentException::class.java,
        ) {
            TariffUpdateQualifiedActivationGate
                .requireQualified(
                    manifest(
                        activation =
                            TariffUpdateActivation
                                .CANDIDATE,
                    ),
                    FerieturTariffUpdateComponents
                        .currentRuntime,
                )
        }
    }
}
