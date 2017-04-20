package org.duniter.elasticsearch.subscription.util;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by blavenie on 10/04/17.
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

    public static long delayBeforeHour(int hour) {
        return nextHour(hour).getTime() - System.currentTimeMillis();
    }

    public static long delayBeforeDayAndHour(int dayOfTheWeek, int hour) {
        return nextDayAndHour(dayOfTheWeek, hour).getTime() - System.currentTimeMillis();
    }


}

