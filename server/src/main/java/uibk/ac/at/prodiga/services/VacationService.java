package uibk.ac.at.prodiga.services;

import com.google.common.collect.Lists;
import de.jollyday.Holiday;
import de.jollyday.HolidayCalendar;
import de.jollyday.HolidayManager;
import de.jollyday.ManagerParameters;
import org.springframework.context.annotation.Scope;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import uibk.ac.at.prodiga.model.Booking;
import uibk.ac.at.prodiga.model.User;
import uibk.ac.at.prodiga.model.UserRole;
import uibk.ac.at.prodiga.model.Vacation;
import uibk.ac.at.prodiga.repositories.BookingCategoryRepository;
import uibk.ac.at.prodiga.repositories.BookingRepository;
import uibk.ac.at.prodiga.repositories.VacationRepository;
import uibk.ac.at.prodiga.utils.Constants;
import uibk.ac.at.prodiga.utils.MessageType;
import uibk.ac.at.prodiga.utils.ProdigaGeneralExpectedException;
import uibk.ac.at.prodiga.utils.ProdigaUserLoginManager;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * Service for accessing and manipulating vacations.
 */
@Component
@Scope("application")
public class VacationService
{
    private final VacationRepository vacationRepository;
    private final ProdigaUserLoginManager userLoginManager;
    private final BookingRepository bookingRepository;
    private final BookingCategoryRepository bookingCategoryRepository;
    private final LogInformationService logInformationService;

    public VacationService(VacationRepository vacationRepository, ProdigaUserLoginManager userLoginManager, BookingRepository bookingRepository, BookingCategoryRepository bookingCategoryRepository, LogInformationService logInformationService)
    {
        this.vacationRepository = vacationRepository;
        this.userLoginManager = userLoginManager;
        this.bookingRepository = bookingRepository;
        this.logInformationService = logInformationService;
        this.bookingCategoryRepository = bookingCategoryRepository;
    }

        /**
         * Returns a collection of all the users
         * @return A collection of all vacations of the user calling the method
         */
    @PreAuthorize("hasAuthority('EMPLOYEE')") //NOSONAR
    public Collection<Vacation> getAllVacations()
    {
        User u = userLoginManager.getCurrentUser();
        return Lists.newArrayList(vacationRepository.findAllByUser(u));
    }

    /**
     * Returns all vacation by the given user
     * @param u The user
     * @return A list with vacations
      */
    @PreAuthorize("hasAuthority('ADMIN') or principal.username eq #u.username")
    public Collection<Vacation> getAllVacationsByUser(User u) {
        return vacationRepository.findAllByUser(u);
    }

    /**
     * Saves a vacation in the database. If an object with this ID already exists, overwrites the object's data at this ID
     * @param vacation The vacation to save
     * @return The new state of the object in the database.
     * @throws ProdigaGeneralExpectedException Is thrown when data is not valid, e.g. dates do not work out or vacation user is not currently logged in user.
     */
    @PreAuthorize("hasAuthority('EMPLOYEE')") //NOSONAR
    public Vacation saveVacation(Vacation vacation) throws ProdigaGeneralExpectedException
    {
        User u = userLoginManager.getCurrentUser();

        //Make sure user is correct
        if(!vacation.getUser().equals(u))
        {
            throw new RuntimeException("Attempted to save vacation of different user.");
        }

        //check vacation start time
        if(vacation.getEndDate().before(vacation.getBeginDate()))
        {
            throw new ProdigaGeneralExpectedException("Vacation cannot end before it begins.", MessageType.ERROR);
        }

        if(vacation.getBeginDate().before(new Date()))
        {
            throw new ProdigaGeneralExpectedException("Vacation cannot be set for the past.", MessageType.ERROR);
        }

        //check vacation end time
        LocalDate maxDate = LocalDate.of(LocalDate.now().getYear()+1,12,31);
        if(toLocalDate(vacation.getEndDate()).isAfter(maxDate))
        {
            throw new ProdigaGeneralExpectedException("Vacations can only be set for the current and following year.", MessageType.ERROR);
        }

        //check vacation specifics (see method details)
        checkVacationBelowThresholdAndValid(vacation);

        //set appropriate fields
        if(vacation.isNew())
        {
            vacation.setObjectCreatedDateTime(new Date());
            vacation.setObjectCreatedUser(userLoginManager.getCurrentUser());
        }
        else
        {
            //additionally, if vacation already existed, make sure that the user of the existing database vacation matches, and that it is not a vacation that used to be in the past.
            Vacation db_vacation = vacationRepository.findFirstById(vacation.getId());
            if(!db_vacation.getUser().equals(u))
            {
                throw new RuntimeException("Attempted to edit vacation of different user.");
            }
            if(db_vacation.getBeginDate().before(new Date()))
            {
                throw new ProdigaGeneralExpectedException("Vacations cannot be set for the past", MessageType.ERROR);
            }

            vacation.setObjectChangedDateTime(new Date());
            vacation.setObjectChangedUser(userLoginManager.getCurrentUser());

            //delete old vacation booking if exists, create new one after
            bookingRepository.findUsersBookingWithCategoryInRange(u, bookingCategoryRepository.findById(Constants.VACATION_BOOKING_ID).orElse(null),
                    Date.from(Instant.ofEpochMilli(db_vacation.getBeginDate().toInstant().toEpochMilli() - 1000 * 60 * 30)),
                    Date.from(Instant.ofEpochMilli(db_vacation.getEndDate().toInstant().toEpochMilli() + 1000 * 60 * 60 * 24 + 1000 * 60 * 30))).stream().findFirst().ifPresent(bookingRepository::delete);
        }

        //Save method if no exception has been thrown so far
        Vacation result = vacationRepository.save(vacation);

        //Create vacation booking covering these days
        Booking vacationBooking = new Booking();
        vacationBooking.setDept(u.getAssignedDepartment());
        vacationBooking.setTeam(u.getAssignedTeam());
        vacationBooking.setActivityStartDate(vacation.getBeginDate());
        vacationBooking.setActivityEndDate(Date.from(Instant.ofEpochMilli(vacation.getEndDate().toInstant().toEpochMilli() + 1000 * 60 * 60 * 24)));
        vacationBooking.setBookingCategory(bookingCategoryRepository.findById(Constants.VACATION_BOOKING_ID).orElse(null));
        vacationBooking.setUser(u);
        vacationBooking.setObjectCreatedUser(u);
        vacationBooking.setObjectCreatedDateTime(new Date());
        bookingRepository.save(vacationBooking);

        logInformationService.logForCurrentUser("Vacation " + result.getId() + " was saved");

        return result;
    }

    /**
     * Loads a single vacation by id
     *
     * @param vacationId The id to search by
     * @return the vacation with the given ID
     */
    @PreAuthorize("hasAuthority('EMPLOYEE')") //NOSONAR
    public Vacation getVacationById(Long vacationId)
    {
        Vacation v = vacationRepository.findFirstById(vacationId);
        if(v == null) return null;

        if(!v.getUser().equals(userLoginManager.getCurrentUser()))
        {
            throw new RuntimeException("Attempted to load vacation from different user.");
        }
        return v;
    }

    /**
     * Deletes the vacation.
     *
     * @param vacation the vacation to delete
     */
    @PreAuthorize("hasAuthority('EMPLOYEE')") //NOSONAR
    public void deleteVacation(Vacation vacation, boolean hardDelete) throws ProdigaGeneralExpectedException
    {
        User u = userLoginManager.getCurrentUser();
        Vacation v = vacation;
        if(!hardDelete) {
            v = vacationRepository.findFirstById(vacation.getId());
            if(!v.getUser().equals(u))
            {
                throw new RuntimeException("Attempted to delete vacation from different user.");
            }
            if(v.getBeginDate().before(new Date()))
            {
                throw new ProdigaGeneralExpectedException("Cannot delete vacations that have already begun or ended.", MessageType.ERROR);
            }
        }
        vacationRepository.delete(v);

        bookingRepository.findUsersBookingWithCategoryInRange(u, bookingCategoryRepository.findById(Constants.VACATION_BOOKING_ID).orElse(null),
                Date.from(Instant.ofEpochMilli(v.getBeginDate().toInstant().toEpochMilli() - 1000 * 60 * 30)),
                Date.from(Instant.ofEpochMilli(v.getEndDate().toInstant().toEpochMilli() + 1000 * 60 * 60 * 24 + 1000 * 60 * 30))).stream().findFirst().ifPresent(bookingRepository::delete);

        logInformationService.logForCurrentUser("Vacation " + v.getId() + " was deleted");
    }

    /**
     * Deletes all vacations for the given user
     * @param u The user
     */
    @PreAuthorize("hasAuthority('ADMIN') or principal.username eq #u.username")
    public void deleteVacationsForUser(User u) throws ProdigaGeneralExpectedException {
        if(u == null) {
            return;
        }
        Collection<Vacation> vacations = getAllVacationsByUser(u);

        if(vacations.size() > 0) {
            for(Vacation v : vacations) {
                deleteVacation(v, true);
            }
        }
    }

    /**
     * Returns an integer containing the number of remaining vacation days in the given year
     * @param year The given year
     * @return The number of remaining vacation days in this year.
     */
    @PreAuthorize("hasAuthority('EMPLOYEE')") //NOSONAR
    public int getUsersRemainingVacationDays(int year)
    {
        User u = userLoginManager.getCurrentUser();
        return 25 - vacationRepository.findUsersYearlyVacations(u,year).stream().mapToInt(v -> getVacationDaysInYear(v, year)).sum();
    }

    /**
     * Checks if any vacation by logged in user covers the day of start or end of the given booking
     * @param booking The booking to check
     * @return A vacation that blocks this booking if exists, otherwise null
     */
    @PreAuthorize("hasAuthority('EMPLOYEE')") //NOSONAR
    public Vacation vacationCoversBooking(Booking booking)
    {
        return vacationCoversBooking(booking, userLoginManager.getCurrentUser());
    }

    public Vacation vacationCoversBooking(Booking booking, User u) {
        LocalDate beginDate = toLocalDate(booking.getActivityStartDate()).atStartOfDay(ZoneId.systemDefault()).toLocalDate();
        LocalDate endDate = toLocalDate(booking.getActivityEndDate()).atStartOfDay(ZoneId.systemDefault()).toLocalDate();

        Vacation v = vacationRepository.findVacationCoveringDate(toDate(beginDate), u);
        if (v != null) return v;

        return vacationRepository.findVacationCoveringDate(toDate(endDate), u);
    }

    /**
     * Given a vacation, gets all other vacations in the year of the start and end date.
     * The method then checks
     *  - that 25 vacation days are not passed for any year.
     *  - that no other vacation of this user covers the same days
     *  - that no booking is already taken for any of the vacation days.
     * @param vacation the vacation to check
     * @throws ProdigaGeneralExpectedException is thrown with type ERROR if the vacation is not valid in some way.
     */
    private void checkVacationBelowThresholdAndValid(Vacation vacation) throws ProdigaGeneralExpectedException
    {
        //Check vacation duration
        LocalDate startDate = vacation.getBeginDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate endDate = vacation.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        //Check remaining days for start year
        checkYearlyVacationDays(vacation, startDate.getYear());

        //Repeat for end year if end year != start year
        if(startDate.getYear() != endDate.getYear())
        {
            checkYearlyVacationDays(vacation, endDate.getYear());
        }

        //Make sure that vacation covers at least one actual vacation day (not weekend or holdiay)
        if(getCountVacationDays(startDate, endDate) == 0)
        {
            throw new ProdigaGeneralExpectedException("At least one vacation day must not be a holiday or weekend day.", MessageType.ERROR);
        }

        //Check that there is no other vacations over the same days
        Collection<Vacation> vcs = vacationRepository.findUsersVacationInRange(vacation.getUser(), vacation.getBeginDate(), vacation.getEndDate());
        if(vacation.getId() != null) vcs.remove(vacation);

        if(!vcs.isEmpty())
        {
            throw new ProdigaGeneralExpectedException("Vacation covers existing vacation time.", MessageType.ERROR);
        }

        //check that there is no booking for any vacation days
        if(!bookingRepository.findUsersBookingInRange(vacation.getUser(), vacation.getBeginDate(), vacation.getEndDate()).stream().filter(x -> !x.getBookingCategory().getId().equals(Constants.VACATION_BOOKING_ID)).collect(Collectors.toList()).isEmpty())
        {
            throw new ProdigaGeneralExpectedException("Vacation covers existing bookings.", MessageType.ERROR);
        }
    }

    /**
     * Given a vacation and a year, checks the sum of the vacation user's other vacations in this year and the given vacation to be below 25
     * @param vacation The vacation to calculate the yearly vacation days for
     * @param year The year to calculate the yearly vacation days for
     * @throws ProdigaGeneralExpectedException Is thrown when the vacation days of 25 are exceeded for this year.
     */
    private void checkYearlyVacationDays(Vacation vacation, int year) throws ProdigaGeneralExpectedException
    {
        Collection<Vacation> vcsStart = vacationRepository.findUsersYearlyVacations(vacation.getUser(), year);
        if(vacation.getId() != null) vcsStart.remove(vacation);
        int daysFromOtherVacations = vcsStart.stream().mapToInt(v ->
                getVacationDaysInYear(v, year)).sum();
        //add the vacation on
        int currentVacationTime = getVacationDaysInYear(vacation, year);
        if(daysFromOtherVacations + currentVacationTime > 25)
        {
            throw new ProdigaGeneralExpectedException("Yearly vacation days for the year " + year + " cannot exceed 25.", MessageType.ERROR);
        }
    }

    /**
     * Gets the number of days in a vacation
     * @param vacation The vacation to check
     * @return The number of days between start and end date, excluding weekends and holidays
     */
    public int getVacationDays(Vacation vacation)
    {
        return getCountVacationDays(toLocalDate(vacation.getBeginDate()), toLocalDate(vacation.getEndDate()));
    }

    /**
     * Gets the number of days in a vacation, but only the days in a given year
     * @param vacation The vacation to check
     * @param year the year bound
     * @return The number of days between start and end date, in the year provided, excluding holidays and weekends
     */
    public int getVacationDaysInYear(Vacation vacation, int year)
    {
        LocalDate beginDate = toLocalDate(vacation.getBeginDate());
        LocalDate endDate = toLocalDate(vacation.getEndDate());
        LocalDate upperBound = LocalDate.of(year,12,31);
        LocalDate lowerBound = LocalDate.of(year,1,1);
        if(beginDate.isBefore(lowerBound)) beginDate = lowerBound;
        if(endDate.isAfter(upperBound)) endDate = upperBound;

        return getCountVacationDays(beginDate, endDate);
    }

    /**
     * For two LocalDate variables, calculates the amount of vacation days between them, excluding holidays and weekends.
     * @param beginDate The start date of the vacation.
     * @param endDate The end date of the vacation.
     * @return The vacation days excluding weekends and national holidays.
     */
    public int getCountVacationDays(LocalDate beginDate, LocalDate endDate)
    {
        HolidayManager m = HolidayManager.getInstance(ManagerParameters.create(HolidayCalendar.AUSTRIA));

        int vacationDays = 0;
        while(!beginDate.isAfter(endDate))
        {
            //check if date is weekend day
            if(beginDate.getDayOfWeek() != DayOfWeek.SATURDAY && beginDate.getDayOfWeek() != DayOfWeek.SUNDAY)
            {
                //because two date formats werent enough yet
                org.joda.time.LocalDate jodaDate = org.joda.time.LocalDate.fromDateFields(toDate(beginDate));
                //check if date is a holiday
                if(!m.isHoliday(jodaDate))
                {
                    vacationDays++;
                }
            }
            beginDate = beginDate.plusDays(1);
        }

        return vacationDays;
    }

    /**
     * Gets a collection of holidays from the current year and next year
     * @return A collection of Jollyday Holiday objects from the current year and next year
     */
    public Collection<Holiday> getHolidays()
    {
        HolidayManager m = HolidayManager.getInstance(ManagerParameters.create(HolidayCalendar.AUSTRIA));
        Collection<Holiday> holidays = m.getHolidays(LocalDate.now().getYear());
        holidays.addAll(m.getHolidays(LocalDate.now().getYear() + 1));
        return holidays;
    }


    /**
     * Converts a java.util.Date to a LocalDate
     * @param date the date to convert
     * @return The corresponding LocalDate
     */
    public LocalDate toLocalDate(Date date)
    {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * Converts a LocalDate to a java.util.Date
     * @param localDate the date to convert
     * @return The corresponding java.util.Date
     */
    public Date toDate(LocalDate localDate)
    {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
