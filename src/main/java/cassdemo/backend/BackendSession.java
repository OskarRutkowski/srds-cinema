package cassdemo.backend;

import com.datastax.driver.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.*;

/*
 * For error handling done right see: 
 * https://www.datastax.com/dev/blog/cassandra-error-handling-done-right
 * 
 * Performing stress tests often results in numerous WriteTimeoutExceptions, 
 * ReadTimeoutExceptions (thrown by Cassandra replicas) and 
 * OpetationTimedOutExceptions (thrown by the client). Remember to retry
 * failed operations until success (it can be done through the RetryPolicy mechanism:
 * https://stackoverflow.com/questions/30329956/cassandra-datastax-driver-retry-policy )
 */

public class BackendSession {

	private static final Logger logger = LoggerFactory.getLogger(BackendSession.class);

	private Session session;

	public BackendSession(String contactPoint, String keyspace) throws BackendException {

		Cluster cluster = Cluster.builder().addContactPoint(contactPoint).build();
		try {
			session = cluster.connect(keyspace);
		} catch (Exception e) {
			throw new BackendException("Could not connect to the cluster. " + e.getMessage() + ".", e);
		}
	}

	public String selectMovies() throws BackendException {
		StringBuilder builder = new StringBuilder();

		ResultSet rs = null;

		try {
			rs = session.execute("SELECT * FROM movies;");
		} catch (Exception e) {
			throw new BackendException("Could not perform a query. " + e.getMessage() + ".", e);
		}

		for (Row row : rs) {
			String movie_title = row.getString("movie_title");
			builder.append(String.format("%s\n", movie_title));
		}

		return builder.toString();
	}

	public String selectAvailableMovieDates(String movieTitle) throws BackendException, ParseException {
		StringBuilder builder = new StringBuilder();
		ResultSet rs = null;
		ResultSet rs2 = null;

		try {
			rs = session.execute("select date from cinema_screenings where movie_title='"  +movieTitle +"';");
		} catch (Exception e) {
			throw new BackendException("Could not perform a query. " + e.getMessage() + ".", e);
		}

		Set<String> dates = new TreeSet<>();
		for (Row row : rs) {
			String date = row.getDate("date").toString();
			if (!dates.contains(date)) {
				dates.add(date);
				builder.append(String.format("Dostepna data %s\n", date));
			}
		}
		return builder.toString();
	}

	public int selectAvailableMovies(String movieTitle) throws BackendException, ParseException {
		ResultSet rs = null;

		try {
			rs = session.execute("select count(*) from cinema_screenings where movie_title='"  +movieTitle +"';");
		} catch (Exception e) {
			throw new BackendException("Could not perform a query. " + e.getMessage() + ".", e);
		}

		int cnt = 0;
		for (Row row : rs) {
			cnt = (int)row.getLong("count");
		}
		return cnt;
	}

	public String selectAvailableMovieHours(String movieTitle, String date) throws BackendException {
		StringBuilder builder = new StringBuilder();
		ResultSet rs = null;

		try {
			rs = session.execute("select hour from cinema_screenings where movie_title='"  +movieTitle +"'" +
					"and date='" +date +"';");
		} catch (Exception e) {
			throw new BackendException("Could not perform a query. " + e.getMessage() + ".", e);
		}

		Set<Integer> hours = new TreeSet<>();
		for (Row row : rs) {
			int hour = row.getInt("hour");
			if (!hours.contains(hour)) {
				hours.add(hour);
				builder.append(String.format("Dostepna godzina %d\n", hour));
			}
		}
		return builder.toString();
	}

	public int selectAllPlacesForThisMovieHour(String movieTitle, String date, int hour) throws BackendException {
		ResultSet rs = null;

		try {
			rs = session.execute("select cinema_hall from cinema_screenings where movie_title='"  +movieTitle +"'" +
					"and date='" +date +"'" +"and hour=" +hour +";");
		} catch (Exception e) {
			throw new BackendException("Could not perform a query. " + e.getMessage() + ".", e);
		}

		int places = 0;
		for (Row row : rs) {
			int cinemaHallNumber = row.getInt("cinema_hall");
			places += getCinemaHallPlaces(cinemaHallNumber);
		}
		return places;
	}

	public int selectReservedCinemaHallPlacesForThisMovieHour(String movieTitle, String date, int hour, int hallNumber) throws BackendException {
		ResultSet rs = null;

		try {
			rs = session.execute("select count(*) from reservations where movie_title='"  +movieTitle +"'" +
					"and date='" +date +"'" +"and hour=" +hour +" and cinema_hall=" +hallNumber +";");
		} catch (Exception e) {
			throw new BackendException("Could not perform a query. " + e.getMessage() + ".", e);
		}

		int reservedPlaces = 0;
		for (Row row : rs) {
			reservedPlaces = (int)row.getLong("count");
		}
		return reservedPlaces;
	}

	public ArrayList<Integer> selectCinemaHallForThisMovieHour(String movieTitle, String date, int hour) throws BackendException {
		ResultSet rs = null;

		try {
			rs = session.execute("select cinema_hall from cinema_screenings where movie_title='"  +movieTitle +"'" +
					"and date='" +date +"'" +"and hour=" +hour  +";");
		} catch (Exception e) {
			throw new BackendException("Could not perform a query. " + e.getMessage() + ".", e);
		}

		ArrayList<Integer> halls = new ArrayList<>();
		for (Row row : rs) {
			int hall = row.getInt("cinema_hall");
			halls.add(hall);
		}
		return halls;
	}

	public int selectReservedPlacesForThisMovieHour(String movieTitle, String date, int hour) throws BackendException {
		ResultSet rs = null;

		try {
			rs = session.execute("select count(*) from reservations where movie_title='"  +movieTitle +"'" +
					"and date='" +date +"'" +"and hour=" +hour +";");
		} catch (Exception e) {
			throw new BackendException("Could not perform a query. " + e.getMessage() + ".", e);
		}

		int reservedPlaces = 0;
		for (Row row : rs) {
			reservedPlaces = (int)row.getLong("count");
		}
		return reservedPlaces;
	}

	public int checkReservedPlace(String movieTitle, String date, int hour, int cinemaHall, int place) throws BackendException {
		ResultSet rs = null;
		int row2 = place/10 + 1;
		int number = place%10;

		try {
			rs = session.execute("select count(*) from reservations where movie_title='"  +movieTitle +"'" +
					"and date='" +date +"'" +"and hour=" +hour +" and cinema_hall=" + cinemaHall +" and " +
					"row = " +row2 +" and number_in_row=" +number +";");
		} catch (Exception e) {
			throw new BackendException("Could not perform a query. " + e.getMessage() + ".", e);
		}

		int numberOfReservations = 0;
		for (Row row : rs) {
			numberOfReservations = (int)row.getLong("count");
		}
		return numberOfReservations;
	}

	public int checkInsertedReservation(String movieTitle, String date, int hour, int cinemaHall, int place, UUID uuid) throws BackendException {
		ResultSet rs = null;

		int row2 = place/10 + 1;
		int number = place%10;

		try {
			rs = session.execute("select count(*) from reservations where movie_title='"  +movieTitle +"' " +
					" and date='" +date +"'" +" and hour=" +hour +" and cinema_hall="+cinemaHall+" and row="+row2+" and number_in_row="+number+
					" and res_id=" +uuid+" ;");
		} catch (Exception e) {
			throw new BackendException("Could not perform a query. " + e.getMessage() + ".", e);
		}

		int isReservationActive = 0;
		for (Row row : rs) {
			isReservationActive = (int)row.getLong("count");
		}
		return isReservationActive;
	}

	public int checkInsertedReservation2(String movieTitle, String date, int hour, int cinemaHall, int place, UUID uuid) throws BackendException {
		ResultSet rs = null;

		int row2 = place/10 + 1;
		int number = place%10;

		try {
			rs = session.execute("select count(*) from reservations where movie_title='"  +movieTitle +"' " +
					" and date='" +date +"'" +" and hour=" +hour +" and cinema_hall="+cinemaHall+" and row="+row2+" and number_in_row="+number+";");
		} catch (Exception e) {
			throw new BackendException("Could not perform a query. " + e.getMessage() + ".", e);
		}

		int cnt = 0;
		for (Row row : rs) {
			cnt = (int)row.getLong("count");
		}

		if (cnt > 1){
			ResultSet rs2 = null;
			System.out.println("ZNALEZIONO PODWOJNA REZERWACJE\n\n");
			try {
				rs2 = session.execute("select writetime(name), res_id from reservations where movie_title='"  +movieTitle +"' " +
						" and date='" +date +"'" +" and hour=" +hour +" and cinema_hall="+cinemaHall+" and row="+row2+" and number_in_row="+number+";");
			} catch (Exception e) {
				throw new BackendException("Could not perform a query. " + e.getMessage() + ".", e);
			}
			ArrayList<Long> timestamps = new ArrayList<>();
			ArrayList<UUID> uuids2 = new ArrayList<>();
			for (Row row : rs2) {
				Long timestamp = row.getLong("writetime(name)");
				UUID uuid2 = row.getUUID("res_id");
				timestamps.add(timestamp);
				uuids2.add(uuid2);
			}
			int firstReservation = timestamps.indexOf(Collections.min(timestamps));

			ResultSet rs3 = null;
			for (int i = 0; i < timestamps.size(); i++) {
				if (i != firstReservation) {
					try {
						rs2 = session.execute("delete from reservations where movie_title='"  +movieTitle +"'" +
								" and date='" +date +"'" +" and hour=" +hour +" and cinema_hall="+cinemaHall+" and row="+row2+
								" and number_in_row= " +number +" and res_id=" +uuids2.get(i) +";");
					} catch (Exception e) {
						throw new BackendException("Could not perform a query. " + e.getMessage() + ".", e);
					}
				}
			}
		}
		return 1;
	}

	public int whichReservationIsFirst(String movieTitle, String date, int hour) throws BackendException {
		ResultSet rs = null;

		try {
			rs = session.execute("select count(*) from reservations where movie_title='"  +movieTitle +"'" +
					"and date='" +date +"'" +"and hour=" +hour +";");
		} catch (Exception e) {
			throw new BackendException("Could not perform a query. " + e.getMessage() + ".", e);
		}

		int reservedPlaces = 0;
		for (Row row : rs) {
			reservedPlaces = (int)row.getLong("count");
		}
		return reservedPlaces;
	}

	public void insertReservation(String movieTitle, String date, int hour, int cinemaHall, int place, UUID uuid, String name, String email) throws BackendException {
		int row2 = place/10 + 1;
		int number = place%10;
		try {
			session.execute("insert into reservations (movie_title, date, hour, cinema_hall, row, number_in_row, res_id, name, email)" +
					" values ('" + movieTitle +"','" + date +"'," +hour +"," + cinemaHall + "," +row2 +","+
					number +"," + uuid +",' " +name +"','"+email+"');");
		} catch (Exception e) {
			throw new BackendException("Could not perform an insert. " + e.getMessage() + ".", e);
		}
	}

	public void insertNewMovie(String movieTitle) throws BackendException {
		try {
			session.execute("insert into movies (movie_title) values ('" + movieTitle +"');");
		} catch (Exception e) {
			throw new BackendException("Could not perform an insert. " + e.getMessage() + ".", e);
		}
	}

	public void insertCinemaScreening(String movieTitle, String date, int hour, int cinemaHall) throws BackendException {

		try {
			session.execute("insert into cinema_screenings (movie_title, date, hour, cinema_hall)" +
					" values ('" + movieTitle +"','" + date +"'," +hour +"," + cinemaHall +");");
		} catch (Exception e) {
			throw new BackendException("Could not perform an insert. " + e.getMessage() + ".", e);
		}
	}

	public ArrayList<Integer> selectReservedPlacesHallForMovieHour(String movieTitle, String date, int hour, int cinemaHall) throws BackendException {
		ResultSet rs = null;

		try {
			rs = session.execute("select * from reservations where movie_title='"  +movieTitle +"'" +
					"and date='" +date +"'" +"and hour=" +hour +" and cinema_hall=" + cinemaHall +";");
		} catch (Exception e) {
			throw new BackendException("Could not perform a query. " + e.getMessage() + ".", e);
		}

		int row2 = 0, number = 0;
		ArrayList<Integer> places = new ArrayList<>();
		for (Row row : rs) {
			row2 = row.getInt("row");
			number = row.getInt("number_in_row");
			places.add((row2-1)*10+number);
		}
		return places;
	}

	public int getCinemaHallPlaces(int cinemaHallNumber) throws BackendException {
		ResultSet rs = null;

		try {
			rs = session.execute("select places_number from cinema_halls where cinema_hall_number="  +cinemaHallNumber +";");
		} catch (Exception e) {
			throw new BackendException("Could not perform a query. " + e.getMessage() + ".", e);
		}

		int places_number = 0;
		for (Row row : rs) {
			places_number = row.getInt("places_number");
		}
		return places_number;
	}

	public void deleteReservation(String movieTitle, String date, int hour, int cinemaHall, int place, UUID uuid) throws BackendException {
		int row2 = place/10 + 1;
		int number = place%10;
		try {
			session.execute("delete from reservations where movie_title='"  +movieTitle +"' " +
					" and date='" +date +"'" +" and hour=" +hour +" and cinema_hall="+cinemaHall+" and row="+row2+" and number_in_row="+number+
					" and res_id=" +uuid +";");
		} catch (Exception e) {
			throw new BackendException("Could not perform an insert. " + e.getMessage() + ".", e);
		}
	}

	public int selectCinemaScreenings(String movieTitle) throws BackendException {
		ResultSet rs = null;

		try {
			rs = session.execute("select count(*) from cinema_screenings where movie_title='"  +movieTitle +"';");
		} catch (Exception e) {
			throw new BackendException("Could not perform a query. " + e.getMessage() + ".", e);
		}

		int liczba_wejsciowek=0;
		for (Row row : rs) {
			liczba_wejsciowek = (int)row.getLong("count");
		}
		return liczba_wejsciowek;
	}

	protected void finalize() {
		try {
			if (session != null) {
				session.getCluster().close();
			}
		} catch (Exception e) {
			logger.error("Could not close existing cluster", e);
		}
	}

}
