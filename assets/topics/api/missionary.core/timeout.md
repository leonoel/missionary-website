# `missionary.core/timeout`

## Usage
* `(timeout task ms)`
* `(timeout task ms value)`

`ms` must be a number. `value` can be anything, default is `nil`.

## Description
A function [operator](/operators.html) returning a temporally bounded view of given `task`. If `task` process
terminates within `ms` milliseconds, `timeout` process terminates with this result. Otherwise, `task` process is
cancelled and `timeout` process completes with `value`.

## Examples
Ensure a task terminates within 500ms :
```clojure
(require '[missionary.core :as m])

(def sleep-for-1-sec (m/sleep 1000 :foo))

(m/? (m/timeout sleep-for-1-sec 500 :bar))
:= :bar
```

## Synchronicity
* `task` spawn is synchronous with `timeout` spawn
* `task` termination is synchronous with `timeout` termination

## See also
* [`sleep`](/api/missionary.core/sleep.html)