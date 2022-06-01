package org.duniter.core.util;

/*-
 * #%L
 * Duniter4j :: Core Shared
 * %%
 * Copyright (C) 2014 - 2017 EIS
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by blavenie on 13/09/17.
 */
public class DateUtils {
    public static final long DAY_DURATION_IN_MILLIS = 24 * 60 * 60 * 1000;


    public static Date nextHour(int hour) {
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(System.currentTimeMillis());
        if (cal.get(Calendar.HOUR_OF_DAY) >= hour) {
            // Too late for today: add 1 day (will wait tomorrow)
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public static Date nextDayAndHour(int dayOfTheWeek, int hour) {
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(System.currentTimeMillis());
        if (cal.get(Calendar.DAY_OF_WEEK) > dayOfTheWeek || (cal.get(Calendar.DAY_OF_WEEK) == dayOfTheWeek && cal.get(Calendar.HOUR_OF_DAY) >= hour)) {
            // Too late for this week: will wait for next week
            cal.add(Calendar.WEEK_OF_YEAR, 1);
        }
        cal.set(Calendar.DAY_OF_WEEK, dayOfTheWeek);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }


    public static Date nextHour() {
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.add(Calendar.HOUR, 1);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public static long delayBeforeHour(int hour) {
        return nextHour(hour).getTime() - System.currentTimeMillis();
    }

    public static long delayBeforeNextHour() {
        return nextHour().getTime() - System.currentTimeMillis();
    }

    public static long delayBeforeDayAndHour(int dayOfTheWeek, int hour) {
        return nextDayAndHour(dayOfTheWeek, hour).getTime() - System.currentTimeMillis();
    }

    public static String elapsedTime(long timeInMs) {
        long elapsedTime = System.currentTimeMillis() - timeInMs;
        StringBuilder sb = new StringBuilder();
        sb.append("in ");
        if (elapsedTime < 1000) {
            return sb.append(elapsedTime).append("ms").toString();
        }
        double seconds = (double) elapsedTime / 1_000;
        if (seconds < 60) {
            return sb.append(seconds).append("s").toString();
        }
        int minutesFloor = (int) Math.floor(seconds / 60);
        int secondsFloor = (int) Math.floor(seconds - minutesFloor * 60);
        int millis = (int) Math.floor((seconds - secondsFloor - minutesFloor * 60) * 1_000);

        return sb.append(minutesFloor).append("min ")
            .append(secondsFloor).append("s ")
            .append(millis).append("ms")
            .toString();
    }
}
