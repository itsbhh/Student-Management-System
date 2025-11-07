CREATE KEYSPACE IF NOT EXISTS student_keyspace
WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

USE student_keyspace;

CREATE TABLE IF NOT EXISTS admins (
    id TEXT PRIMARY KEY,
    password TEXT
);


CREATE TABLE IF NOT EXISTS teachers (
    id TEXT PRIMARY KEY,
    name TEXT,
    password TEXT
);

CREATE TABLE IF NOT EXISTS students (
    id TEXT PRIMARY KEY,
    name TEXT,
    subject1 INT,
    subject2 INT,
    subject3 INT,
    password TEXT,
    total INT,
    percentage DOUBLE,
    rank INT
);

-- Admin user
INSERT INTO admins (id, password) VALUES ('Admin12ABCDE', 'adminpassword');

-- Sample Teacher (password = 'teacherpass1')
INSERT INTO teachers (id, name, password) VALUES ('T1234', 'John Doe', 'teacherpass1') IF NOT EXISTS;

-- Sample Students (password = 'pass1')
INSERT INTO students (id, name, subject1, subject2, subject3, password, total, percentage, rank)
VALUES ('S1001', 'Alice', 80, 85, 90, 'pass1', 255, 85.0, 0) IF NOT EXISTS;

INSERT INTO students (id, name, subject1, subject2, subject3, password, total, percentage, rank)
VALUES ('S1002', 'Bob', 75, 70, 78, 'pass1', 223, 74.33, 0) IF NOT EXISTS;
