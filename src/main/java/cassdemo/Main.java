package cassdemo;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import cassdemo.backend.BackendException;
import cassdemo.backend.BackendSession;

public class Main {

	public static void showCinemaHall (BackendSession session, String movieTitle, String date, int hour, int cinemaHall ) throws Exception {
		ArrayList reserved = session.selectReservedPlacesHallForMovieHour(movieTitle,date,hour, cinemaHall);
		for (int i = 1; i <= session.getCinemaHallPlaces(cinemaHall); i++) {
			if (reserved.contains(i))
				System.out.print(" X");
			else if (!reserved.contains(i))
				System.out.print(" " +i);
			if ( i %10 == 0)
				System.out.println("\n");
		}
	}

	private static final String PROPERTIES_FILENAME = "config.properties";

	public static void main(String[] args) throws Exception {
		String contactPoint = null;
		String keyspace = null;
		Scanner input = new Scanner(System.in);

		Properties properties = new Properties();
		try {
			properties.load(Main.class.getClassLoader().getResourceAsStream(PROPERTIES_FILENAME));

			contactPoint = properties.getProperty("contact_point");
			keyspace = properties.getProperty("keyspace");
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		BackendSession session = new BackendSession(contactPoint, keyspace);

		while (true) {
			// menu glowne
			System.out.println("\n--------------------------------");
			System.out.println("\n----Witaj w kinie Kinoswiat!----\n");
			System.out.print("--------------------------------\n" +
					"\nWybierz tryb uzytkowania systemu:\n" +
					"Administracja kina - wcisnij 1\nRezerwacja biletu  - wcisnij 2\nTesty               - wcisnij 3\nZakoncz            " +
					"- wcisnij 0\nTwoj wybor: ");
			// wybor trybu uzytkowania
			String mode = input.next();

			switch (mode) {
				// tryb administratora - dodawanie nowych filmow do kolekcji i seansow istniejacych filmow
				case "1":
					System.out.println("\n-------------Tryb administratora--------------");
					System.out.println("\nDodaj nowy film do kolekcji         - wcisnij 1");
					System.out.println("Dodaj nowy seans istniejacego filmu - wcisnij 2\n");
					switch (input.next()) {
						case "1":
							System.out.print("Podaj tytul nowego filmu dla repertuaru kina: ");
							String new_movie_title = input.next();
							session.insertNewMovie(new_movie_title);
							System.out.println("Pomyslnie dodano nowy film do kolekcji!\n");
							break;
						case "2":
							String movies = session.selectMovies();
							System.out.print("\nSeans ktorego filmu chcesz dodac:\n" + movies + "\nWybor: ");
							String movie_title = input.next();
							System.out.print("Ustal date wyswietlania filmu w formacie yyyy-mm-dd: ");
							String movie_date = input.next();
							System.out.print("Ustal godzine wyswietlania filmu (okragla godzina np. 15): ");
							int new_movie_hour = input.nextInt();
							System.out.print("Wybierz sale kinowa do wyswietlenia tego filmu: ");
							int new_movie_cinema_hall = input.nextInt();
							session.insertCinemaScreening(movie_title, movie_date, new_movie_hour, new_movie_cinema_hall);
							System.out.println("Pomyslnie dodano film do repertuaru\n");
							break;
					}
					break;

				// tryb rezerwacji biletow
				case "2":
					String filmy = session.selectMovies();
					System.out.print("\n-----Tryb rezerwacji biletow-----\n" +
							"Oto nasz repertuar filmowy:\n" + filmy + "\nNa jaki film chcesz zarezerwowac bilet: ");
					String movieTitle = input.next();
					System.out.println(session.selectAvailableMovieDates(movieTitle));
					if (session.selectAvailableMovies(movieTitle) == 0){
						System.out.println("Ten film obecnie nie jest dostepny w repertuarze");
						break;
					}
					System.out.print("Podaj date z podanych powyzej: ");
					String date = input.next();

					System.out.println(session.selectAvailableMovieHours(movieTitle, date));
					System.out.print("Podaj godzine z podanych powyzej: ");
					int hour = input.nextInt();

					int allPlaces = session.selectAllPlacesForThisMovieHour(movieTitle, date, hour);
					int reservedPlaces = session.selectReservedPlacesForThisMovieHour(movieTitle, date, hour);
					System.out.println("Udostepniono " +(allPlaces-reservedPlaces) + " miejsc\n");
					if (allPlaces - reservedPlaces <= 0)
						break;
					System.out.print("Wybierz sale kinowa: ");
					int placesVIP = session.getCinemaHallPlaces(1) - session.selectReservedCinemaHallPlacesForThisMovieHour(movieTitle, date, hour, 1);
					int placesStandard2 = session.getCinemaHallPlaces(2) - session.selectReservedCinemaHallPlacesForThisMovieHour(movieTitle, date, hour, 2);
					int placesStandard3 = session.getCinemaHallPlaces(3) - session.selectReservedCinemaHallPlacesForThisMovieHour(movieTitle, date, hour, 3);
					ArrayList<Integer> halls = session.selectCinemaHallForThisMovieHour(movieTitle, date, hour);
					if (!halls.contains(1))
						placesVIP = 0;
					if (!halls.contains(2))
						placesStandard2 = 0;
					if (!halls.contains(3))
						placesStandard3 = 0;
					System.out.println("Sala VIP " +placesVIP + " miejsc - wcisnij 1\nSala standard " +placesStandard2 + " miejsc - wcisnij 2\nSala standard " + placesStandard3 +" miejsc - wcisnij 3");
					int cinemaHall = input.nextInt();

					showCinemaHall(session, movieTitle, date, hour, cinemaHall);
					System.out.print("Ile biletow chcesz zarezerwowac: ");
					int tickets = input.nextInt();
					int place = 0;
					ArrayList<Integer> places = new ArrayList<>();
					for (int i = 0; i < tickets; i++) {
						System.out.print("Podaj nr miejsca: ");
						place = input.nextInt();
						places.add(place);
					}
					System.out.print("Podaj swoje imie: ");
					String imie = input.next();
					System.out.print("Podaj swoj adres e-mail: ");
					String email = input.next();

					ArrayList<Integer> placesOK = new ArrayList<>();
					System.out.println("Trwa przetwarzanie ...");
					int lastChoicePlace = 0;
					for (int i = 0; i < places.size(); i++) {
						int numberOfReservations = session.checkReservedPlace(movieTitle, date, hour, cinemaHall, places.get(i));
						if (numberOfReservations == 0)
							placesOK.add(places.get(i));
						lastChoicePlace = places.get(i);
						while (numberOfReservations == 1) {
							System.out.println("Miejsce " +lastChoicePlace + " jest juz zarezerwowane\n" +
									"Wybierz nowe miejsce\n");
							showCinemaHall(session, movieTitle, date, hour, cinemaHall);
							System.out.print("Podaj nr miejsca: ");
							place = input.nextInt();
							numberOfReservations = session.checkReservedPlace(movieTitle, date, hour, cinemaHall, place);
							if (numberOfReservations == 0)
								placesOK.add(place);
							else
								lastChoicePlace = place;
						}
					}
					ArrayList<UUID> uuids = new ArrayList<>();
					for (int i = 0; i < places.size(); i++) {
						UUID tmpUuid = UUID.randomUUID();
						uuids.add(tmpUuid);
						session.insertReservation(movieTitle, date, hour, cinemaHall, placesOK.get(i), uuids.get(i), imie, email);
					}
					// sprawdzenie czy na pewno zarezerwowalo miejsce
					int isReservedSuccessful = 0;
					for(int i=3; i>=1; i--){
						System.out.println(i+"...");
						TimeUnit.SECONDS.sleep(1);
					}
					for (int i = 0; i < placesOK.size(); i++) {
						isReservedSuccessful = session.checkInsertedReservation(movieTitle, date, hour, cinemaHall, placesOK.get(i), uuids.get(i));
						if(isReservedSuccessful == 1)
							System.out.println("Rezerwacja miejsca nr " +placesOK.get(i) + " sie powiodla! Zapraszamy na seans.");
						else {
							System.out.println("Rezerwacja sie niestety nie powiodla!");
							for (int j = 0; j < places.size(); j++)
								session.deleteReservation(movieTitle, date, hour, cinemaHall, placesOK.get(j), uuids.get(j));
						}
						}
					break;

				case "3":
					String movieTitleTest = "Nietykalni";
					String dateTest = "2020-02-03";
					int cinemaHallTest = 3;
					int hourTest = 10;
					int ticketsTest = 500;
					int lastChoicePlaceTest = 0;
					String emailTest = "test@test";
					String imieTest = "test";
					Random placeTest  = new Random();

					for (int i = 0; i < ticketsTest; i++) {
						UUID tmpUuidTest = UUID.randomUUID();
						int randomPlace = placeTest.nextInt(150)+1;
						int numberOfReservations = session.checkReservedPlace(movieTitleTest, dateTest, hourTest, cinemaHallTest, randomPlace);
						if (numberOfReservations == 0) {
							session.insertReservation(movieTitleTest, dateTest, hourTest, cinemaHallTest, randomPlace, tmpUuidTest, imieTest, emailTest);
							TimeUnit.MILLISECONDS.sleep(200);
							isReservedSuccessful = session.checkInsertedReservation2(movieTitleTest, dateTest, hourTest, cinemaHallTest, randomPlace, tmpUuidTest);
							if(isReservedSuccessful == 1)
								System.out.println("Rezerwacja miejsca nr " +randomPlace+ " sie powiodla! Zapraszamy na seans.");
							else {
								System.out.println("Rezerwacja " +randomPlace +" sie niestety nie powiodla! <wstepna rezerwacja byla OK>");
                                session.deleteReservation(movieTitleTest, dateTest, hourTest, cinemaHallTest, randomPlace, tmpUuidTest);
							}
						}
						else
                            System.out.println("Nie zarezerwowano miejsca " +randomPlace);
					}



					break;
				// koniec programu
				case "0":
					System.exit(0);

				// zla opcja, powrot do menu glownego
				default:
					System.out.println("Prosimy o wybranie poprawnej opcji!\n");
					break;
			}
		}
	}
}
