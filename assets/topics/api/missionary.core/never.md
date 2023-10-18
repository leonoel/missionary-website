# `missionary.core/never`

## Usage
* `never`

A value [operator](/operators.html) describing a task performing no action and never completing. When `never` process
is cancelled, it crashes with an instance of [`Cancelled`](/api/missionary.cancelled.html).

Example :
```clojure
(require '[missionary.core :as m])

;; perform the task
(def ps
  (m/never
    (partial prn :success)
    (partial prn :failure)))

;; cancel the process
(ps)
;; :failure #error
```

## Synchronicity
* `never` crash is synchronous with `never` cancellation