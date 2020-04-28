package uibk.ac.at.prodiga.tests.helper;

import org.mockito.internal.util.collections.Sets;
import uibk.ac.at.prodiga.model.Department;
import uibk.ac.at.prodiga.model.Team;
import uibk.ac.at.prodiga.model.User;
import uibk.ac.at.prodiga.model.UserRole;
import uibk.ac.at.prodiga.repositories.*;
import uibk.ac.at.prodiga.model.*;
import uibk.ac.at.prodiga.utils.badge.Badge;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.Temporal;
import java.util.Date;
import java.util.Random;
import java.util.Set;

public class DataHelper {

    public static String TEST_PASSWORD = "passwd";
    public static String TEST_PASSWORD_ENCODED = "$2a$10$d8cQ7Euz2hM43HOHWolUGeCEZSS/ltJVJYiJAmczl1X5FKzCjg6PC";

    /**
     * Creates a new random user with random username
     * @return The newly created username
     */
    public static User createRandomUser(UserRepository userRepository) {
        String username = createRandomString(30);

        return createUserWithRoles(username, Sets.newSet(UserRole.EMPLOYEE), userRepository);
    }

    /**
     * Creates an admin user with the given username
     * @param username User name
     * @param userRepository User repository to save user
     * @return The newly created user
     */
    public static User createAdminUser(String username, UserRepository userRepository) {
        return createUserWithRoles(username, Sets.newSet(UserRole.ADMIN), userRepository);
    }

    /**
     * Creates a user with the given user name and roles
     * @param username The username to use
     * @param roles The roles to use
     * @param userRepository The repository to use
     * @return The newly created user
     */
    public static User createUserWithRoles(String username, Set<UserRole> roles, UserRepository userRepository) {
        User u = new User();
        u.setUsername(username);
        u.setCreateDate(new Date());
        u.setRoles(roles);
        u.setCreateUser(null);
        u.setEmail("test@test.com");
        u.setId(username);
        u.setEnabled(true);
        u.setFirstName("Generic");
        u.setLastName("Namer");
        u.setPassword(TEST_PASSWORD_ENCODED);
        u.setUpdateDate(new Date());

        return userRepository.save(u);
    }

    /**
     * Creates a user with a random user name and roles, as well as a certain department and team
     * @param roles The roles to use
     * @param createUser the creation user for this user.
     * @param dept The department the user is in
     * @param team The team the user is in
     * @param userRepository The repository to use
     * @return The newly created user
     */
    public static User createUserWithRoles(Set<UserRole> roles, User createUser, Department dept, Team team, UserRepository userRepository)
    {
        return createUserWithRoles(createRandomString(30), roles, createUser, dept, team, userRepository);
    }

    /**
     * Creates a user with a specified user name and roles, as well as a certain department and team
     * @param username The username for this user.
     * @param roles The roles to use
     * @param createUser the creation user for this user.
     * @param dept The department the user is in
     * @param team The team the user is in
     * @param userRepository The repository to use
     * @return The newly created user
     */
    public static User createUserWithRoles(String username, Set<UserRole> roles, User createUser, Department dept, Team team, UserRepository userRepository)
    {
        User u = new User();
        u.setUsername(username);
        u.setCreateDate(new Date());
        u.setRoles(roles);
        u.setCreateUser(createUser);
        u.setEmail("test@test.com");
        u.setId(username);
        u.setEnabled(true);
        u.setAssignedDepartment(dept);
        u.setAssignedTeam(team);
        u.setFirstName("Generic");
        u.setLastName("Namer");
        u.setPassword(TEST_PASSWORD_ENCODED);
        u.setUpdateDate(new Date());

        return userRepository.save(u);
    }

    /**
     * Creates a random department
     * @param createUser The creation user of the department
     * @param departmentRepository The repository to save the department
     * @return The randomly generated department.
     */
    public static Department createRandomDepartment(User createUser, DepartmentRepository departmentRepository)
    {
        String name = createRandomString(30);

        Department dept = new Department();
        dept.setName(name);
        dept.setObjectCreatedUser(createUser);
        dept.setObjectCreatedDateTime(new Date());
        return departmentRepository.save(dept);
    }

    /**
     * Creates a random badge
     * @param user The user who receives the batch
     * @param badgeDBRepository The repository to save the badge.
     * @return The randomly generated badge.
     */
    public static BadgeDB createRandomBadge(User user, BadgeDBRepository badgeDBRepository)
    {
        String name = createRandomString(30);

        BadgeDB badgeDB = new BadgeDB();
        badgeDB.setBadgeName(name);
        badgeDB.setUser(user);
        badgeDB.setToDate(new Date());
        badgeDB.setFromDate(new Date());
        return badgeDBRepository.save(badgeDB);
    }

    /**
     * Creates a random team within a department
     * @param dept The department the team is in
     * @param createUser The creation user for the team
     * @param teamRepository The repository to save the team.
     * @return The ranomly generated team.
     */
    public static Team createRandomTeam(Department dept,  User createUser, TeamRepository teamRepository) {
        String name = createRandomString(30);

        Team team = new Team();
        team.setName(name);
        team.setObjectCreatedUser(createUser);
        team.setObjectCreatedDateTime(new Date());
        team.setDepartment(dept);

        return teamRepository.save(team);
    }

    /**
     * Creates a booking given the specified data and with a random task duration. Task duration will always lie in legal values (less than 7 days ago, less than 8 hours long, longer than 30 minutes)
     * @param category The category of the booking
     * @param createUser User who saves the activity
     * @param dice Dice which the activity is connected to
     * @param bookingRepository The repository to store the entry with.
     * @return The booking entry after being stored in the database.
     */
    public static Booking createBooking(BookingCategory category, User createUser, Dice dice, BookingRepository bookingRepository)
    {
        Random r = new Random();
        //offset for date endtime (from 0 minutes to (24*6)*60 minutes = 6 days ago)
        int offset = r.nextInt(8641);
        //duration of the task (from 30 minutes to 8*60 = 8 hours)
        int duration = r.nextInt(451) + 30;

        Date endDate = new Date(new Date().getTime() - offset * 60 * 1000);
        Date startDate = new Date(endDate.getTime() - duration * 60 * 1000);

        return createBooking(category, startDate, endDate, createUser, dice, bookingRepository);
    }

    /**
     * Creates a booking given the specified data and returns it
     * @param category The category of the booking
     * @param startDate Start of the activity
     * @param endDate End of the activity
     * @param createUser User who saves the activity
     * @param dice Dice which the activity is connected to
     * @param bookingRepository The repository to store the entry with.
     * @return The booking entry after being stored in the database.
     */
    public static Booking createBooking(BookingCategory category, Date startDate, Date endDate, User createUser, Dice dice, BookingRepository bookingRepository)
    {
        Booking booking = new Booking();
        booking.setActivityStartDate(startDate);
        booking.setActivityEndDate(endDate);
        booking.setDice(dice);
        booking.setBookingCategory(category);
        booking.setDept(dice.getUser().getAssignedDepartment());
        booking.setTeam(dice.getUser().getAssignedTeam());
        booking.setObjectCreatedDateTime(new Date());
        booking.setObjectCreatedUser(createUser);
        return bookingRepository.save(booking);
    }

    /**
     * Creates a vacation entry with a date relative to the current date.
     * This method does not do any validation on the input and simply stores what is given in the database.
     * Negative values can be used to create a vacation in the past.
     * @param daysFromNow the days from the current day until the vacation starts
     * @param toDaysFromNow the days from the current day until the vacation ends
     * @return The vacation as it was saved in the database.
     */
    public static Vacation createVacation(int daysFromNow, int toDaysFromNow, User forUser, VacationRepository vacationRepository)
    {
        LocalDate nowDate = LocalDate.now();
        LocalDate fromDate = nowDate.plusDays(daysFromNow);
        LocalDate toDate = nowDate.plusDays(toDaysFromNow);

        Vacation vacation = new Vacation();
        vacation.setBeginDate(Date.from(fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        vacation.setEndDate(Date.from(toDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        vacation.setUser(forUser);
        vacation.setObjectCreatedDateTime(new Date());
        vacation.setObjectCreatedUser(forUser);
        return vacationRepository.save(vacation);
    }

    /**
     * Creates a vacation entry with absolute date values.
     * This method does not do any validation on the input and simply stores what is given in the database.
     * @param startDate the starting date of the vacation
     * @param endDate the ending date of the vacation
     * @return The vacation as it was saved in the database.
     */
    public static Vacation createVacation(LocalDate startDate, LocalDate endDate, User forUser, VacationRepository vacationRepository)
    {
        Vacation vacation = new Vacation();
        vacation.setBeginDate(Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        vacation.setEndDate(Date.from(endDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        vacation.setUser(forUser);
        vacation.setObjectCreatedDateTime(new Date());
        vacation.setObjectCreatedUser(forUser);
        return vacationRepository.save(vacation);
    }

     /**
      * Creates a given dice with the given data
     * @param internalId The internal Id used by the dice (and the raspi if not exists)
     * @param raspi The raspi may be null
     * @param u The user which creates all objects
     * @param diceRepository The Repository to save the dice
     * @param raspberryPiRepository The Repository to save the raspi
     * @param roomRepository The Repository to save the room
     * @return The newly created Dice
     */
    public static Dice createDice(String internalId,
                                  RaspberryPi raspi,
                                  User u,
                                  DiceRepository diceRepository,
                                  RaspberryPiRepository raspberryPiRepository,
                                  RoomRepository roomRepository) {
        if(raspi == null) {
            raspi = createRaspi(internalId, u, null, raspberryPiRepository, roomRepository);
        }

        Dice d = new Dice();
        d.setAssignedRaspberry(raspi);
        d.setInternalId(internalId);
        d.setObjectChangedDateTime(new Date());
        d.setObjectChangedUser(u);
        d.setObjectCreatedDateTime(new Date());
        d.setObjectCreatedUser(u);
        d.setUser(u);

        return diceRepository.save(d);
    }

    /**
     *  Creates a given dice with the given data
     * @param internalId The internal Id used by the dice (and the raspi if not exists)
     * @param raspi The raspi may be null
     * @param createUser The user which creates all object
     * @param diceUser The user which is assigned to the dice
     * @param diceRepository The Repository to save the dice
     * @param raspberryPiRepository The Repository to save the raspi
     * @param roomRepository The Repository to save the room
     * @return The newly created Dice
     */
    public static Dice createDice(String internalId,
                                  RaspberryPi raspi,
                                  User createUser,
                                  User diceUser,
                                  DiceRepository diceRepository,
                                  RaspberryPiRepository raspberryPiRepository,
                                  RoomRepository roomRepository) {
        if(raspi == null) {
            raspi = createRaspi(internalId, createUser, null, raspberryPiRepository, roomRepository);
        }

        Dice d = new Dice();
        d.setAssignedRaspberry(raspi);
        d.setInternalId(internalId);
        d.setObjectCreatedDateTime(new Date());
        d.setObjectCreatedUser(createUser);
        d.setObjectChangedDateTime(new Date());
        d.setObjectChangedUser(createUser);
        d.setUser(diceUser);

        return diceRepository.save(d);
    }

    /**
     * Creates a raspi with the given internal Id
     * @param internalId The internal ID to use
     * @param raspberryPiRepository The repository to save the raspi
     * @param u User which created the raspi
     * @param r Room in which the raspi gets saved can be null
     * @return The saved raspi
     */
    public static RaspberryPi createRaspi(String internalId,
                                          User u,
                                          Room r,
                                          RaspberryPiRepository raspberryPiRepository,
                                          RoomRepository roomRepository) {
        if(r == null) {
            r = createRoom(createRandomString(20), u, roomRepository);
        }

        RaspberryPi raspi = new RaspberryPi();
        raspi.setInternalId(internalId);
        raspi.setPassword(TEST_PASSWORD_ENCODED);
        raspi.setObjectChangedDateTime(new Date());
        raspi.setObjectCreatedDateTime(new Date());
        raspi.setObjectChangedUser(u);
        raspi.setObjectCreatedUser(u);
        raspi.setAssignedRoom(r);

        return raspberryPiRepository.save(raspi);
    }

    /**
     * Creates a new room with the given user and the given name
     * @param name Rooms name
     * @param u  user which created the room
     * @param roomRepository Repository used to save the room
     * @return The newly created room
     */
    public static Room createRoom(String name, User u, RoomRepository roomRepository) {
        Room r = new Room();
        r.setName(name);
        r.setObjectChangedDateTime(new Date());
        r.setObjectChangedUser(u);
        r.setObjectCreatedDateTime(new Date());
        r.setObjectCreatedUser(u);

        return roomRepository.save(r);
    }

    public static BookingCategory createBookingCategory(String name, User u, Set<Team> teams,
                                                        BookingCategoryRepository bookingCategoryRepository) {
        BookingCategory cat = new BookingCategory();
        cat.setName(name);
        cat.setTeams(teams);
        cat.setObjectCreatedUser(u);
        cat.setObjectCreatedDateTime(new Date());

        return bookingCategoryRepository.save(cat);
    }

    public static BookingCategory createBookingCategory(String name, User u,
                                                 BookingCategoryRepository bookingCategoryRepository) {
        return createBookingCategory(name, u, null, bookingCategoryRepository);
    }

    private static String createRandomString(int size) {
        String alphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "0123456789"
                + "abcdefghijklmnopqrstuvxyz";


        StringBuilder sb = new StringBuilder(size);

        for (int i = 0; i < size; i++) {
            int index = (int)(alphaNumericString.length() * Math.random());

            sb.append(alphaNumericString.charAt(index));
        }

        return sb.toString();
    }

}
