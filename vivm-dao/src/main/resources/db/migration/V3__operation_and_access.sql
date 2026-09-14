CREATE TABLE operation_task (
    id VARCHAR(36) NOT NULL,
    actor_id INT NOT NULL,
    operation_type VARCHAR(64) NOT NULL,
    resource_type VARCHAR(32) NOT NULL,
    resource_id VARCHAR(255),
    pool_id INT,
    command_snapshot TEXT,
    status VARCHAR(32) NOT NULL,
    step VARCHAR(64) NOT NULL,
    external_ref VARCHAR(255),
    error_code VARCHAR(64),
    error_summary VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    started_at DATETIME(6),
    finished_at DATETIME(6),
    PRIMARY KEY (id)
);

CREATE TABLE pool_permission (
    pool_id INT NOT NULL,
    user_id INT NOT NULL,
    permission VARCHAR(32) NOT NULL,
    PRIMARY KEY (pool_id, user_id, permission)
);

CREATE TABLE pool_admission (
    pool_id INT NOT NULL,
    operation_id VARCHAR(36) NOT NULL,
    acquired_at DATETIME(6) NOT NULL,
    PRIMARY KEY (pool_id),
    CONSTRAINT uk_pool_admission_operation UNIQUE (operation_id)
);

CREATE TABLE image_occupancy (
    image_id INT NOT NULL,
    holder_id VARCHAR(36) NOT NULL,
    mode VARCHAR(16) NOT NULL,
    vm_id INT,
    acquired_at DATETIME(6) NOT NULL,
    PRIMARY KEY (image_id, holder_id)
);

CREATE INDEX idx_operation_actor ON operation_task (actor_id, created_at);
CREATE INDEX idx_operation_resource ON operation_task (resource_type, resource_id);
CREATE INDEX idx_image_occupancy_holder ON image_occupancy (holder_id);
