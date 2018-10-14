CREATE TABLE Exercise
(
    id              SERIAL PRIMARY KEY,
    name            varchar(64) NOT NULL UNIQUE,
    sets            INT NOT NULL,
    reps            INT NOT NULL,
    weight          INT NOT NULL
);

CREATE TABLE Cardio
(
    id              SERIAL PRIMARY KEY,
    name            varchar(64) NOT NULL UNIQUE,
    duration        INT NOT NULL,
    spec            INT NOT NULL
);

CREATE TABLE Plan
(
    id              SERIAL PRIMARY KEY,
    name            varchar(64) NOT NULL UNIQUE
);

CREATE TABLE DoneExercise
(
    day             DATE NOT NULL,
    exerciseid      INT NOT NULL,
    planid          INT NOT NULL,
    FOREIGN KEY     (planid) REFERENCES PLAN (id),
    FOREIGN KEY     (exerciseid) REFERENCES Exercise (id)
);

CREATE TABLE DoneCardio
(
    day             DATE NOT NULL,
    exerciseid      INT NOT NULL,
    planid          INT NOT NULL,
    FOREIGN KEY     (planid) REFERENCES PLAN (id),
    FOREIGN KEY     (exerciseid) REFERENCES Exercise (id)
);
