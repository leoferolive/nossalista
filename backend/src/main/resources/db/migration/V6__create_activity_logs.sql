CREATE TABLE activity_logs (
    id UUID PRIMARY KEY,
    list_id UUID NOT NULL,
    user_id UUID NOT NULL,
    user_name VARCHAR(100),
    action VARCHAR(50) NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_id UUID,
    target_name VARCHAR(200),
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_activity_logs_list FOREIGN KEY (list_id) REFERENCES lists(id) ON DELETE CASCADE,
    CONSTRAINT fk_activity_logs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_activity_logs_list_created_at ON activity_logs(list_id, created_at);
CREATE INDEX idx_activity_logs_user_id ON activity_logs(user_id);
