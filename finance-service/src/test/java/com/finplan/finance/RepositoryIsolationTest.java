package com.finplan.finance;

import com.finplan.finance.domain.Enums.Frequency;
import com.finplan.finance.domain.Enums.IncomeType;
import com.finplan.finance.domain.Income;
import com.finplan.finance.domain.Repositories.IncomeRepository;
import com.finplan.finance.domain.OutboxEvent;
import com.finplan.finance.domain.Repositories.OutboxRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs against a real PostgreSQL: Flyway applies the migration, then Hibernate (ddl-auto=validate) checks that
 * every entity matches the schema. Needs Docker (skipped automatically without it).
 */
@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RepositoryIsolationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired IncomeRepository incomes;
    @Autowired OutboxRepository outbox;

    private Income income(UUID user, String label, String amount, Frequency f) {
        Income i = new Income();
        i.setUserId(user);
        i.setType(IncomeType.BASIC);
        i.setLabel(label);
        i.setAmount(new BigDecimal(amount));
        i.setFrequency(f);
        return incomes.saveAndFlush(i);
    }

    @Test
    void otherUsersCannotSeeOrFetchMyRows() {
        UUID alice = UUID.randomUUID(), bob = UUID.randomUUID();
        Income mine = income(alice, "Salary", "80000", Frequency.MONTHLY);
        income(bob, "Salary", "50000", Frequency.MONTHLY);

        assertEquals(1, incomes.findByUserIdOrderByCreatedAtDesc(alice).size());
        assertTrue(incomes.findByIdAndUserId(mine.getId(), alice).isPresent());
        assertTrue(incomes.findByIdAndUserId(mine.getId(), bob).isEmpty());
    }

    @Test
    void annualIncomeIsSpreadOverTwelveMonths() {
        Income bonus = income(UUID.randomUUID(), "Bonus", "120000", Frequency.ANNUAL);
        assertEquals(0, new BigDecimal("10000.00").compareTo(bonus.monthlyAmount()));
    }

    @Test
    void databaseRejectsNegativeAmounts() {
        assertThrows(Exception.class, () -> income(UUID.randomUUID(), "Bad", "-1", Frequency.MONTHLY));
    }

    @Test
    void outboxBatchKeepsEachUsersEventsInOrder() {
        UUID userId = UUID.randomUUID();
        OutboxEvent first = event(userId, Instant.parse("2026-01-01T00:00:00Z"));
        OutboxEvent second = event(userId, Instant.parse("2026-01-02T00:00:00Z"));
        outbox.saveAndFlush(first);
        outbox.saveAndFlush(second);

        assertEquals(java.util.List.of(first.getId()), outbox.lockNextBatch().stream().map(OutboxEvent::getId).toList());
        outbox.markPublished(first.getId(), Instant.now());
        assertEquals(java.util.List.of(second.getId()), outbox.lockNextBatch().stream().map(OutboxEvent::getId).toList());
    }

    private OutboxEvent event(UUID userId, Instant createdAt) {
        OutboxEvent event = new OutboxEvent();
        event.setAggregateType("INCOME");
        event.setAggregateId(UUID.randomUUID());
        event.setEventType("INCOME_CHANGED");
        event.setUserId(userId);
        event.setPayload("{}");
        event.setCreatedAt(createdAt);
        return event;
    }
}
