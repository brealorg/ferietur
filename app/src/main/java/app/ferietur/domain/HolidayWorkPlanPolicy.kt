package app.ferietur.domain

/**
 * EQS01A2 finalization policy.
 *
 * Oslo EQS routine 53398 makes the holiday-stay work plan and its notice status relevant before
 * a new calculation is frozen. This policy does not decide overtime and does not alter monetary
 * calculation; EQS01B owns that semantic change.
 */
object HolidayWorkPlanPolicy {
    fun requiresExplicitStatus(employerKind: EmployerKind): Boolean =
        employerKind == EmployerKind.OSLO_KOMMUNE

    fun canFinalize(
        employerKind: EmployerKind,
        status: HolidayWorkPlanStatus,
    ): Boolean =
        !requiresExplicitStatus(employerKind) ||
            status != HolidayWorkPlanStatus.NOT_CLARIFIED
}
