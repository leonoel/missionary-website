# `missionary.core/reduce`

## Usage
* `(reduce rf flow)`
* `(reduce rf init flow)`

## Description
A function [operator](/operators.html) returning a task completing with the reduction of `flow` values by function `rf`
with optional initial state `init`. If initial state is not provided, `rf` must provide it via its zero arity. The
`flow` process is consumed as fast as possible to feed the reduction. When the reducing function returns a `reduced`
state or throws, the `flow` process is cancelled and its subsequent values are ignored.

## Examples
Simple reduction :
```clojure
(require '[missionary.core :as m])

(def input (m/seed (range 10)))

(def ps
  ((m/reduce + input)
   (partial prn :success)
   (partial prn :failure)))
:success 45
```

Take first value :
```clojure
(require '[missionary.core :as m])

(def input (m/ap (m/? (m/sleep 1000 (m/amb :a :b :c)))))

(def ps
  ((m/reduce (comp reduced {}) nil input)
   (partial prn :success)
   (partial prn :failure)))
;; after 1s
:success :a
```

## Synchronicity
* `flow` spawn is synchronous with `reduce` spawn
* `flow` transfers are synchronous with `flow` steps
* `reduce` completion is synchronous with `flow` termination
* `flow` cancellation is synchronous with `reduce` cancellation or any `flow` transfer returning a value causing the
reduction step to early terminate, whichever comes first