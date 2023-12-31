package uibk.ac.at.prodiga.services;

import com.google.common.collect.Lists;
import org.springframework.context.annotation.Scope;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import uibk.ac.at.prodiga.model.Room;
import uibk.ac.at.prodiga.model.User;
import uibk.ac.at.prodiga.repositories.RoomRepository;
import uibk.ac.at.prodiga.repositories.UserRepository;
import uibk.ac.at.prodiga.utils.MessageType;
import uibk.ac.at.prodiga.utils.ProdigaGeneralExpectedException;

import java.util.Collection;
import java.util.Date;

/*
Anlegen, abfragen, bearbeiten und löschen von Räumen.
Checken was passiert, wenn noch ein Raspi/Würfel in dem Raum is usw...
 */
@Component
@Scope("application")
public class RoomService {
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final LogInformationService logInformationService;
    private final RaspberryPiService raspberryPiService;

    private User getAuthenicatedUser(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findFirstByUsername(auth.getName());
    }

    public RoomService(RoomRepository roomRepository, UserRepository userRepository, LogInformationService logInformationService, RaspberryPiService raspberryPiService){
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.logInformationService = logInformationService;
        this.raspberryPiService = raspberryPiService;
    }

    /**
     * Returns a collection of all rooms
     * @return A collection of all rooms
     */
    @PreAuthorize("hasAuthority('ADMIN')") //NOSONAR
    public Collection<Room> getAllRooms(){
        return Lists.newArrayList(roomRepository.findAll());
    }

    /**
     * Gets the first room with the specified id. (Unique identifier)
     * @param id the id of the room
     * @return The room with this Id, or null if none exists
     */
    @PreAuthorize("hasAuthority('ADMIN')") //NOSONAR
    public Room getFirstById(long id){
        return roomRepository.findFirstById(id);
    }

    /**
     * Saves the current room in the database. If room with this ID already exists, overwrites data of existing room in the database.
     * @param room The room to save
     * @return The new state of the room after saving in the DB
     */
    @PreAuthorize("hasAuthority('ADMIN')") //NOSONAR
    public Room saveRoom(Room room) throws ProdigaGeneralExpectedException{
        if(room.getName() == null || room.getName().isEmpty()){
            throw new ProdigaGeneralExpectedException("Roomname cannot be empty", MessageType.ERROR);
        }
        if(room.getName().length() < 2 || room.getName().length() > 20) {
            throw new ProdigaGeneralExpectedException("Room name must be between 2 and 20 characters", MessageType.ERROR);
        }

        if(room.isNew()){
            if(roomRepository.findFirstByName(room.getName()) != null){
                throw new ProdigaGeneralExpectedException("Room with same name already exists.", MessageType.ERROR);
            }
            room.setObjectCreatedDateTime(new Date());
            room.setObjectCreatedUser(getAuthenicatedUser());
        }
        else{
            room.setObjectChangedDateTime(new Date());
            room.setObjectChangedUser(getAuthenicatedUser());
        }

        Room result = roomRepository.save(room);

        logInformationService.logForCurrentUser("Room "+ result.getName() + " was saved");

        return result;
    }

    /**
     * Deletes the room with this ID from the database.
     * @param roomToDelete The room to delete
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    public void deleteRoom(Room roomToDelete) throws ProdigaGeneralExpectedException {
        if(!raspberryPiService.findByRoom(roomToDelete).isEmpty()) {
            throw new ProdigaGeneralExpectedException("Room can not be deleted if there is a Raspberry Pi in it", MessageType.ERROR);
        }
        roomRepository.delete(roomToDelete);
        logInformationService.logForCurrentUser("Room " + roomToDelete.getName() + " was deleted!");
    }

    /**
     * Loads a room by its roomname
     * @param roomname roomname to search for
     * @return a room with the given roomname
     */
    @PreAuthorize("hasAuthority('ADMIN')") //NOSONAR
    public Room loadRoom(String roomname) {
        return roomRepository.findFirstByName(roomname);
    }

    /**
     * Creates a new room
     * @return a new room
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    public Room createNewRoom() {
        return new Room();
    }


}
