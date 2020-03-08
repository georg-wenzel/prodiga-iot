package uibk.ac.at.prodiga.ui.controllers;

import java.util.Collection;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import uibk.ac.at.prodiga.model.User;
import uibk.ac.at.prodiga.services.UserService;

/**
 * Controller for the user list view.
 *
 * This class is part of the skeleton project provided for students of the
 * courses "Software Architecture" and "Software Engineering" offered by the
 * University of Innsbruck.
 */
@Component
@Scope("view")
public class UserListController {

    private final UserService userService;

    public UserListController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Returns a list of all users.
     *
     * @return
     */
    public Collection<User> getUsers() {
        return userService.getAllUsers();
    }

}