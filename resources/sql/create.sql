CREATE TABLE ExerciseType
(
    id              INT NOT NULL,
    name            varchar(64) NOT NULL UNIQUE
);

CREATE TABLE Cardio
(
    id              SERIAL PRIMARY KEY,
    name            varchar(64) NOT NULL UNIQUE,
    duration        varchar(64),
    distance        INT,
    lowpulse        INT,
    highpulse       INT,
    level           INT
);

CREATE TABLE WeightLift
(
    id              SERIAL PRIMARY KEY,
    name            varchar(64) NOT NULL UNIQUE,
    sets            INT NOT NULL,
    reps            INT NOT NULL,
    weight          INT NOT NULL
);

CREATE TABLE Plan
(
    id              SERIAL PRIMARY KEY,
    name            varchar(64) NOT NULL UNIQUE,
    timescompleted  INT NOT NULL,
    active          BOOL NOT NULL
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

CREATE TABLE DoneWeightlift
(
    id              SERIAL PRIMARY KEY,
    day             DATE NOT NULL,
    exerciseid      INT NOT NULL,
    planid          INT NOT NULL,
    sets            INT NOT NULL,
    reps            INT NOT NULL,
    weight          INT NOT NULL,
    FOREIGN KEY     (planid) REFERENCES PLAN (id),
    FOREIGN KEY     (exerciseid) REFERENCES Weightlift (id)
);

CREATE TABLE DoneCardio
(
    id              SERIAL PRIMARY KEY,
    day             DATE NOT NULL,
    exerciseid      INT NOT NULL,
    planid          INT NOT NULL,
    duration        varchar(64),
    distance        INT,
    highpulse       INT,
    lowpulse        INT,
    level           INT,
    exercisetype    INT,
    FOREIGN KEY     (planid) REFERENCES PLAN (id),
    FOREIGN KEY     (exerciseid) REFERENCES Cardio (id)
);

CREATE TABLE SquashOpponent
(
    id              SERIAL PRIMARY KEY,
    name            varchar(64) UNIQUE
);

CREATE TABLE SquashResult
(
    id              SERIAL PRIMARY KEY,
    day             DATE NOT NULL,
    opponentid      INT NOT NULL,
    myscore         INT NOT NULL,
    opponentScore   INT NOT NULL,
    FOREIGN KEY     (opponentid) REFERENCES SquashOpponent (id)
);
