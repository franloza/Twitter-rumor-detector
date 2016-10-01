CREATE TABLE tweets
(
    id BIGINT(20) PRIMARY KEY NOT NULL,
    user VARCHAR(150) NOT NULL,
    classified TINYINT(1) DEFAULT '0' NOT NULL,
    assertion TINYINT(1),
    topic TINYINT(1),
    rumor TINYINT(1),
    text MEDIUMTEXT NOT NULL
);
CREATE INDEX users__index ON tweets (user);
CREATE TABLE hashtags
(
    hashtag VARCHAR(200) PRIMARY KEY NOT NULL,
    count INT(11)
);