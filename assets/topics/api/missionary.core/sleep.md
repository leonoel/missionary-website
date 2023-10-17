# `missionary.core/sleep`

## Usage
* `(sleep ms)`
* `(sleep ms value)`

An [operator](/operators.html) returning a task performing no action. `sleep` process completes with `value` after `ms`
milliseconds. If `value` is not provided, `sleep` process completes with `nil`. If `sleep` process is cancelled before
this delay was elapsed, it crashes with an instance of [`Cancelled`](/api/missionary.Cancelled.html).

Example : sleep for 1 second
```clojure
(require '[missionary.core :as m])

(def sleep-for-1-sec (m/sleep 1000 :foo))

(m/? sleep-for-1-sec)
:= :foo
```

## Synchronicity
* `sleep` completion is not synchronous with `sleep` spawn
* `sleep` crash is synchronous with `sleep` cancellation

## See also
* [`timeout`](/api/missionary.core/timeout.html)