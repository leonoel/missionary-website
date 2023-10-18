# `missionary.core/compel`

## Usage
* `(compel task)`

A function [operator](/operators.html) returning a task spawning given `task`. When `task` process terminates,
`compel` process terminates with this result. Cancelling a `compel` process has no effect.

Example : ensure an asynchronous effect is performed to completion before terminating
```clojure
(require '[missionary.core :as m])

;; a task emulating a mandatory action
(def cleanup (m/sleep 100))

;; run cleanup in a finally block
;; compel ensures it won't be cancelled
(defn with-cleanup [task]
  (m/sp (try (m/? task)
             (finally
               (m/? (m/compel cleanup))
               (prn :cleaned-up)))))

;; perform the task
(def ps
  ((with-cleanup m/never)
   (partial prn :success)
   (partial prn :failure)))

;; cancel the process
(ps)
;; (after 100ms)
;; :cleaned-up
;; :failure #error
```

## Synchronicity
* any `compel` event is synchronous with its `task` counterpart.