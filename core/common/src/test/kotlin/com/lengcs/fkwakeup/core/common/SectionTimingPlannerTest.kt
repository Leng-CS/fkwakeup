package com.lengcs.fkwakeup.core.common

import com.google.common.truth.Truth.assertThat
import com.lengcs.fkwakeup.core.model.SectionTemplate
import org.junit.Test

class SectionTimingPlannerTest {
    @Test
    fun `single continuous segment calculates following lessons`() {
        val plan = SectionTimingPlanner.build(4, listOf(SectionTimingSegment(1, 8 * 60, 45, 10)))

        assertThat(plan).isInstanceOf(SectionTimingPlanResult.Valid::class.java)
        val rows = (plan as SectionTimingPlanResult.Valid).sections
        assertThat(rows.map { it.startMinutes to it.endMinutes }).containsExactly(
            480 to 525, 535 to 580, 590 to 635, 645 to 690,
        ).inOrder()
    }

    @Test
    fun `afternoon breakpoint recalculates only its following lessons`() {
        val plan = SectionTimingPlanner.build(
            sectionCount = 8,
            segments = listOf(
                SectionTimingSegment(1, 480, 45, 10),
                SectionTimingSegment(5, 840, 50, 15),
            ),
        ) as SectionTimingPlanResult.Valid

        assertThat(plan.sections[3].endMinutes).isEqualTo(690)
        assertThat(plan.sections.drop(4).map { it.startMinutes to it.endMinutes }).containsExactly(
            840 to 890, 905 to 955, 970 to 1020, 1035 to 1085,
        ).inOrder()
    }

    @Test
    fun `breakpoint cannot overlap the previous lesson`() {
        val plan = SectionTimingPlanner.build(
            4,
            listOf(SectionTimingSegment(1, 480, 45, 10), SectionTimingSegment(3, 500, 45, 10)),
        )

        assertThat(plan).isInstanceOf(SectionTimingPlanResult.Invalid::class.java)
        assertThat((plan as SectionTimingPlanResult.Invalid).message).contains("第 2 节")
    }

    @Test
    fun `saved table is grouped back into continuous segments`() {
        val segments = SectionTimingPlanner.segmentsFrom(
            listOf(
                SectionTemplate(1, 1, 480, 525),
                SectionTemplate(1, 2, 535, 580),
                SectionTemplate(1, 3, 840, 890),
                SectionTemplate(1, 4, 905, 955),
            ),
        )

        assertThat(segments).containsExactly(
            SectionTimingSegment(1, 480, 45, 10),
            SectionTimingSegment(3, 840, 50, 15),
        ).inOrder()
    }
}
