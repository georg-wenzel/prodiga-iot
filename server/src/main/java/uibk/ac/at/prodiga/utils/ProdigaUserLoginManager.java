package uibk.ac.at.prodiga.utils;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import uibk.ac.at.prodiga.model.User;
import uibk.ac.at.prodiga.services.UserService;

@Component
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ProdigaUserLoginManager {

    private final UserService userService;


    public ProdigaUserLoginManager(@Lazy UserService userService) {
        this.userService = userService;
    }

    /**
     * Returns the currently logged on user, null if no user is authenticated
     * for this session.
     *
     * @return
     */
    public User getCurrentUser() {
            String currentUserName = getCurrentUserName();
            return userService.loadUser(currentUserName);
    }

    /**
     * Returns the username of the user for this session, empty string if no
     * user has been authenticated for this session.
     *
     * @return
     */
    public String getCurrentUserName() {
        if (!isLoggedIn()) {
            return "";
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String name = auth.getName(); //get logged in username
        return name;
    }

    /**
     * Checks if a user is authenticated for this session.
     *
     * @return true if a non-anonymous user has been authenticated, false
     * otherwise
     */
    public boolean isLoggedIn() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            return auth.isAuthenticated() && !auth.getName().equals("anonymousUser");
        } else {
            return false;
        }
    }

    /**
     * Checks if the user for this session has the given role (c.f.
     * {@link UserRole}).
     *
     * @param role the role to check for as string
     * @return true if a user is authenticated and the current user has the
     * given role, false otherwise
     */
    public boolean hasRole(String role) {
        if (role == null || role.isEmpty() || !isLoggedIn()) {
            return false;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if (role.equals(ga.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
