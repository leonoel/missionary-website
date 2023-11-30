# `missionary.core/rdv`

## Usage
* `(rdv)` - define a new rendez-vous.
* `r` - used as task, receive a message from rendez-vous `r`.
* `(r x)` - return a task sending message `x` to rendez-vous `r`, completing with `nil`.

## Description
A [port](/ports.html) constructor defining a synchronous rendez-vous. A rendez-vous is an unbuffered communication
channel allowing messages to be transferred from a sender to a receiver when both ends are ready to do so. Sending and
reception are asynchronous effects completing on transfer.

## Examples
```clojure
(require '[missionary.core :as m])

(def r (m/rdv))

(def main
  (m/join (constantly nil)
    (m/sp
      (m/? (r :foo))
      (m/? (r :bar))
      (m/? (r :baz)))
    (m/sp (prn :> (m/? r) (m/? r) (m/? r)))))

(def ps (main #(prn :success %) #(prn :failure %)))
:> :foo :bar :baz
:success nil
```

## Synchronicity
* rendez-vous definition is a synchronous effect.
* rendez-vous send completion is synchronous with receive completion.
* if the sender is ready before the receiver, receive completion is synchronous with receive spawn.
* if the receiver is ready before the sender, send completion is synchronous with send spawn.