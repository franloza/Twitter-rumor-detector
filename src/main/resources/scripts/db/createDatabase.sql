CREATE TABLE tweets
(
    id BIGINT(20) PRIMARY KEY NOT NULL,
    text MEDIUMTEXT NOT NULL,
    classified TINYINT(1) DEFAULT '0' NOT NULL,
    assertion TINYINT(1),
    topic TINYINT(1),
    rumor TINYINT(1)
);

