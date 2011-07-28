package it.unitn.wildmac;

import it.unitn.wildmac.messages.Report;

public class GpsPosition {
	public class GpsCoordinate {
		private short direction, degree;

		private int minute;

		public GpsCoordinate(short direction, short degree, int minute) {
			this.direction = direction;
			this.degree = degree;
			this.minute = minute;
		}

		public short getDegree() {
			return degree;
		}

		public short getDirection() {
			return direction;
		}

		public int getMinute() {
			return minute;
		}

		public String toString() {
			String tmp = new Double((double) minute / 60.).toString()
					.replaceAll("\\.", "");
			return ((direction == 'S' || direction == 'W') ? "-" : "")
					+ new Integer(degree) + "." + tmp;
		}
	}

	public class GpsTime {
		private int hour, min, sec, ms = 0;
		private long timestamp;

		public GpsTime(int hour, int min, int sec, long timestamp) {
			this.hour = hour;
			this.min = min;
			this.sec = sec;
			this.timestamp = timestamp;
			this.ms = 0;
		}

		private GpsTime(GpsTime reference, long otherTimestamp) {
			timestamp = 0;

			long delta = otherTimestamp - reference.timestamp;
			long carry = delta;

			hour = (int) (reference.hour + delta / 1000 / 3600);
			carry %= 1000 * 3600;
			min = (int) (reference.min + carry / 1000 / 60);
			carry %= 1000 * 60;
			sec = (int) (reference.sec + carry / 1000);
			carry %= 1000;
			ms = (int) carry;
		}

		public int getHour() {
			return hour;
		}

		public int getMin() {
			return min;
		}

		public int getSec() {
			return sec;
		}

		public String toString() {
			if (ms != 0)
				return hour + ":" + min + ":" + sec + "." + ms;
			return hour + ":" + min + ":" + sec;
		}

		public GpsTime convertTimestamp(long otherTimestamp) {
			if (timestamp == 0)
				return null;
			return new GpsTime(this, otherTimestamp);
		}
	}

	private Report report;

	public GpsPosition(Report report) {
		this.report = report;
	}

	public GpsCoordinate getLatitude() {
		return new GpsCoordinate(report.get_gps_latitude_direction(),
				report.get_gps_latitude_degree(),
				report.get_gps_latitude_minute());
	}

	public GpsCoordinate getLongitude() {
		return new GpsCoordinate(report.get_gps_longitude_direction(),
				report.get_gps_longitude_degree(),
				report.get_gps_longitude_minute());
	}

	public GpsTime getTime() {
		return new GpsTime(report.get_gps_time_hour(),
				report.get_gps_time_min(), report.get_gps_time_sec(),
				report.get_gps_timestamp());
	}

	public long getTimestamp() {
		return report.get_gps_timestamp();
	}
}
