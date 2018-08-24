# myblog

This is a myblog for clojure web project based on stuartsierra/component

# Setup

1. Run `agopen template` and change the string template everywhare to
   'projectname'
2. rename src/{clj,cljs}/template to src/{clj,cljs}/projectname and
3. `lein run`
4. Optionally specify the database configuration under [:db :name]
5. Configure new process:
    - Port
    - Database name
6. Launch emacs, enter the program and hit C-c j, \R
7. Go to localhost:port
8. Start hacking
