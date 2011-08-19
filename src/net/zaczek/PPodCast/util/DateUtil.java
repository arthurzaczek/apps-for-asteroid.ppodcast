package net.zaczek.PPodCast.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtil {

	public static final SimpleDateFormat rfc822DateFormats[] = new SimpleDateFormat[] {
			new SimpleDateFormat("EEE, d MMM yy HH:mm:ss z"),
			new SimpleDateFormat("EEE, d MMM yy HH:mm z"),
			new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z"),
			new SimpleDateFormat("EEE, d MMM yyyy HH:mm z"),
			new SimpleDateFormat("d MMM yy HH:mm z"), 
			new SimpleDateFormat("d MMM yy HH:mm:ss z"),
			new SimpleDateFormat("d MMM yyyy HH:mm z"), 
			new SimpleDateFormat("d MMM yyyy HH:mm:ss z")
		};
	
	/**
	 * Parse an RFC 822 date string.
	 * 
	 * @param dateString The date string to parse
	 * @return The date, or null if it could not be parsed.
	 */
	public static Date parseRfc822DateString(String dateString) {
		Date date = null;
		for (SimpleDateFormat sdf : rfc822DateFormats) {
			try {
				date = sdf.parse(dateString);
			} catch (ParseException e) {
				// Don't care, we'll just run through all
			}
			if (date != null) {
				return date;
			}
		}
		return null;
	}

	/**
	 * Format a date into a format suitable for SQLite
	 * 
	 * @param date The date to format
	 * @return The formatted date, or null if it could not be formatted.
	 */
	public static String dateToSqliteDateString(Date date) {
		// The format is the same as CURRENT_TIMESTAMP: "YYYY-MM-DD HH:MM:SS"
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		if (date != null) {
			return sdf.format(date);
		}
		return null;
	}
	
	public static String getTimeString(long millis) {
	    StringBuffer buf = new StringBuffer();

	    int hours = (int)(millis / (1000*60*60));
	    int minutes = (int)(( millis % (1000*60*60) ) / (1000*60));
	    int seconds = (int)(( ( millis % (1000*60*60) ) % (1000*60) ) / 1000);

	    buf
	        .append(String.format("%02d", hours))
	        .append(":")
	        .append(String.format("%02d", minutes))
	        .append(":")
	        .append(String.format("%02d", seconds));

	    return buf.toString();
	}
}
