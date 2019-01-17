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
[ ] Why do I have cardio and donecardio with the same arguments? I could just
    reference one. Same with weightlift and doneweightlift
[ ] Generate training graphs from squash-results or maybe statistics statistics
[ ] Shold not be able to add the same exercise to plan
[ ] Send email to ask for current plan
[ ] Send email for update
