package vn.edu.usth.flightinfo.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TimeUtilsTest {

    @Test
    public void extractsHourMinute() {
        assertEquals("08:05", TimeUtils.hhmm("2025-01-01 08:05"));
        assertNull(TimeUtils.hhmm("08:05"));
        assertNull(TimeUtils.hhmm(null));
    }

    @Test
    public void computesProgress() {
        assertEquals(0, TimeUtils.progressPercent(100, 200, 50));
        assertEquals(50, TimeUtils.progressPercent(100, 200, 150));
        assertEquals(100, TimeUtils.progressPercent(100, 200, 500));
        assertEquals(-1, TimeUtils.progressPercent(0, 200, 150));
        assertEquals(-1, TimeUtils.progressPercent(200, 100, 150));
    }

    @Test
    public void shiftsAcrossMidnightAndYears() {
        assertEquals("2025-01-02 01:30", TimeUtils.shift("2025-01-01 23:00", 150 * 60));
        assertEquals("2024-12-31 23:00", TimeUtils.shift("2025-01-01 01:00", -2 * 3600));
        assertEquals("garbage", TimeUtils.shift("garbage", 60));
    }
}
