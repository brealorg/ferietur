package app.ferietur.domain

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TariffRuntimeRegistrationPlanTest {

    private val currentPackage =
        FerieturTariffs.dok25_2026_2028

    private val source =
        TariffUpdateSourceDocument(
            id = "synthetic-source",
            label = "Syntetisk registreringskilde",
            sourceUri =
                "https://example.invalid/runtime-registration.pdf",
            publishedOn =
                LocalDate.of(2027, 4, 20),
            sha256 = "c".repeat(64),
        )

    private val verification =
        TariffUpdateVerification(
            verifiedAt =
                LocalDateTime.of(
                    2027,
                    4,
                    25,
                    12,
                    0,
                ),
            sourceHashesVerified = true,
            notes = "Syntetisk A5A4-test.",
        )

    private fun salaryComponent(
        id: String,
        start: LocalDate,
        end: LocalDate,
    ) = TariffUpdateRegisteredComponent(
        kind =
            TariffUpdateComponentKind.SALARY_TABLE,
        id = id,
        tariffPackageId = currentPackage.id,
        effectiveFrom = start,
        effectiveThrough = end,
    )

    private fun activeManifest(
        component: TariffUpdateRegisteredComponent,
    ) = TariffUpdateManifest(
        updateId =
            "runtime-registration-test-${component.id}",
        tariffPackageId =
            component.tariffPackageId,
        rulesetVersion =
            currentPackage.rulesetVersion,
        effectiveFrom =
            component.effectiveFrom,
        effectiveThrough =
            component.effectiveThrough,
        components = listOf(
            TariffUpdateComponentRef(
                kind = component.kind,
                id = component.id,
                sourceDocumentIds =
                    listOf(source.id),
            ),
        ),
        sourceDocuments = listOf(source),
        verification = verification,
        activation =
            TariffUpdateActivation.ACTIVE,
    )

    private fun qualifiedSalary(
        component: TariffUpdateRegisteredComponent,
    ): TariffUpdateQualificationResult.Qualified {
        val candidateRegistry =
            TariffUpdateComponentRegistry(
                listOf(
                    TariffUpdateRegisteredComponents
                        .tariffPackage(currentPackage),
                    component,
                ),
            )

        return TariffUpdateQualifiedActivationGate
            .requireQualified(
                activeManifest(component),
                candidateRegistry,
            )
    }

    @Test
    fun contiguousFutureSalaryTableProducesAddOnlyPlan() {
        val nextSalary =
            salaryComponent(
                id = "salary-2027-next",
                start = LocalDate.of(2027, 5, 1),
                end = LocalDate.of(2028, 4, 30),
            )

        val current =
            FerieturTariffUpdateComponents
                .currentRuntime

        assertNull(
            current.component(
                TariffUpdateComponentKind.SALARY_TABLE,
                nextSalary.id,
            ),
        )

        val result =
            TariffRuntimeRegistrationPlanner.plan(
                qualifiedSalary(nextSalary),
                current,
            )

        assertTrue(
            result is
                TariffRuntimeRegistrationPlanResult.Planned,
        )

        val plan =
            (result as
                TariffRuntimeRegistrationPlanResult.Planned)
                .plan

        assertEquals(
            listOf(nextSalary),
            plan.additions,
        )

        assertTrue(plan.alreadyPresent.isEmpty())

        assertNull(
            current.component(
                TariffUpdateComponentKind.SALARY_TABLE,
                nextSalary.id,
            ),
        )
    }

    @Test
    fun salaryBoundaryGapFailsClosed() {
        val nextSalary =
            salaryComponent(
                id = "salary-gap",
                start = LocalDate.of(2027, 5, 2),
                end = LocalDate.of(2028, 4, 30),
            )

        val result =
            TariffRuntimeRegistrationPlanner.plan(
                qualifiedSalary(nextSalary),
                FerieturTariffUpdateComponents.currentRuntime,
            )

        assertTrue(
            result is
                TariffRuntimeRegistrationPlanResult.Rejected,
        )

        val rejected =
            result as
                TariffRuntimeRegistrationPlanResult.Rejected

        assertTrue(
            rejected.issues.any {
                it.code ==
                    TariffRuntimeRegistrationIssueCode
                        .COMPONENT_PERIOD_GAP
            },
        )
    }

    @Test
    fun salaryBoundaryOverlapFailsClosed() {
        val nextSalary =
            salaryComponent(
                id = "salary-overlap",
                start = LocalDate.of(2027, 4, 30),
                end = LocalDate.of(2028, 4, 30),
            )

        val result =
            TariffRuntimeRegistrationPlanner.plan(
                qualifiedSalary(nextSalary),
                FerieturTariffUpdateComponents.currentRuntime,
            )

        assertTrue(
            result is
                TariffRuntimeRegistrationPlanResult.Rejected,
        )

        val rejected =
            result as
                TariffRuntimeRegistrationPlanResult.Rejected

        assertTrue(
            rejected.issues.any {
                it.code ==
                    TariffRuntimeRegistrationIssueCode
                        .COMPONENT_PERIOD_OVERLAP
            },
        )
    }

    @Test
    fun existingImmutableIdCannotBeRedefined() {
        val modifiedExisting =
            salaryComponent(
                id = OsloSalaryTable2026.tableId,
                start =
                    OsloSalaryTable2026.effectiveFromDate,
                end =
                    OsloSalaryTable2026
                        .verifiedThroughDate
                        .minusDays(1),
            )

        val result =
            TariffRuntimeRegistrationPlanner.plan(
                qualifiedSalary(modifiedExisting),
                FerieturTariffUpdateComponents.currentRuntime,
            )

        assertTrue(
            result is
                TariffRuntimeRegistrationPlanResult.Rejected,
        )

        val rejected =
            result as
                TariffRuntimeRegistrationPlanResult.Rejected

        assertTrue(
            rejected.issues.any {
                it.code ==
                    TariffRuntimeRegistrationIssueCode
                        .IMMUTABLE_COMPONENT_REDEFINITION
            },
        )
    }

    @Test
    fun registrationContainingOnlyExistingComponentIsRejectedAsNoOp() {
        val current =
            FerieturTariffUpdateComponents.currentRuntime

        val existingSalary =
            requireNotNull(
                current.component(
                    TariffUpdateComponentKind.SALARY_TABLE,
                    OsloSalaryTable2026.tableId,
                ),
            )

        val qualified =
            TariffUpdateQualifiedActivationGate
                .requireQualified(
                    activeManifest(existingSalary),
                    current,
                )

        val result =
            TariffRuntimeRegistrationPlanner.plan(
                qualified,
                current,
            )

        assertTrue(
            result is
                TariffRuntimeRegistrationPlanResult.Rejected,
        )

        val rejected =
            result as
                TariffRuntimeRegistrationPlanResult.Rejected

        assertTrue(
            rejected.issues.any {
                it.code ==
                    TariffRuntimeRegistrationIssueCode.NO_NEW_COMPONENTS
            },
        )
    }

    @Test
    fun newTariffPackageCannotBeRegisteredPartially() {
        val newPackage =
            TariffUpdateRegisteredComponent(
                kind =
                    TariffUpdateComponentKind.TARIFF_PACKAGE,
                id = "oslo-dok25-2028-2030",
                tariffPackageId =
                    "oslo-dok25-2028-2030",
                effectiveFrom =
                    LocalDate.of(2028, 5, 1),
                effectiveThrough =
                    LocalDate.of(2030, 4, 30),
                rulesetVersion = "2028.1",
            )

        val candidateRegistry =
            TariffUpdateComponentRegistry(
                listOf(newPackage),
            )

        val manifest =
            TariffUpdateManifest(
                updateId = "partial-new-package",
                tariffPackageId = newPackage.id,
                rulesetVersion =
                    requireNotNull(
                        newPackage.rulesetVersion,
                    ),
                effectiveFrom =
                    newPackage.effectiveFrom,
                effectiveThrough =
                    newPackage.effectiveThrough,
                components = listOf(
                    TariffUpdateComponentRef(
                        kind = newPackage.kind,
                        id = newPackage.id,
                        sourceDocumentIds =
                            listOf(source.id),
                    ),
                ),
                sourceDocuments = listOf(source),
                verification = verification,
                activation =
                    TariffUpdateActivation.ACTIVE,
            )

        val qualified =
            TariffUpdateQualifiedActivationGate
                .requireQualified(
                    manifest,
                    candidateRegistry,
                )

        val result =
            TariffRuntimeRegistrationPlanner.plan(
                qualified,
                FerieturTariffUpdateComponents.currentRuntime,
            )

        assertTrue(
            result is
                TariffRuntimeRegistrationPlanResult.Rejected,
        )

        val rejected =
            result as
                TariffRuntimeRegistrationPlanResult.Rejected

        assertTrue(
            rejected.issues.any {
                it.code ==
                    TariffRuntimeRegistrationIssueCode
                        .NEW_PACKAGE_INCOMPLETE
            },
        )
    }

    @Test
    fun requirePlanFailsClosedWhenGapExists() {
        val nextSalary =
            salaryComponent(
                id = "salary-gap-require",
                start = LocalDate.of(2027, 5, 2),
                end = LocalDate.of(2028, 4, 30),
            )

        assertThrows(
            IllegalArgumentException::class.java,
        ) {
            TariffRuntimeRegistrationPlanner.requirePlan(
                qualifiedSalary(nextSalary),
                FerieturTariffUpdateComponents.currentRuntime,
            )
        }
    }

    @Test
    fun projectedPlanContainsCurrentAndNewComponentsWithoutMutation() {
        val nextSalary =
            salaryComponent(
                id = "salary-projection",
                start = LocalDate.of(2027, 5, 1),
                end = LocalDate.of(2028, 4, 30),
            )

        val current =
            FerieturTariffUpdateComponents.currentRuntime

        val originalSize =
            current.all().size

        val plan =
            TariffRuntimeRegistrationPlanner.requirePlan(
                qualifiedSalary(nextSalary),
                current,
            )

        assertEquals(
            originalSize + 1,
            plan.projectedComponents.size,
        )

        assertEquals(
            originalSize,
            current.all().size,
        )

        assertFalse(
            current.all().any {
                it.id == nextSalary.id
            },
        )

        assertTrue(
            plan.projectedComponents.any {
                it.id == nextSalary.id
            },
        )
    }
}
