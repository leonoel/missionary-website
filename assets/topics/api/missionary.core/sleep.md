# `missionary.core/sleep`

## Usage
* `(sleep ms)`
* `(sleep ms value)`

`ms` must be a number. `value` can be anything, default is `nil`.

## Description
An [operator](/operators.html) returning a task performing no action. `sleep` process completes with `value` after `ms`
milliseconds. If `sleep` process is cancelled before this delay was elapsed, it crashes with an instance of
[`Cancelled`](/api/missionary.Cancelled.html).

## Examples
Sleep for 1 second :
```clojure
(require '[missionary.core :as m])

(def sleep-for-1-sec (m/sleep 1000 :foo))

(def ps
  (sleep-for-1-sec
    (partial prn :success)
    (partial prn :failure)))
;; after 1s
:success :foo
```

## Synchronicity
* `sleep` completion is not synchronous with `sleep` spawn
* `sleep` crash is synchronous with `sleep` cancellation

## See also
* [`timeout`](/api/missionary.core/timeout.html)