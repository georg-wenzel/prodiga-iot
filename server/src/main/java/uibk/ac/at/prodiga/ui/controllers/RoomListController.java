package uibk.ac.at.prodiga.ui.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import uibk.ac.at.prodiga.model.Room;
import uibk.ac.at.prodiga.services.RoomService;

import java.io.Serializable;
import java.util.Collection;

@Component
@Scope("view")
public class RoomListController {

    private final RoomService roomService;

    public RoomListController(RoomService roomService){this.roomService = roomService;}

    public Collection<Room> getRooms(){return roomService.getAllRooms();}
}