package app.ferietur.domain

enum class TariffRuntimeRegistrationActionKind {
    ADD,
    ALREADY_PRESENT,
}

data class TariffRuntimeRegistrationAction(
    val kind: TariffRuntimeRegistrationActionKind,
    val component: TariffUpdateRegisteredComponent,
)

enum class TariffRuntimeRegistrationIssueCode {
    IMMUTABLE_COMPONENT_REDEFINITION,
    COMPONENT_PERIOD_OVERLAP,
    COMPONENT_PERIOD_GAP,
    NEW_PACKAGE_INCOMPLETE,
    NO_NEW_COMPONENTS,
}

data class TariffRuntimeRegistrationIssue(
    val code: TariffRuntimeRegistrationIssueCode,
    val detail: String,
)

data class TariffRuntimeRegistrationPlan(
    val updateId: String,
    val actions: List<TariffRuntimeRegistrationAction>,
    val projectedComponents: List<TariffUpdateRegisteredComponent>,
) {
    val additions: List<TariffUpdateRegisteredComponent>
        get() = actions
            .filter {
                it.kind ==
                    TariffRuntimeRegistrationActionKind.ADD
            }
            .map { it.component }

    val alreadyPresent: List<TariffUpdateRegisteredComponent>
        get() = actions
            .filter {
                it.kind ==
                    TariffRuntimeRegistrationActionKind.ALREADY_PRESENT
            }
            .map { it.component }
}

sealed interface TariffRuntimeRegistrationPlanResult {
    data class Planned(
        val plan: TariffRuntimeRegistrationPlan,
    ) : TariffRuntimeRegistrationPlanResult

    data class Rejected(
        val updateId: String,
        val issues: List<TariffRuntimeRegistrationIssue>,
    ) : TariffRuntimeRegistrationPlanResult
}

/**
 * Add-only dry-run between A5A3 qualification and any future runtime mutation.
 *
 * The planner projects the resulting immutable component set but never mutates
 * FerieturTariffs, FerieturTariffRates, OsloSalaryTables or the resolver.
 */
object TariffRuntimeRegistrationPlanner {

    private val periodizedKinds = setOf(
        TariffUpdateComponentKind.TARIFF_PACKAGE,
        TariffUpdateComponentKind.RATE_SET,
        TariffUpdateComponentKind.SALARY_TABLE,
    )

    private val completeNewPackageKinds = setOf(
        TariffUpdateComponentKind.TARIFF_PACKAGE,
        TariffUpdateComponentKind.RULESET,
        TariffUpdateComponentKind.RATE_SET,
        TariffUpdateComponentKind.SALARY_TABLE,
        TariffUpdateComponentKind.RULE_SOURCE_BINDINGS,
    )

    fun plan(
        qualified: TariffUpdateQualificationResult.Qualified,
        currentRuntime: TariffUpdateComponentRegistry,
    ): TariffRuntimeRegistrationPlanResult {
        val issues =
            mutableListOf<TariffRuntimeRegistrationIssue>()

        val actions =
            mutableListOf<TariffRuntimeRegistrationAction>()

        qualified.components.forEach { candidate ->
            val existing = currentRuntime.component(
                candidate.kind,
                candidate.id,
            )

            when {
                existing == null -> {
                    actions += TariffRuntimeRegistrationAction(
                        kind =
                            TariffRuntimeRegistrationActionKind.ADD,
                        component = candidate,
                    )
                }

                existing == candidate -> {
                    actions += TariffRuntimeRegistrationAction(
                        kind =
                            TariffRuntimeRegistrationActionKind.ALREADY_PRESENT,
                        component = candidate,
                    )
                }

                else -> {
                    issues += TariffRuntimeRegistrationIssue(
                        code =
                            TariffRuntimeRegistrationIssueCode
                                .IMMUTABLE_COMPONENT_REDEFINITION,
                        detail =
                            "Komponenten ${candidate.kind}/${candidate.id} " +
                                "finnes allerede med annen immutable metadata.",
                    )
                }
            }
        }

        val additions = actions
            .filter {
                it.kind ==
                    TariffRuntimeRegistrationActionKind.ADD
            }
            .map { it.component }

        val projected =
            currentRuntime.all() + additions

        validateProjectedPeriods(
            projected = projected,
            issues = issues,
        )

        validateNewPackageBundles(
            additions = additions,
            qualifiedComponents = qualified.components,
            issues = issues,
        )

        if (
            additions.isEmpty() &&
            issues.none {
                it.code ==
                    TariffRuntimeRegistrationIssueCode
                        .IMMUTABLE_COMPONENT_REDEFINITION
            }
        ) {
            issues += TariffRuntimeRegistrationIssue(
                code =
                    TariffRuntimeRegistrationIssueCode.NO_NEW_COMPONENTS,
                detail =
                    "Oppdateringen ${qualified.manifest.updateId} " +
                        "inneholder ingen nye runtime-komponenter.",
            )
        }

        if (issues.isNotEmpty()) {
            return TariffRuntimeRegistrationPlanResult.Rejected(
                updateId = qualified.manifest.updateId,
                issues = issues.toList(),
            )
        }

        return TariffRuntimeRegistrationPlanResult.Planned(
            plan = TariffRuntimeRegistrationPlan(
                updateId = qualified.manifest.updateId,
                actions = actions.toList(),
                projectedComponents = projected.toList(),
            ),
        )
    }

    fun requirePlan(
        qualified: TariffUpdateQualificationResult.Qualified,
        currentRuntime: TariffUpdateComponentRegistry,
    ): TariffRuntimeRegistrationPlan =
        when (
            val result = plan(
                qualified,
                currentRuntime,
            )
        ) {
            is TariffRuntimeRegistrationPlanResult.Planned ->
                result.plan

            is TariffRuntimeRegistrationPlanResult.Rejected ->
                throw IllegalArgumentException(
                    "Runtime-registreringen for ${result.updateId} ble avvist: " +
                        result.issues.joinToString("; ") {
                            "${it.code}: ${it.detail}"
                        },
                )
        }

    private fun validateProjectedPeriods(
        projected: List<TariffUpdateRegisteredComponent>,
        issues: MutableList<TariffRuntimeRegistrationIssue>,
    ) {
        periodizedKinds.forEach { kind ->
            val kindComponents =
                projected.filter { it.kind == kind }

            val groups =
                if (
                    kind ==
                    TariffUpdateComponentKind.TARIFF_PACKAGE
                ) {
                    mapOf(
                        "all-packages" to kindComponents,
                    )
                } else {
                    kindComponents.groupBy {
                        it.tariffPackageId
                    }
                }

            groups.forEach { (owner, components) ->
                val ordered =
                    components.sortedBy {
                        it.effectiveFrom
                    }

                ordered
                    .zipWithNext()
                    .forEach pair@{ (previous, next) ->
                        if (
                            !next.effectiveFrom.isAfter(
                                previous.effectiveThrough,
                            )
                        ) {
                            issues +=
                                TariffRuntimeRegistrationIssue(
                                    code =
                                        TariffRuntimeRegistrationIssueCode
                                            .COMPONENT_PERIOD_OVERLAP,
                                    detail =
                                        "$kind i $owner overlapper: " +
                                            "${previous.id} " +
                                            "${previous.effectiveFrom}–" +
                                            "${previous.effectiveThrough} og " +
                                            "${next.id} " +
                                            "${next.effectiveFrom}–" +
                                            "${next.effectiveThrough}.",
                                )

                            return@pair
                        }

                        val expectedNext =
                            previous.effectiveThrough.plusDays(1)

                        if (
                            next.effectiveFrom != expectedNext
                        ) {
                            issues +=
                                TariffRuntimeRegistrationIssue(
                                    code =
                                        TariffRuntimeRegistrationIssueCode
                                            .COMPONENT_PERIOD_GAP,
                                    detail =
                                        "$kind i $owner har gap mellom " +
                                            "${previous.id} og ${next.id}: " +
                                            "forventet $expectedNext, fikk " +
                                            "${next.effectiveFrom}.",
                                )
                        }
                    }
            }
        }
    }

    private fun validateNewPackageBundles(
        additions: List<TariffUpdateRegisteredComponent>,
        qualifiedComponents: List<TariffUpdateRegisteredComponent>,
        issues: MutableList<TariffRuntimeRegistrationIssue>,
    ) {
        val newPackageIds = additions
            .filter {
                it.kind ==
                    TariffUpdateComponentKind.TARIFF_PACKAGE
            }
            .map { it.id }

        newPackageIds.forEach { packageId ->
            val suppliedKinds =
                qualifiedComponents
                    .filter {
                        it.tariffPackageId == packageId
                    }
                    .map { it.kind }
                    .toSet()

            val missing =
                completeNewPackageKinds - suppliedKinds

            if (missing.isNotEmpty()) {
                issues += TariffRuntimeRegistrationIssue(
                    code =
                        TariffRuntimeRegistrationIssueCode
                            .NEW_PACKAGE_INCOMPLETE,
                    detail =
                        "Ny tariffpakke $packageId mangler runtime-komponenter: " +
                            missing
                                .sortedBy { it.name }
                                .joinToString(),
                )
            }
        }
    }
}
