# `missionary.core/compel`

## Usage
* `(compel task)`

## Description
A function [operator](/operators.html) disabling `task` cancellation. When `task` process terminates, `compel` process
terminates with this result. Cancelling a `compel` process has no effect.

## Examples
Ensure an asynchronous effect is performed to completion before terminating
```clojure
(require '[missionary.core :as m])

;; a task emulating a mandatory action
(def cleanup
  (m/sp
    (m/? (m/sleep 100))
    (prn :cleaned-up)))

;; run cleanup in a finally block
;; compel ensures it won't be cancelled
(defn with-cleanup [task]
  (m/sp (try (m/? task)
             (finally
               (m/? (m/compel cleanup))))))

(def main (with-cleanup m/never))

(def ps (main #(prn :success %) #(prn :failure %)))
(ps)
;; (after 100ms)
:cleaned-up
:failure #error{}
```

## Synchronicity
* any `compel` event is synchronous with its `task` counterpart.