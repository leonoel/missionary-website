# `missionary.core/dfv`

## Usage
* `(dfv)` - define a new dataflow variable.
* `v` - used as a task, dereference dataflow variable `v`.
* `(v x)` - assign `x` to dataflow variable `v` if empty, return assigned value.

## Description
A [port](/ports.html) constructor defining a dataflow variable. A variable is initially empty and can be assigned to
an arbitrary value later, at most once. Dereferencing a variable is an asynchronous effect completing on assignment.

Note : `dfv` can be seen as an asynchronous version of `clojure.core/promise`.

## Examples
Dataflow variables can be used for [oz-style declarative concurrency](https://en.wikipedia.org/wiki/Oz_(programming_language)#Dataflow_variables_and_declarative_concurrency)
```clojure
(require '[missionary.core :as m])

(def x (m/dfv))
(def y (m/dfv))
(def z (m/dfv))

(def main
  (m/join (constantly nil)
    (m/sp (prn :> (z (+ (m/? x) (m/? y)))))
    (m/sp (x 40))
    (m/sp (y 2))))

(def ps (main #(prn :success %) #(prn :failure %)))
:> 42
:success nil
```

## Synchronicity
* variable definition is a synchronous effect.
* variable assignment is a synchronous effect.
* before a variable is assigned, dereferencing completion is synchronous with variable assignment.
* after a variable is assigned, dereferencing completion is synchronous with dereferencing spawn.