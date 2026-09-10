CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    message VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL
);
