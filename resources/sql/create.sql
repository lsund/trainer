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

CREATE TABLE PlanExercise
(
    id              SERIAL PRIMARY KEY,
    exerciseid      INT NOT NULL,
    planid          INT NOT NULL,
    FOREIGN KEY     (planid) REFERENCES PLAN (id),
    FOREIGN KEY     (exerciseid) REFERENCES Exercise (id)
);

CREATE TABLE PlanCardio
(
    id              SERIAL PRIMARY KEY,
    cardioid        INT NOT NULL,
    planid          INT NOT NULL,
    FOREIGN KEY     (planid) REFERENCES PLAN (id),
    FOREIGN KEY     (cardioid) REFERENCES Exercise (id)
);

CREATE TABLE DoneExercise
(
    id              SERIAL PRIMARY KEY,
    day             DATE NOT NULL,
    exerciseid      INT NOT NULL,
    planid          INT NOT NULL,
    FOREIGN KEY     (planid) REFERENCES PLAN (id),
    FOREIGN KEY     (exerciseid) REFERENCES Exercise (id)
);

CREATE TABLE DoneCardio
(
    id              SERIAL PRIMARY KEY,
    day             DATE NOT NULL,
    cardioid        INT NOT NULL,
    planid          INT NOT NULL,
    FOREIGN KEY     (planid) REFERENCES PLAN (id),
    FOREIGN KEY     (cardioid) REFERENCES Exercise (id)
);
