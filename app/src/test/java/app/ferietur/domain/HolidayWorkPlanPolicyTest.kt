package app.ferietur.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HolidayWorkPlanPolicyTest {
    @Test
    fun osloRequiresExplicitHolidayWorkPlanStatus() {
        assertTrue(HolidayWorkPlanPolicy.requiresExplicitStatus(EmployerKind.OSLO_KOMMUNE))
        assertFalse(
            HolidayWorkPlanPolicy.canFinalize(
                EmployerKind.OSLO_KOMMUNE,
                HolidayWorkPlanStatus.NOT_CLARIFIED,
            ),
        )
    }

    @Test
    fun bothResolvedOsloStatusesAllowFinalizationGate() {
        assertTrue(
            HolidayWorkPlanPolicy.canFinalize(
                EmployerKind.OSLO_KOMMUNE,
                HolidayWorkPlanStatus.APPROVED_AND_TIMELY_NOTIFIED,
            ),
        )
        assertTrue(
            HolidayWorkPlanPolicy.canFinalize(
                EmployerKind.OSLO_KOMMUNE,
                HolidayWorkPlanStatus.NOT_APPROVED_OR_LATE,
            ),
        )
    }

    @Test
    fun nonOsloFrameworkDoesNotInventOsloEqsRequirement() {
        assertTrue(
            HolidayWorkPlanPolicy.canFinalize(
                EmployerKind.PRIVATE_OR_OTHER_UNRESOLVED,
                HolidayWorkPlanStatus.NOT_CLARIFIED,
            ),
        )
        assertTrue(
            HolidayWorkPlanPolicy.canFinalize(
                EmployerKind.UNSPECIFIED,
                HolidayWorkPlanStatus.NOT_CLARIFIED,
            ),
        )
    }
}
