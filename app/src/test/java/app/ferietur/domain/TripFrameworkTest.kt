package app.ferietur.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TripFrameworkTest {
    @Test
    fun employerDeterminesWhetherDok25BasisIsConfirmed() {
        assertTrue(EmployerKind.OSLO_KOMMUNE.isRuleBasisConfirmed())
        assertEquals("Oslo kommune – Dok. 25 2026–28, kapittel 20", EmployerKind.OSLO_KOMMUNE.ruleBasisLabel())
        assertFalse(EmployerKind.PRIVATE_OR_OTHER_UNRESOLVED.isRuleBasisConfirmed())
        assertEquals("Ikke endelig avklart", EmployerKind.PRIVATE_OR_OTHER_UNRESOLVED.ruleBasisLabel())
    }

    @Test
    fun rosterComparisonIsIndependentFromEmployerAndPayer() {
        assertEquals(FundingMode.TURNUS_PLUS_EXTERNAL, RosterComparisonMode.USE_NORMAL_ROSTER.toFundingMode())
        assertEquals(FundingMode.VACATION_SEPARATE, RosterComparisonMode.DO_NOT_USE_NORMAL_ROSTER.toFundingMode())
    }
}
