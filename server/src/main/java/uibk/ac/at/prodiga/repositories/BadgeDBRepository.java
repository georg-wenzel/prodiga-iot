package uibk.ac.at.prodiga.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uibk.ac.at.prodiga.model.BadgeDB;
import uibk.ac.at.prodiga.model.User;

import java.util.Collection;

/**
 * DB Repository for managing Badges
 */

public interface BadgeDBRepository extends AbstractRepository<BadgeDB, String> {

    BadgeDB findFirstByName(String badgeName);

    @Query("SELECT ALL FROM BadgeDB b WHERE :user = b.user")
    Collection<BadgeDB> findByUser(@Param("user") User user);
}
