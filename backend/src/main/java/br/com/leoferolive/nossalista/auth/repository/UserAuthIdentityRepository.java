package br.com.leoferolive.nossalista.auth.repository;

import br.com.leoferolive.nossalista.auth.domain.UserAuthIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Repository for stable external-provider identities. */
@Repository
public interface UserAuthIdentityRepository extends JpaRepository<UserAuthIdentity, UUID> {

    /** Finds an identity by its provider-scoped stable key. */
    Optional<UserAuthIdentity> findByProviderAndIssuerAndSubject(
        String provider, String issuer, String subject
    );

    /** Finds the identity already occupying a user's provider/issuer slot. */
    Optional<UserAuthIdentity> findByProviderAndIssuerAndUserId(
        String provider, String issuer, UUID userId
    );
}
