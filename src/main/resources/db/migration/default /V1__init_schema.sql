CREATE TABLE user
(
    id       bigint AUTO_INCREMENT,
    username VARCHAR(255) UNIQUE,
    password VARCHAR(255)
);
