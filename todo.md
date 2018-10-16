[x] Doneexercise should store sets, reps and weight for historic data
[x] Adding an exercise should not update the 'exercise'. It should instead be
    possible to update it directly
[ ] Shold not be able to add the same exercise to plan
[ ] Fix the todos in render
[ ] Remove plancardio etc. planexercise should take a third parameter,
    exercisetype. This should be an integer, determining a lookup in a new
    table, called exercisetype. It will be something like
        name        |   id
        strength    |   1
        timedcardio |   2
        interval    |   3
        distance    |   4
    Each has a corresponding table with different properties. Every exercise
    can be added to the planexercise table, no matter what type it is.
