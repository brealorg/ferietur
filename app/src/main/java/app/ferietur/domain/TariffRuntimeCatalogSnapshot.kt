package app.ferietur.domain

import java.math.BigDecimal
import java.time.LocalDate

/**
 * Actual typed runtime payload corresponding to one immutable update component.
 *
 * These payloads are materialized only into an isolated catalog snapshot in
 * A5A5. They do not register themselves in Ferietur's global runtime objects.
 */
sealed interface TariffRuntimeComponentPayload {
    val component: TariffUpdateRegisteredComponent
}

data class TariffPackageRuntimePayload(
    val value: TariffPackage,
) : TariffRuntimeComponentPayload {
    override val component =
        TariffUpdateRegisteredComponents.tariffPackage(value)
}

data class RulesetRuntimePayload(
    val tariffPackage: TariffPackage,
) : TariffRuntimeComponentPayload {
    override val component =
        TariffUpdateRegisteredComponents.ruleset(tariffPackage)
}

data class RateSetRuntimePayload(
    val value: TariffRateSet,
) : TariffRuntimeComponentPayload {
    override val component =
        TariffUpdateRegisteredComponents.rateSet(value)
}

class SalaryTableRuntimePayload(
    val value: SalaryTablePeriod,
) : TariffRuntimeComponentPayload {
    override val component =
        TariffUpdateRegisteredComponents.salaryTable(
            value.descriptor,
        )
}

data class RuleSourceBindingsRuntimePayload(
    val tariffPackage: TariffPackage,
    val bindings: List<RuleSourceBinding>,
) : TariffRuntimeComponentPayload {
    override val component =
        TariffUpdateRegisteredComponents.ruleSourceBindings(
            tariffPackage,
            bindings,
        )
}

data class ResolvedRuntimeCatalogDate(
    val tariffPackage: TariffPackage,
    val rateSet: TariffRateSet,
    val salaryTable: SalaryTableDescriptor,
    val annualSalary: BigDecimal,
)

/**
 * Fully typed, isolated view of a possible runtime catalog.
 *
 * It can resolve dates exactly like a future runtime catalog would, but it is
 * not connected to FerieturTariffResolver or any global catalog singleton.
 */
class TariffRuntimeCatalogSnapshot internal constructor(
    payloadValues: List<TariffRuntimeComponentPayload>,
) {
    private val payloads =
        payloadValues.toList()

    private val payloadByKey =
        payloads.associateBy {
            it.component.kind to it.component.id
        }

    val componentRegistry =
        TariffUpdateComponentRegistry(
            payloads.map { it.component },
        )

    private val tariffPackages =
        payloads
            .filterIsInstance<TariffPackageRuntimePayload>()
            .map { it.value }
            .sortedBy { it.effectiveFrom }

    private val rateSets =
        payloads
            .filterIsInstance<RateSetRuntimePayload>()
            .map { it.value }

    private val salaryPeriods =
        payloads
            .filterIsInstance<SalaryTableRuntimePayload>()
            .map { it.value }

    private val rateSetCatalog =
        TariffRateSetCatalog(rateSets)

    private val salaryTableCatalog =
        SalaryTableCatalog(salaryPeriods)

    init {
        require(payloads.isNotEmpty()) {
            "Runtime-katalogsnapshot kan ikke være tomt."
        }

        require(payloadByKey.size == payloads.size) {
            "Duplisert typed runtime-payload."
        }

        require(tariffPackages.isNotEmpty()) {
            "Runtime-katalogsnapshot mangler tariffpakke."
        }

        require(rateSets.isNotEmpty()) {
            "Runtime-katalogsnapshot mangler satssett."
        }

        require(salaryPeriods.isNotEmpty()) {
            "Runtime-katalogsnapshot mangler lønnstabell."
        }
    }

    fun payloads(): List<TariffRuntimeComponentPayload> =
        payloads.toList()

    fun packageForDate(
        date: LocalDate,
    ): TariffPackage? =
        tariffPackages.lastOrNull {
            it.covers(date)
        }

    fun rateSetForDate(
        tariffPackageId: String,
        date: LocalDate,
    ): TariffRateSet? =
        rateSetCatalog.forDate(
            tariffPackageId,
            date,
        )

    fun salaryTableForDate(
        date: LocalDate,
    ): SalaryTableDescriptor? =
        salaryTableCatalog.descriptorForDate(date)

    fun annualSalaryForDate(
        step: Int,
        date: LocalDate,
    ): BigDecimal? =
        salaryTableCatalog.annualSalaryForDate(
            step,
            date,
        )

    fun resolveDate(
        salaryStep: Int,
        date: LocalDate,
    ): ResolvedRuntimeCatalogDate? {
        val tariffPackage =
            packageForDate(date)
                ?: return null

        val rateSet =
            rateSetForDate(
                tariffPackage.id,
                date,
            )
                ?: return null

        val salaryTable =
            salaryTableForDate(date)
                ?: return null

        if (
            salaryTable.tariffPackageId !=
            tariffPackage.id
        ) {
            return null
        }

        val annualSalary =
            annualSalaryForDate(
                salaryStep,
                date,
            )
                ?: return null

        return ResolvedRuntimeCatalogDate(
            tariffPackage = tariffPackage,
            rateSet = rateSet,
            salaryTable = salaryTable,
            annualSalary = annualSalary,
        )
    }
}

/**
 * Typed mirror of today's live catalog state.
 *
 * Building or using this snapshot does not change live runtime state.
 */
object FerieturRuntimeCatalogSnapshots {
    val current: TariffRuntimeCatalogSnapshot by lazy {
        val tariffPackage =
            FerieturTariffs.dok25_2026_2028

        val salaryDescriptor =
            requireNotNull(
                OsloSalaryTables.descriptorForDate(
                    OsloSalaryTable2026.effectiveFromDate,
                ),
            )

        TariffRuntimeCatalogSnapshot(
            listOf(
                TariffPackageRuntimePayload(
                    tariffPackage,
                ),
                RulesetRuntimePayload(
                    tariffPackage,
                ),
                RateSetRuntimePayload(
                    FerieturTariffRates.dok25_2026_2028,
                ),
                SalaryTableRuntimePayload(
                    SalaryTablePeriod(
                        descriptor = salaryDescriptor,
                        annualSalary =
                            OsloSalaryTable2026::annualSalary,
                    ),
                ),
                RuleSourceBindingsRuntimePayload(
                    tariffPackage = tariffPackage,
                    bindings =
                        FerieturRuleSources.bindings,
                ),
            ),
        )
    }
}

enum class TariffRuntimeCatalogMaterializationIssueCode {
    DUPLICATE_UPDATE_PAYLOAD,
    MISSING_ADDITION_PAYLOAD,
    UNPLANNED_UPDATE_PAYLOAD,
    PAYLOAD_METADATA_MISMATCH,
    PROJECTED_COMPONENT_SET_MISMATCH,
    SNAPSHOT_CONSTRUCTION_FAILED,
}

data class TariffRuntimeCatalogMaterializationIssue(
    val code: TariffRuntimeCatalogMaterializationIssueCode,
    val detail: String,
)

sealed interface TariffRuntimeCatalogMaterializationResult {
    data class Materialized(
        val snapshot: TariffRuntimeCatalogSnapshot,
    ) : TariffRuntimeCatalogMaterializationResult

    data class Rejected(
        val updateId: String,
        val issues:
            List<TariffRuntimeCatalogMaterializationIssue>,
    ) : TariffRuntimeCatalogMaterializationResult
}

/**
 * Materializes the A5A4 dry-run plan into a typed isolated snapshot.
 *
 * Every ADD action must have exactly one typed payload whose generated
 * immutable component metadata is byte-for-byte equivalent at the model level
 * to the component approved by the registration plan.
 */
object TariffRuntimeCatalogSnapshotMaterializer {

    fun materialize(
        plan: TariffRuntimeRegistrationPlan,
        current: TariffRuntimeCatalogSnapshot,
        updatePayloads: List<TariffRuntimeComponentPayload>,
    ): TariffRuntimeCatalogMaterializationResult {
        val issues =
            mutableListOf<TariffRuntimeCatalogMaterializationIssue>()

        fun issue(
            code: TariffRuntimeCatalogMaterializationIssueCode,
            detail: String,
        ) {
            issues +=
                TariffRuntimeCatalogMaterializationIssue(
                    code,
                    detail,
                )
        }

        val payloadGroups =
            updatePayloads.groupBy {
                it.component.kind to it.component.id
            }

        payloadGroups
            .filterValues { it.size > 1 }
            .keys
            .forEach { (kind, id) ->
                issue(
                    TariffRuntimeCatalogMaterializationIssueCode
                        .DUPLICATE_UPDATE_PAYLOAD,
                    "Typed payload $kind/$id er duplisert.",
                )
            }

        val plannedAdditions =
            plan.additions.associateBy {
                it.kind to it.id
            }

        payloadGroups.keys
            .filterNot {
                it in plannedAdditions
            }
            .forEach { (kind, id) ->
                issue(
                    TariffRuntimeCatalogMaterializationIssueCode
                        .UNPLANNED_UPDATE_PAYLOAD,
                    "Typed payload $kind/$id finnes ikke som ADD i planen.",
                )
            }

        plan.additions.forEach { planned ->
            val key =
                planned.kind to planned.id

            val payload =
                payloadGroups[key]
                    ?.singleOrNull()

            if (payload == null) {
                issue(
                    TariffRuntimeCatalogMaterializationIssueCode
                        .MISSING_ADDITION_PAYLOAD,
                    "ADD-komponenten ${planned.kind}/${planned.id} " +
                        "mangler én entydig typed payload.",
                )
                return@forEach
            }

            if (payload.component != planned) {
                issue(
                    TariffRuntimeCatalogMaterializationIssueCode
                        .PAYLOAD_METADATA_MISMATCH,
                    "Typed payload ${planned.kind}/${planned.id} " +
                        "samsvarer ikke med metadata i dry-run-planen.",
                )
            }
        }

        if (issues.isNotEmpty()) {
            return TariffRuntimeCatalogMaterializationResult.Rejected(
                    updateId = plan.updateId,
                    issues = issues.toList(),
                )
        }

        val projectedPayloads =
            current.payloads() + updatePayloads

        val projectedComponentMap =
            projectedPayloads
                .map { it.component }
                .associateBy {
                    it.kind to it.id
                }

        val plannedComponentMap =
            plan.projectedComponents.associateBy {
                it.kind to it.id
            }

        if (
            projectedComponentMap !=
            plannedComponentMap
        ) {
            issue(
                TariffRuntimeCatalogMaterializationIssueCode
                    .PROJECTED_COMPONENT_SET_MISMATCH,
                "Typed payload-projeksjon samsvarer ikke med " +
                    "A5A4-planens projectedComponents.",
            )

            return TariffRuntimeCatalogMaterializationResult.Rejected(
                    updateId = plan.updateId,
                    issues = issues.toList(),
                )
        }

        val snapshot =
            try {
                TariffRuntimeCatalogSnapshot(
                    projectedPayloads,
                )
            } catch (failure: IllegalArgumentException) {
                issue(
                    TariffRuntimeCatalogMaterializationIssueCode
                        .SNAPSHOT_CONSTRUCTION_FAILED,
                    failure.message
                        ?: "Ukjent snapshot-feil.",
                )

                return TariffRuntimeCatalogMaterializationResult.Rejected(
                        updateId = plan.updateId,
                        issues = issues.toList(),
                    )
            }

        return TariffRuntimeCatalogMaterializationResult.Materialized(
                snapshot,
            )
    }

    fun requireMaterialized(
        plan: TariffRuntimeRegistrationPlan,
        current: TariffRuntimeCatalogSnapshot,
        updatePayloads: List<TariffRuntimeComponentPayload>,
    ): TariffRuntimeCatalogSnapshot =
        when (
            val result =
                materialize(
                    plan,
                    current,
                    updatePayloads,
                )
        ) {
            is TariffRuntimeCatalogMaterializationResult.Materialized ->
                result.snapshot

            is TariffRuntimeCatalogMaterializationResult.Rejected ->
                throw IllegalArgumentException(
                    "Runtime-katalogsnapshot for ${result.updateId} " +
                        "ble avvist: " +
                        result.issues.joinToString("; ") {
                            "${it.code}: ${it.detail}"
                        },
                )
        }
}
