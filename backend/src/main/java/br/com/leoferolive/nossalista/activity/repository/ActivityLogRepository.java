package br.com.leoferolive.nossalista.activity.repository;

import br.com.leoferolive.nossalista.activity.domain.ActivityLogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLogEntry, UUID> {
    Page<ActivityLogEntry> findByList_IdOrderByCreatedAtDesc(UUID listId, Pageable pageable);
}
