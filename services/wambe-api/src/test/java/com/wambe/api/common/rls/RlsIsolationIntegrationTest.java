package com.wambe.api.common.rls;

import static org.assertj.core.api.Assertions.assertThat;

import com.wambe.api.PostgresIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

class RlsIsolationIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactions;

    @Test
    void transactionLocalOwnerContextDoesNotLeakAcrossReusedConnection() {
        UUID ownerA = UUID.randomUUID();
        UUID ownerB = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        transactions.executeWithoutResult(status -> {
            setOwner(ownerA);
            jdbcTemplate.update("""
                    insert into events (
                        id, owner_id, status, timezone, last_saved_at
                    ) values (?, ?, 'draft', 'Africa/Lagos', now())
                    """,
                    eventId,
                    ownerA);
        });

        int ownerBCount = transactions.execute(status -> {
            setOwner(ownerB);
            return jdbcTemplate.queryForObject("select count(*) from events", Integer.class);
        });
        int ownerACount = transactions.execute(status -> {
            setOwner(ownerA);
            return jdbcTemplate.queryForObject("select count(*) from events", Integer.class);
        });

        assertThat(ownerBCount).isZero();
        assertThat(ownerACount).isEqualTo(1);
    }

    private void setOwner(UUID ownerId) {
        jdbcTemplate.queryForObject(
                "select set_config('app.current_user_id', ?, true)",
                String.class,
                ownerId.toString());
    }
}
