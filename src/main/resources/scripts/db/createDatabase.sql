CREATE TABLE keywords
(
    keyword VARCHAR(100) PRIMARY KEY NOT NULL,
    weight FLOAT DEFAULT '1' NOT NULL
);
CREATE TABLE queries
(
    id INT(11) PRIMARY KEY NOT NULL AUTO_INCREMENT,
    query MEDIUMTEXT,
    minTweetId MEDIUMTEXT,
    counter INT(11) DEFAULT '0'
);
CREATE TABLE tweets
(
    id BIGINT(20) PRIMARY KEY NOT NULL,
    userId BIGINT(20),
    userName VARCHAR(150) NOT NULL,
    text MEDIUMTEXT NOT NULL,
    textHash INT(11),
    creationDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    favoriteCount INT(11),
    classified TINYINT(1) DEFAULT '0' NOT NULL,
    assertion TINYINT(1),
    topic TINYINT(1),
    rumor TINYINT(1),
    retweetCount INT(11)
);
CREATE INDEX hash__index ON tweets (textHash);
CREATE INDEX users__index ON tweets (userName);
CREATE TABLE tweets_crawled_tf
(
    id BIGINT(20) PRIMARY KEY NOT NULL,
    crawledId MEDIUMTEXT,
    userName VARCHAR(150) NOT NULL,
    userId BIGINT(20),
    textHash INT(11),
    creationDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    favoriteCount INT(11),
    classified TINYINT(1) DEFAULT '0' NOT NULL,
    assertion TINYINT(1),
    topic TINYINT(1),
    rumor TINYINT(1),
    retweetCount INT(11),
    score DOUBLE DEFAULT '0',
    text MEDIUMTEXT NOT NULL,
    crawledDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE tweets_crawled_tfidf
(
    id BIGINT(20) PRIMARY KEY NOT NULL,
    crawledId MEDIUMTEXT,
    userName VARCHAR(150) NOT NULL,
    userId BIGINT(20),
    text MEDIUMTEXT NOT NULL,
    textHash INT(11),
    favoriteCount INT(11),
    classified TINYINT(1) DEFAULT '0' NOT NULL,
    assertion TINYINT(1),
    topic TINYINT(1),
    rumor TINYINT(1),
    retweetCount INT(11),
    score DOUBLE DEFAULT '0',
    creationDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    crawledDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE collector_terms
(
    terms TEXT
);
CREATE TABLE collector
(
    id BIGINT(20) PRIMARY KEY NOT NULL,
    text MEDIUMTEXT NOT NULL,
    textHash INT(11)
);
CREATE UNIQUE INDEX collector_textHash_uindex ON collector (textHash);

