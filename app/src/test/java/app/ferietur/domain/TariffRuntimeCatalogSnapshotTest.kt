package app.ferietur.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TariffRuntimeCatalogSnapshotTest {

    private val currentPackage =
        FerieturTariffs.dok25_2026_2028

    private val boundary =
        LocalDate.of(2027, 5, 1)

    private val source =
        TariffUpdateSourceDocument(
            id = "snapshot-source",
            label = "Syntetisk snapshot-kilde",
            sourceUri =
                "https://example.invalid/snapshot.pdf",
            publishedOn =
                LocalDate.of(2027, 4, 20),
            sha256 = "d".repeat(64),
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
            notes = "Syntetisk A5A5-test.",
        )

    private fun nextDescriptor(
        id: String = "salary-2027-snapshot",
        start: LocalDate = boundary,
        end: LocalDate =
            LocalDate.of(2028, 4, 30),
    ) = SalaryTableDescriptor(
        id = id,
        effectiveFrom = start,
        verifiedThrough = end,
        tariffPackageId =
            currentPackage.id,
        sourceLabel =
            "Syntetisk A5A5-lønnstabell",
        sourcePageUrl =
            "https://example.invalid/salary-2027",
    )

    private fun nextPeriod(
        descriptor: SalaryTableDescriptor =
            nextDescriptor(),
    ) = SalaryTablePeriod(
        descriptor = descriptor,
        annualSalary = { step ->
            OsloSalaryTable2026
                .annualSalary(step)
                .add(BigDecimal("10000"))
        },
    )

    private fun qualifiedAndPlan(
        descriptor: SalaryTableDescriptor =
            nextDescriptor(),
    ): Pair<
        TariffUpdateQualificationResult.Qualified,
        TariffRuntimeRegistrationPlan,
    > {
        val component =
            TariffUpdateRegisteredComponents
                .salaryTable(descriptor)

        val candidateRegistry =
            TariffUpdateComponentRegistry(
                FerieturTariffUpdateComponents
                    .currentRuntime
                    .all() +
                    component,
            )

        val manifest =
            TariffUpdateManifest(
                updateId =
                    "snapshot-${descriptor.id}",
                tariffPackageId =
                    currentPackage.id,
                rulesetVersion =
                    currentPackage.rulesetVersion,
                effectiveFrom =
                    descriptor.effectiveFrom,
                effectiveThrough =
                    descriptor.verifiedThrough,
                components = listOf(
                    TariffUpdateComponentRef(
                        kind =
                            TariffUpdateComponentKind
                                .SALARY_TABLE,
                        id = descriptor.id,
                        sourceDocumentIds =
                            listOf(source.id),
                    ),
                ),
                sourceDocuments =
                    listOf(source),
                verification =
                    verification,
                activation =
                    TariffUpdateActivation.ACTIVE,
            )

        val qualified =
            TariffUpdateQualifiedActivationGate
                .requireQualified(
                    manifest,
                    candidateRegistry,
                )

        val plan =
            TariffRuntimeRegistrationPlanner
                .requirePlan(
                    qualified,
                    FerieturTariffUpdateComponents
                        .currentRuntime,
                )

        return qualified to plan
    }

    @Test
    fun currentTypedSnapshotMirrorsCurrentRuntimeComponents() {
        val snapshot =
            FerieturRuntimeCatalogSnapshots.current

        assertEquals(
            FerieturTariffUpdateComponents
                .currentRuntime
                .all()
                .toSet(),
            snapshot.componentRegistry
                .all()
                .toSet(),
        )

        val resolved =
            requireNotNull(
                snapshot.resolveDate(
                    32,
                    LocalDate.of(2027, 4, 30),
                ),
            )

        assertEquals(
            "614600",
            resolved.annualSalary.toPlainString(),
        )

        assertEquals(
            OsloSalaryTable2026.tableId,
            resolved.salaryTable.id,
        )
    }

    @Test
    fun isolatedSnapshotResolvesExactSalaryBoundary() {
        val descriptor =
            nextDescriptor()

        val (_, plan) =
            qualifiedAndPlan(descriptor)

        val current =
            FerieturRuntimeCatalogSnapshots.current

        assertNull(
            current.resolveDate(
                32,
                boundary,
            ),
        )

        assertNull(
            OsloSalaryTables.descriptorForDate(
                boundary,
            ),
        )

        val snapshot =
            TariffRuntimeCatalogSnapshotMaterializer
                .requireMaterialized(
                    plan = plan,
                    current = current,
                    updatePayloads = listOf(
                        SalaryTableRuntimePayload(
                            nextPeriod(descriptor),
                        ),
                    ),
                )

        val before =
            requireNotNull(
                snapshot.resolveDate(
                    32,
                    boundary.minusDays(1),
                ),
            )

        val after =
            requireNotNull(
                snapshot.resolveDate(
                    32,
                    boundary,
                ),
            )

        assertEquals(
            "614600",
            before.annualSalary.toPlainString(),
        )

        assertEquals(
            "624600",
            after.annualSalary.toPlainString(),
        )

        assertEquals(
            OsloSalaryTable2026.tableId,
            before.salaryTable.id,
        )

        assertEquals(
            descriptor.id,
            after.salaryTable.id,
        )

        assertEquals(
            before.tariffPackage.id,
            after.tariffPackage.id,
        )

        assertEquals(
            before.rateSet.id,
            after.rateSet.id,
        )

        // The isolated snapshot must not activate anything globally.
        assertNull(
            OsloSalaryTables.descriptorForDate(
                boundary,
            ),
        )
    }

    @Test
    fun missingTypedPayloadFailsClosed() {
        val (_, plan) =
            qualifiedAndPlan()

        val result =
            TariffRuntimeCatalogSnapshotMaterializer
                .materialize(
                    plan = plan,
                    current =
                        FerieturRuntimeCatalogSnapshots.current,
                    updatePayloads = emptyList(),
                )

        assertTrue(
            result is
                TariffRuntimeCatalogMaterializationResult
                    .Rejected,
        )

        val rejected =
            result as
                TariffRuntimeCatalogMaterializationResult
                    .Rejected

        assertTrue(
            rejected.issues.any {
                it.code ==
                    TariffRuntimeCatalogMaterializationIssueCode
                        .MISSING_ADDITION_PAYLOAD
            },
        )
    }

    @Test
    fun unplannedTypedPayloadFailsClosed() {
        val descriptor =
            nextDescriptor()

        val (_, plan) =
            qualifiedAndPlan(descriptor)

        val extra =
            nextDescriptor(
                id = "unplanned-salary",
            )

        val result =
            TariffRuntimeCatalogSnapshotMaterializer
                .materialize(
                    plan = plan,
                    current =
                        FerieturRuntimeCatalogSnapshots.current,
                    updatePayloads = listOf(
                        SalaryTableRuntimePayload(
                            nextPeriod(descriptor),
                        ),
                        SalaryTableRuntimePayload(
                            nextPeriod(extra),
                        ),
                    ),
                )

        assertTrue(
            result is
                TariffRuntimeCatalogMaterializationResult
                    .Rejected,
        )

        val rejected =
            result as
                TariffRuntimeCatalogMaterializationResult
                    .Rejected

        assertTrue(
            rejected.issues.any {
                it.code ==
                    TariffRuntimeCatalogMaterializationIssueCode
                        .UNPLANNED_UPDATE_PAYLOAD
            },
        )
    }

    @Test
    fun typedPayloadMetadataMustMatchDryRunPlan() {
        val descriptor =
            nextDescriptor()

        val (_, plan) =
            qualifiedAndPlan(descriptor)

        val mismatched =
            descriptor.copy(
                verifiedThrough =
                    descriptor.verifiedThrough
                        .minusDays(1),
            )

        val result =
            TariffRuntimeCatalogSnapshotMaterializer
                .materialize(
                    plan = plan,
                    current =
                        FerieturRuntimeCatalogSnapshots.current,
                    updatePayloads = listOf(
                        SalaryTableRuntimePayload(
                            nextPeriod(mismatched),
                        ),
                    ),
                )

        assertTrue(
            result is
                TariffRuntimeCatalogMaterializationResult
                    .Rejected,
        )

        val rejected =
            result as
                TariffRuntimeCatalogMaterializationResult
                    .Rejected

        assertTrue(
            rejected.issues.any {
                it.code ==
                    TariffRuntimeCatalogMaterializationIssueCode
                        .PAYLOAD_METADATA_MISMATCH
            },
        )
    }

    @Test
    fun materializationDoesNotMutateSourceSnapshotOrGlobalCatalog() {
        val descriptor =
            nextDescriptor(
                id = "salary-isolation-proof",
            )

        val (_, plan) =
            qualifiedAndPlan(descriptor)

        val current =
            FerieturRuntimeCatalogSnapshots.current

        val originalComponents =
            current.componentRegistry
                .all()
                .toList()

        val materialized =
            TariffRuntimeCatalogSnapshotMaterializer
                .requireMaterialized(
                    plan = plan,
                    current = current,
                    updatePayloads = listOf(
                        SalaryTableRuntimePayload(
                            nextPeriod(descriptor),
                        ),
                    ),
                )

        assertEquals(
            originalComponents,
            current.componentRegistry.all(),
        )

        assertNull(
            current.salaryTableForDate(
                boundary,
            ),
        )

        assertNull(
            OsloSalaryTables.descriptorForDate(
                boundary,
            ),
        )

        assertEquals(
            descriptor.id,
            materialized
                .salaryTableForDate(boundary)
                ?.id,
        )
    }
}
