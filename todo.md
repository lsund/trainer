# Deployment

[x] Kill digitalocean apps
[x] reimport database
[x] Upgrade heroku plan

# General

[x] Doneexercise should store sets, reps and weight for historic data
[x] Adding an exercise should not update the 'exercise'. It should instead be
    possible to update it directly
[x] Remove plancardio etc. planexercise should take a third parameter,
    exercisetype. This should be an integer, determining a lookup in a new
    table, called exercisetype. It will be something like
        name        |   id
        strength    |   1
        timedcardio |   2
        interval    |   3
        distance    |   4
    Each has a corresponding table with different properties. Every exercise
    can be added to the planexercise table, no matter what type it is.
[x] Fix the todos in render
[x] add-exercise and update-exercise is now specific to weightlift. It needs to
    adapted. Probably use the exercise type here.
[x] Add squash opponent through user interface
[x] Use time strings eg 3m45s instead of raw integers in UI.
    [x] util/duration-str->int
    [x] util/int->duration-str
    [x] Write a migration from ints to duration str in database
        [x] Move old duration column name to duration2
        [x] create new column named duration
        [x] Do migration
        [x] Delete duration2
    [x] Update cardio table as well
    [x] update plotter to use duration-str->int before generating plots
[x] Why do I have cardio and donecardio with the same arguments? I could just
    reference one. Same with weightlift and doneweightlift
[x] Bug in update duration.
[x] When adding plan, cardios do not get added
[x] http call to handler does not work
[x] Should be possible to update the weights directly in /save-plan
    [x] Add a function increment that always increments a weight/cardio acc.
        to a specific rule
    [x] Add this to /save-plan interface

[ ] calls to db module should only be done in handler (program entry)
[ ] Generate training graphs from squash-results or maybe statistics statistics
[ ] Shold not be able to add the same exercise to plan
[ ] Send email to ask for current plan
[ ] Send email for update
