-- Schema for the default database: the tenant schema plus the
-- user_tenants lookup table, which only lives here.
-- Column order matches the positional INSERT statements in Query.sql.
CREATE TABLE organizations
(
    id      bigint       NOT NULL AUTO_INCREMENT,
    version bigint                DEFAULT NULL,
    address varchar(255)          DEFAULT NULL,
    email   varchar(255)          DEFAULT NULL,
    name    varchar(255)          DEFAULT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE persons
(
    id                     bigint NOT NULL AUTO_INCREMENT,
    version                bigint       DEFAULT NULL,
    birth_date             date         DEFAULT NULL,
    email                  varchar(255) DEFAULT NULL,
    first_name             varchar(255) DEFAULT NULL,
    home_phone             varchar(255) DEFAULT NULL,
    last_name              varchar(255) DEFAULT NULL,
    mobile                 varchar(255) DEFAULT NULL,
    social_security_number varchar(255) DEFAULT NULL,
    organization_id        bigint       DEFAULT NULL,
    PRIMARY KEY (id),
    KEY fk_persons_organization (organization_id),
    CONSTRAINT fk_persons_organization FOREIGN KEY (organization_id) REFERENCES organizations (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE users
(
    id        bigint NOT NULL AUTO_INCREMENT,
    version   bigint       DEFAULT NULL,
    password  varchar(255) DEFAULT NULL,
    username  varchar(255) DEFAULT NULL,
    person_id bigint       DEFAULT NULL,
    PRIMARY KEY (id),
    KEY fk_users_person (person_id),
    CONSTRAINT fk_users_person FOREIGN KEY (person_id) REFERENCES persons (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE user_tenants
(
    id        bigint NOT NULL AUTO_INCREMENT,
    version   bigint       DEFAULT NULL,
    tenant    int          DEFAULT NULL,
    user_name varchar(255) DEFAULT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
