package in.codefarm.designarena.user;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.Optional;

/** Reads and writes accounts. */
interface AppUserRepository extends Repository<AppUser, Long> {

    AppUser save(AppUser user);

    Optional<AppUser> findById(Long id);

    /**
     * Finds an account by email, ignoring case.
     *
     * <p>Addresses are stored lower-cased and matched lower-cased, so a person who
     * registered as {@code Alice@example.com} can sign in as {@code alice@example.com}.
     * The unique index added in V3 is on {@code lower(email)} to match.
     */
    @Query("SELECT u FROM AppUser u WHERE lower(u.email) = lower(:email)")
    Optional<AppUser> findByEmailIgnoringCase(String email);
}
