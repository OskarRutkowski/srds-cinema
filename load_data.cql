create keyspace Kino with replication = {'class': 'SimpleStrategy', 'replication_factor': 1 };
use kino;

create table movies (movie_title text, primary key(movie_title));
insert into movies (movie_title ) values ('Gladiator');
insert into movies (movie_title ) values ('Nietykalni');
insert into movies (movie_title ) values ('Smerfy');
insert into movies (movie_title ) values ('Psy');

create table cinema_halls (cinema_hall_number int, places_number int, hall_type text, primary key(cinema_hall_number ));
INSERT INTO cinema_halls (cinema_hall_number , places_number , hall_type ) VALUES ( 1,50,'VIP');
INSERT INTO cinema_halls (cinema_hall_number , places_number , hall_type ) VALUES ( 2,100,'standard');
INSERT INTO cinema_halls (cinema_hall_number , places_number , hall_type ) VALUES ( 3,150,'standard');

create table cinema_screenings (movie_title text, date date, hour int, cinema_hall int, primary key(movie_title , date , hour, cinema_hall));
insert into cinema_screenings (movie_title , date , hour , cinema_hall ) VALUES ( 'Psy', '2020-02-02', 10, 1);
insert into cinema_screenings (movie_title , date , hour , cinema_hall ) VALUES ( 'Psy', '2020-02-02', 10, 2);
insert into cinema_screenings (movie_title , date , hour , cinema_hall ) VALUES ( 'Psy', '2020-02-02', 10, 3);
insert into cinema_screenings (movie_title , date , hour , cinema_hall ) VALUES ( 'Psy', '2020-02-02', 12, 3);
insert into cinema_screenings (movie_title , date , hour , cinema_hall ) VALUES ( 'Smerfy', '2020-02-02', 18, 3);
insert into cinema_screenings (movie_title , date , hour , cinema_hall ) VALUES ( 'Nietykalni', '2020-02-03', 10, 3);

create table reservations (movie_title text, date date, hour int, cinema_hall int, row int, number_in_row int, res_id uuid, name text, email text, primary key(movie_title , date , hour , cinema_hall , row , number_in_row, res_id ));


