# `missionary.core/sp`

## Usage
* `(sp & body)`

A macro [operator](/operators.html) returning a task evaluating `body` in an implicit `do`, in a context supporting
interruption check and asynchronous park. `sp` process completes with the evaluation result, or crashes if an exception
is thrown. Cancelling `sp` process interrupts the evaluation context.

Example : a task performing a synchronous effect.
```clojure
(require '[missionary.core :as m])

(def prn-foo (m/sp (prn :foo)))

(m/? prn-foo)   ;; :foo
:= nil
```

Example : transform the result of task.
```clojure
(require '[missionary.core :as m])

(defn map-name [task]
  (m/sp (name (m/? task))))

(m/? (map-name (m/sleep 1000 :foo)))
:= "foo"
```

Example : recover from a task failure.
```clojure
(require '[missionary.core :as m])
(import 'java.io.IOException)

(defn recover-io [task]
  (m/sp (try (m/? task)
             (catch IOException _
               :io-failure))))

(m/? (recover-io (m/via m/blk (slurp "https://clojur.org"))))
:= :io-failure
```

## Synchronicity
* `sp` completion is synchronous with final `body` expression return
* any `body` expression evaluation is synchronous with the return of the parking expression preceding it in program
order, if it exists. Otherwise, it is synchronous with `sp` spawn.

## See also
* [Synchronizers](/synchronizers.html)
* [`!`](/api/missionary.core/!.html)
* [`?`](/api/missionary.core/?.html)