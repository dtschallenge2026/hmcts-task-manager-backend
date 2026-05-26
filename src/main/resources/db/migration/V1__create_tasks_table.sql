CREATE TABLE tasks (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    status      VARCHAR(50)  NOT NULL,
    due_date_time TIMESTAMP  NOT NULL
);
