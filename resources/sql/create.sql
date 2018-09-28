CREATE TABLE Exercise
(
    id              SERIAL PRIMARY KEY,
    name            varchar(64) NOT NULL UNIQUE
);

CREATE TABLE Plan
(
    id              SERIAL PRIMARY KEY,
    name            varchar(64) NOT NULL UNIQUE
);

CREATE TABLE Task
(
    id              SERIAL PRIMARY KEY,
    planid          INT NOT NULL,
    exerciseid      INT NOT NULL,
    FOREIGN KEY     (planid) REFERENCES PLAN (id),
    FOREIGN KEY     (exerciseid) REFERENCES Exercise (id)
);

CREATE TABLE CompletedWeightTask
(
    id              SERIAL PRIMARY KEY,
    planid          INT NOT NULL,
    exerciseid      INT NOT NULL,
    sets            INT NOT NULL,
    reps            INT NOT NULL,
    weight          INT NOT NULL,
    FOREIGN KEY     (planid) REFERENCES PLAN (id),
    FOREIGN KEY     (exerciseid) REFERENCES Exercise (id)
);

CREATE TABLE CompletedCardioTask
(
    id              SERIAL PRIMARY KEY,
    planid          INT NOT NULL,
    exerciseid      INT NOT NULL,
    time            INT NOT NULL,
    level           INT,
    FOREIGN KEY     (planid) REFERENCES PLAN (id),
    FOREIGN KEY     (exerciseid) REFERENCES Exercise (id)
);

