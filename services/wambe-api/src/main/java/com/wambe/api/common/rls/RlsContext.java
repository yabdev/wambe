package com.wambe.api.common.rls;

import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class RlsContext {

    private final EntityManager entityManager;

    public RlsContext(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void apply(UUID ownerId) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("RLS owner context requires an active transaction");
        }
        entityManager.createNativeQuery("select set_config('app.current_user_id', :ownerId, true)")
                .setParameter("ownerId", ownerId.toString())
                .getSingleResult();
    }
}
