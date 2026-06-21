-- ---------------------------------------------------------------------------
-- Spring Security JDBC authentication schema (users + authorities).
-- These tables are NOT managed by JPA; they are created here so that
-- JdbcUserDetailsManager can read/write credentials. Compatible with both
-- PostgreSQL (dev) and H2 (test). Executed after Hibernate schema generation
-- because spring.jpa.defer-datasource-initialization=true.
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS users (
    username VARCHAR(50)  NOT NULL PRIMARY KEY,
    password VARCHAR(100) NOT NULL,
    enabled  BOOLEAN      NOT NULL
);

CREATE TABLE IF NOT EXISTS authorities (
    username  VARCHAR(50) NOT NULL,
    authority VARCHAR(50) NOT NULL,
    CONSTRAINT fk_authorities_users FOREIGN KEY (username) REFERENCES users (username)
);

CREATE UNIQUE INDEX IF NOT EXISTS ix_auth_username ON authorities (username, authority);
