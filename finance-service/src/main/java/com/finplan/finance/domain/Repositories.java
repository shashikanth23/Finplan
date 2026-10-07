package com.finplan.finance.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Every lookup is scoped by user id, so one user can never read or change another user's rows. */
public final class Repositories {
    private Repositories() {}

    public interface ProfileRepository extends JpaRepository<Profile, UUID> {}

    public interface IncomeRepository extends JpaRepository<Income, UUID> {
        List<Income> findByUserIdOrderByCreatedAtDesc(UUID userId);
        Optional<Income> findByIdAndUserId(UUID id, UUID userId);
    }

    public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
        List<Expense> findByUserIdOrderByCreatedAtDesc(UUID userId);
        Optional<Expense> findByIdAndUserId(UUID id, UUID userId);
    }

    public interface DebtRepository extends JpaRepository<Debt, UUID> {
        List<Debt> findByUserIdOrderByCreatedAtDesc(UUID userId);
        Optional<Debt> findByIdAndUserId(UUID id, UUID userId);
    }

    public interface GoalRepository extends JpaRepository<Goal, UUID> {
        List<Goal> findByUserIdOrderByTargetDateAsc(UUID userId);
        Optional<Goal> findByIdAndUserId(UUID id, UUID userId);
    }

    public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {
        @Query(value = """
                select event.*
                from outbox_events event
                where event.published_at is null
                  and not exists (
                    select 1
                    from outbox_events earlier
                    where earlier.user_id = event.user_id
                      and earlier.published_at is null
                      and (earlier.created_at, earlier.id) < (event.created_at, event.id)
                  )
                order by event.created_at, event.id
                limit 50
                for update skip locked
                """, nativeQuery = true)
        List<OutboxEvent> lockNextBatch();

        @Modifying
        @Transactional
        @Query("update OutboxEvent e set e.publishedAt = :at where e.id = :id")
        int markPublished(@Param("id") UUID id, @Param("at") Instant at);
    }
}
