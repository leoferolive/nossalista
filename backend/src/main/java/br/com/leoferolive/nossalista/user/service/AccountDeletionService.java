package br.com.leoferolive.nossalista.user.service;

import br.com.leoferolive.nossalista.list.domain.List;
import br.com.leoferolive.nossalista.list.repository.ListRepository;
import br.com.leoferolive.nossalista.member.repository.ListMemberRepository;
import br.com.leoferolive.nossalista.push.PushSubscriptionStore;
import br.com.leoferolive.nossalista.user.exception.UserNotFoundException;
import br.com.leoferolive.nossalista.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service responsible for complete account deletion (LGPD compliance).
 *
 * Deletion order respects foreign key constraints:
 * 1. Delete activity logs for user's owned lists
 * 2. Delete list items for user's owned lists
 * 3. Delete list members for user's owned lists
 * 4. Delete user's owned lists
 * 5. Delete user's memberships in other lists
 * 6. Remove user's push subscriptions (in-memory store)
 * 7. Delete the user record
 *
 * Note: list_items.created_by uses ON DELETE SET NULL (V8 migration),
 * so items the user created on OTHER people's lists will survive with
 * created_by = null. The user's own lists are fully deleted (CASCADE).
 */
@Service
public class AccountDeletionService {

    private static final Logger log = LoggerFactory.getLogger(AccountDeletionService.class);

    private final UserRepository userRepository;
    private final ListRepository listRepository;
    private final ListMemberRepository listMemberRepository;
    private final PushSubscriptionStore pushSubscriptionStore;

    public AccountDeletionService(
            UserRepository userRepository,
            ListRepository listRepository,
            ListMemberRepository listMemberRepository,
            PushSubscriptionStore pushSubscriptionStore) {
        this.userRepository = userRepository;
        this.listRepository = listRepository;
        this.listMemberRepository = listMemberRepository;
        this.pushSubscriptionStore = pushSubscriptionStore;
    }

    /**
     * Permanently deletes a user account and all associated data.
     *
     * @param userId ID of the user to delete
     * @throws UserNotFoundException if user does not exist
     */
    @Transactional
    public void deleteAccount(UUID userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado: " + userId));

        log.info("Account deletion started: userId={}, username='{}', email='{}'",
                userId, user.getUsername(), user.getEmail());

        // 1-4. Delete user's owned lists and all their dependent data.
        // Each list has CASCADE on activity_logs, list_items, and list_members,
        // so deleting the list removes all children.
        java.util.List<List> ownedLists = listRepository.findByOwnerId(userId);
        for (List list : ownedLists) {
            log.debug("Deleting owned list: listId={}, listName='{}'", list.getId(), list.getName());
            listRepository.delete(list);
        }

        // 5. Delete user's memberships in other people's lists
        var memberships = listMemberRepository.findByUserId(userId);
        if (!memberships.isEmpty()) {
            log.debug("Deleting {} memberships in other lists", memberships.size());
            listMemberRepository.deleteAll(memberships);
        }

        // 6. Remove push subscriptions (in-memory store)
        pushSubscriptionStore.removeAll(userId);

        // 7. Delete the user record itself.
        // Remaining CASCADE constraints (activity_logs.user_id) handle any
        // activity log entries the user created on other people's lists.
        // list_items.created_by is SET NULL via V8 migration.
        userRepository.delete(user);

        log.info("Account deletion completed: userId={}", userId);
    }
}
