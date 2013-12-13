--Group 30

--Q1 Trigger
--This trigger stops players from playing for several teams in the same season
create trigger oneTeam before insert on playsfor \
referencing new as n \
for each row \
    when (n.syear in (select syear from playsfor where pid = n.pid and sport = n.sport and llevel = n.llevel)) \
        SIGNAL SQLSTATE '75000' SET MESSAGE_TEXT='Cannot play twice in same season'
		
--Demonstration:
--Here are players on teams in the seasons for water polo, 2013:
db2 => select * from playsfor where sport = 'Innertube Water Polo' and syear = 2013

PID         NAME                           SYEAR       SPORT                          LLEVEL
----------- ------------------------------ ----------- ------------------------------ ------
  260402399 Class and Pizzaz                      2013 Innertube Water Polo           CRB
  260403842 Marco Pullouts                        2013 Innertube Water Polo           CRB
  260409387 Marco Pullouts                        2013 Innertube Water Polo           CRB
  260411234 Marco Pullouts                        2013 Innertube Water Polo           CRB
  260413685 Marco Pullouts                        2013 Innertube Water Polo           CRB
  260444112 Marco Pullouts                        2013 Innertube Water Polo           CRB
  260457690 Marco Pullouts                        2013 Innertube Water Polo           CRB
  260479834 Marco Pullouts                        2013 Innertube Water Polo           CRB

  8 record(s) selected.

--Adding a player successfully:
db2 => insert into PlaysFor values (260414521, 'Marco Pullouts', 2013, 'Innertube Water Polo', 'CRB')
DB20000I  The SQL command completed successfully.

--Trying to add the player from 'Class and Pizzaz' into the 'Marco Pullouts' team:
db2 => insert into PlaysFor values (260402399, 'Marco Pullouts', 2013, 'Innertube Water Polo', 'CRB')
DB21034E  The command was processed as an SQL statement because it was not a
valid Command Line Processor command.  During SQL processing it returned:
SQL0438N  Application raised error with diagnostic text: "Cannot play twice in
same season".  SQLSTATE=75000


		
--Q2 Stored Procedure
--A procedure for quick clean-up of tables
--Two possible options, 'season' and 'team'
--When 'season' is given, all seasons where the registration deadline has passed
--but there are still less than 4 teams, are deleted
--When 'team' is given, all teams where there are fewer players than the minimum
--are deleted. This is meant to be called once all deadlines have passed.
create procedure clean (in coption varchar(20)) \
language sql \
begin \
--Variables are declared
declare syear int; \
declare sport varchar(30); \
declare llevel varchar(3); \
declare regdeadline date; \
declare counter int; \
declare maximum int; \
declare name varchar(30); \
--The seasonal cursor
declare thisSeason cursor for \
select syear, sport, llevel, regdeadline from season; \
--The team cursor
declare thisTeam cursor for \
select name, syear, sport, llevel from team; \
--The follwing occurs when the 'season' option is given
if coption = 'season' then \
--Set the maximum as the number of seasons in the table
select count(*) into maximum from season; \
--Set counter to zero
set counter = 0; \
--Open the cursor
open thisSeason; \
--Being a loop
fetch_loop1: \
loop \
--If we have already seen all of the rows of the season table, exit the loop
if counter = maximum then \
leave fetch_loop1; \
end if; \
--Retrieve the next row of the season table
fetch thisSeason into syear, sport, llevel, regdeadline; \
--Check to see if the registration deadline has passed
if regdeadline < current_date then \
--Check to see how many teams are in that season
if ((syear, sport, llevel) not in (select t.syear, t.sport, t.llevel \
from team t group by t.syear, t.sport, t.llevel \
having count(*) >= 4)) then \
--Delete if two few teams and passed the deadline
delete from season where current of thisSeason; \
end if; \
end if; \
--Increment counter
set counter = counter + 1; \
end loop; \
--Closing cursor
close thisSeason; \
--This occurs is 'team' option is given
else if coption = 'team' then \
--Setting the maximum to the total number of teams
select count(*) into maximum from team; \
--Setting the counter to zero
set counter = 0; \
--Opening team cursor
open thisTeam; \
--Starting loop
fetch_loop2: \
loop \
--If the whole table has been seen, exit the loop
if counter = maximum then \
leave fetch_loop2; \
end if; \
--Retrieve next team
fetch thisTeam into name, syear, sport, llevel; \
--Check to see if the number of players is less than the minimum for the given league
if ((name, syear, sport, llevel) not in (select p.name, p.syear, p.sport, p.llevel \
from playsfor p group by p.name, p.syear, p.sport, p.llevel \
having count (*) >= (select min(minplayers) from league \
where league.sport = p.sport and league.llevel = p.llevel))) then \
--Delete if the number of players is too small
delete from team where current of thisTeam; \
end if; \
--Increment counter
set counter = counter + 1; \
end loop; \
--Close cursor
close thisTeam; \
end if; \
end if; \
end

--Demonstration:
--Cleaning of Season:
--Here is the list of seasons:
db2 => select * from season

SYEAR       SPORT                          LLEVEL REGDEADLINE MAXTEAMS
----------- ------------------------------ ------ ----------- -----------
       2013 Indoor Soccer                  WB     01/05/2013           30
       2012 Ice Hockey                     MC     09/03/2012           26
       2011 Ice Hockey                     MC     08/31/2011           26
       2012 Ice Hockey                     CRD    09/03/2012           16
       2011 Ice Hockey                     CRD    08/31/2012           16
       2013 Innertube Water Polo           CRB    12/17/2012           20
       2013 2v2 Soccer                     MB     02/21/2013           24
       2013 Innertube Water Polo           CRA    12/17/2012           20
       2012 2v2 Soccer                     MB     01/30/2012           24
       2013 2v2 Soccer                     CRA    02/21/2013           24
       2013 2v2 Soccer                     WB     02/21/2013           24
       2012 Innertube Water Polo           CRB    12/08/2011           20
       2013 Ice Hockey                     MC     09/03/2013           26
       2011 Innertube Water Polo           CRB    12/06/2010           20

  14 record(s) selected.

--Here is a query for only those seasons that have at least 4 teams associated with them:
db2 => select * from Season S where (S.syear, S.sport, S.llevel) \
in (select T.syear, T.sport, T.llevel from Team T \
group by T.syear, T.sport, T.llevel having count(*) >=4)db2 (cont.) => db2 (cont.) =>

SYEAR       SPORT                          LLEVEL REGDEADLINE MAXTEAMS
----------- ------------------------------ ------ ----------- -----------
       2013 Innertube Water Polo           CRB    12/17/2012           20
       2012 2v2 Soccer                     MB     01/30/2012           24

  2 record(s) selected.

--Now we will clean the list of seasons:
  db2 => call clean ('season')

  Return Status = 0

--Let us look at what is left in the list of seasons:
db2 => select * from season

SYEAR       SPORT                          LLEVEL REGDEADLINE MAXTEAMS
----------- ------------------------------ ------ ----------- -----------
       2013 Innertube Water Polo           CRB    12/17/2012           20
       2012 2v2 Soccer                     MB     01/30/2012           24
       2013 Ice Hockey                     MC     09/03/2013           26

  3 record(s) selected.

--As desired, only seasons where the registration deadline has not passed, or those with at
--least 4 teams registered remain.

--Cleaning of Team:
--Here is the list of teams:
NAME                           SYEAR       SPORT                          LLEVEL
------------------------------ ----------- ------------------------------ ------
Bar Down                              2013 2v2 Soccer                     CRA
Class and Pizzaz                      2011 Innertube Water Polo           CRB
Class and Pizzaz                      2013 2v2 Soccer                     CRA
Class and Pizzaz                      2013 Innertube Water Polo           CRB
Cool Runnings                         2011 Ice Hockey                     MC
Cool Runnings                         2012 Ice Hockey                     MC
FC Girls                              2013 Indoor Soccer                  WB
Far Post                              2012 2v2 Soccer                     MB
Far Post                              2013 2v2 Soccer                     MB

...

Thunderhawks                          2012 Ice Hockey                     CRD
soccority                             2013 Indoor Soccer                  WB

  25 record(s) selected.
  
--Here is a query showing only those teams which satisfy the
--minimal player requirements of the given league:
db2 => select * from Team T where (T.name, T.syear, T.sport, T.llevel) in \
db2 (cont.) => (select P.name, P.syear, P.sport, P.llevel from playsfor P \
db2 (cont.) => group by P.name, P.syear, P.sport, P.llevel \
db2 (cont.) => having count(*) >= (select min(minplayers) from league \
db2 (cont.) => where league.sport = P.sport and league.llevel = P.llevel))

NAME                           SYEAR       SPORT                          LLEVEL
------------------------------ ----------- ------------------------------ ------
Class and Pizzaz                      2013 2v2 Soccer                     CRA
Far Post                              2012 2v2 Soccer                     MB
Far Post                              2013 2v2 Soccer                     MB
Marco Pullouts                        2013 Innertube Water Polo           CRB

  4 record(s) selected.

--We perform the clean:
db2 => call clean ('team')

  Return Status = 0

--Here is the result:
db2 => select * from team

NAME                           SYEAR       SPORT                          LLEVEL
------------------------------ ----------- ------------------------------ ------
Class and Pizzaz                      2013 2v2 Soccer                     CRA
Far Post                              2012 2v2 Soccer                     MB
Far Post                              2013 2v2 Soccer                     MB
Marco Pullouts                        2013 Innertube Water Polo           CRB

  4 record(s) selected.

--Q4 Indices
--Index for team on sport, llevel, syear
--This is a useful index because only equality searches occur on the Team table, and so the
--index will always be useful. Further, it is rare that a search will occur on the team
--table and not ask for the sport, since the sport is probably the most important of the attributes.
--Further, the sport, llevel and syear attributes are also used in many other tables. Sport and
--llevel on their own are used in even more tables. For these reasons, queries with teams are
--likely to occur often. Finally, it seems like for an intramural database, selections on teams
--will occur likely, whether it is to inform a set of teams of a game cancelation, or whether all
--teams are being retrieved for schedule construction.
create index team1 on team(sport, llevel, syear) cluster

--Index for game on gdate, gtime
--This is a useful index because often games have to be re-scheduled, or else the team captains
--involved in a specific subset of games have to be informed of some sort of change. Further,
--since the intramurals are all about games, game-related queries are likely to be common.
create index game1 on game(gdate, gtime) cluster
