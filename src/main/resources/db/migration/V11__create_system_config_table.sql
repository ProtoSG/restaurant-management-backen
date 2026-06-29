CREATE TABLE system_config (
    config_key      VARCHAR(100)    PRIMARY KEY,
    config_value    VARCHAR(255)    NOT NULL,
    date_created    TIMESTAMP,
    date_updated    TIMESTAMP,
    user_created    VARCHAR(100),
    user_updated    VARCHAR(100)
);

INSERT INTO system_config (config_key, config_value) VALUES ('takeaway_surcharge', '1');
