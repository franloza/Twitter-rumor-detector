CREATE TABLE tweets
(
    id BIGINT(20) PRIMARY KEY NOT NULL,
    userId BIGINT(20),
    userName VARCHAR(150) NOT NULL,
    text MEDIUMTEXT NOT NULL,
    textHash INT(11),
    creationDate DATE,
    favoriteCount INT(11),
    classified TINYINT(1) DEFAULT '0' NOT NULL,
    assertion TINYINT(1),
    topic TINYINT(1),
    rumor TINYINT(1),
    retweetCount INT(11)
);
CREATE INDEX hash__index ON tweets (textHash);
CREATE INDEX users__index ON tweets (userName);
CREATE TABLE keywords
(
    keyword VARCHAR(100) PRIMARY KEY NOT NULL,
    weight FLOAT DEFAULT '1' NOT NULL
);

