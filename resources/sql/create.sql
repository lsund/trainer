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

CREATE TABLE WeightTask
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

CREATE TABLE CardioTask
(
    id              SERIAL PRIMARY KEY,
    planid          INT NOT NULL,
    exerciseid      INT NOT NULL,
    time            INT NOT NULL,
    level           INT NOT NULL,
    FOREIGN KEY     (planid) REFERENCES PLAN (id),
    FOREIGN KEY     (exerciseid) REFERENCES Exercise (id)
);

CREATE TABLE WeightTaskInstance
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

CREATE TABLE CardioTaskInstance
(
    id              SERIAL PRIMARY KEY,
    planid          INT NOT NULL,
    exerciseid      INT NOT NULL,
    time            INT NOT NULL,
    level           INT,
    FOREIGN KEY     (planid) REFERENCES PLAN (id),
    FOREIGN KEY     (exerciseid) REFERENCES Exercise (id)
);

