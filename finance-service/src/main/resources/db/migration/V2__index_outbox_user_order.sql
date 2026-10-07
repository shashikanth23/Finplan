CREATE INDEX idx_outbox_user_order
    ON outbox_events (user_id, created_at, id)
    WHERE published_at IS NULL;
