# `missionary.core/sem`

## Usage
* `(sem)` - define a new binary semaphore.
* `(sem n)` - define a new counting semaphore with `n` initial permits.
* `s` - used as a task, acquire a permit from semaphore `s`.
* `(s)` - release a permit to semaphore `s`, return `nil`.

## Description
A [port](/ports.html) constructor defining a [semaphore](https://en.wikipedia.org/wiki/Semaphore_(programming)). A
semaphore maintains a set of permits which can be acquired and released. Acquisition is an asynchronous effect
completing with `nil` when the permit is made available.

Note : [`holding`](/api/missionary.core/holding.html) is a simple wrapper around [`?`](/api/missionary.core/_.html) and
`try`/`finally` that can be used to make sure every acquired permit is eventually released. 

## Examples
[Dining philosophers](https://en.wikipedia.org/wiki/Dining_philosophers_problem) with forks represented as binary
semaphores and philosophers as `sp` blocks. This is the resource hierarchy solution, nested semaphores are acquired in
a consistent order to prevent deadlocks.
```clojure
(require '[missionary.core :as m])

(defn phil [id f1 f2]
  (m/sp
    (loop []
      (prn id :thinking)
      (m/? (m/sleep 50))
      (m/holding f1
        (m/holding f2
          (prn id :eating)
          (m/? (m/sleep 70))))
      (recur))))

(def forks (vec (repeatedly 5 m/sem)))

(def dinner
  (m/join (constantly nil)
    (phil 'descartes (forks 0) (forks 1))
    (phil 'hume      (forks 1) (forks 2))
    (phil 'plato     (forks 2) (forks 3))
    (phil 'nietzsche (forks 3) (forks 4))
    (phil 'kant      (forks 0) (forks 4))))

(def main (m/timeout dinner 300))

(def ps (main #(prn :success %) #(prn :failure %)))
descartes :thinking
hume :thinking
plato :thinking
nietzsche :thinking
kant :thinking
descartes :eating
plato :eating
kant :eating
descartes :thinking
hume :eating
plato :thinking
hume :thinking
nietzsche :eating
descartes :eating
kant :thinking
descartes :thinking
kant :eating
plato :eating
nietzsche :thinking
:success nil
```

## Synchronicity
* semaphore definition is a synchronous effect.
* semaphore permit releasing is a synchronous effect.
* when a semaphore has available permits, acquisition completion is synchronous with acquisition spawn.
* when a semaphore has no more permits, acquisition completion is synchronous with the releasing of delivered permit.

## See also
* [`holding`](/api/missionary.core/holding.html)