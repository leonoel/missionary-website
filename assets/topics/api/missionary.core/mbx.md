# `missionary.core/mbx`

## Usage
* `(mbx)` - define a new mailbox.
* `m` - used as a task, dequeue a message from mailbox `m`.
* `(m x)` - enqueue message `x` to mailbox `m`, return `nil`.

## Description
A [port](/ports.html) constructor defining an unbounded, non-blocking FIFO mailbox. A mailbox is initially empty, then
messages can be enqueued synchronously. Dequeuing is an asynchronous effect completing with the oldest message when the
mailbox has some.

## Examples
```clojure
(require '[missionary.core :as m])

(def m (m/mbx))

(def main (m/sp (prn :> (m/? m) (m/? m) (m/? m))))

(def ps (main #(prn :success %) #(prn :failure %)))
(m :foo)
(m :bar)
(m :baz)
:> :foo :bar :baz
:success nil
```

## Synchronicity
* mailbox definition is a synchronous effect.
* mailbox enqueuing is a synchronous effect.
* when the mailbox is empty, dequeuing completion is synchronous with enqueuing.
* when the mailbox is not empty, dequeuing completion is synchronous with dequeuing spawn.