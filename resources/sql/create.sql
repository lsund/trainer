CREATE TABLE WeightLift
(
    id              SERIAL PRIMARY KEY,
    name            varchar(64) NOT NULL UNIQUE,
    sets            INT NOT NULL,
    reps            INT NOT NULL,
    weight          INT NOT NULL
);

CREATE TABLE ExerciseType
(
    id              INT NOT NULL,
    name            varchar(64) NOT NULL UNIQUE
);

INSERT INTO exercisetype VALUES (1, 'weightlift');
INSERT INTO exercisetype VALUES (2, 'timedcardio');
INSERT INTO exercisetype VALUES (3, 'intervalcardio');
INSERT INTO exercisetype VALUES (4, 'distancecardio');

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
    name            varchar(64) NOT NULL UNIQUE,
    timescompleted  INT NOT NULL
);

CREATE TABLE PlannedExercise
(
    id              SERIAL PRIMARY KEY,
    exerciseid      INT NOT NULL,
    exercisetype    INT NOT NULL,
    planid          INT NOT NULL,
    FOREIGN KEY     (planid) REFERENCES PLAN (id),
    FOREIGN KEY     (exerciseid) REFERENCES Exercise (id)
);

CREATE TABLE DoneExercise
(
    id              SERIAL PRIMARY KEY,
    day             DATE NOT NULL,
    exerciseid      INT NOT NULL,
    exercisetype    INT NOT NULL,
    planid          INT NOT NULL,
    sets            INT NOT NULL,
    reps            INT NOT NULL,
    weight          INT NOT NULL,
    FOREIGN KEY     (planid) REFERENCES PLAN (id),
    FOREIGN KEY     (exerciseid) REFERENCES Exercise (id)
);
