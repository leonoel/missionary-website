# Publishers

A publisher is a stateful object representing a memoized view of an effect and sharing the same properties.

Spawning a publisher is called *subscribing*. A publisher is able to manage multiple active subscriptions, ensure
the underlying effect is not spawned more than once and share the results across subscribers. When the process
terminates, the publisher becomes resolved. From this point on, the underlying effect is discarded and all
subscriptions terminate synchronously with the memoized result.

Publisher contruction, a.k.a. *publishing*, is a [synchronous effect](/core-principles.html#synchronous-effects)
returning a fresh object with an empty memoized state. However, it doesn't perform any action beyond memory allocation,
and its lifecycle is fully managed by its subscribers. Therefore, it can be safely shared across threads and its
disposal can be delegated to the garbage collector.

The API provides 3 publisher constructors : [`memo`](/api/missionary.core/memo.html), [`stream`](/api/missionary.core/stream.html),
and [`signal`](/api/missionary.core/signal.html).

Example : memoization of a task.
```clojure
(def fetch-page (m/memo (m/via m/blk (slurp "https://clojure.org"))))

;; perform get request now
(fetch-page
  (partial prn :success)
  (partial prn :failure))
;; after request completes
:success ",,,"

;; reuse memoized result
(fetch-page
  (partial prn :success)
  (partial prn :failure))
;; immediately
:success ",,,"
```

## Supervision
The subscribers of a publisher are all equally in charge of the supervision of its process.

### Error handling
When a publisher's underlying effect crashes, the publisher memoizes the error and resolves to a crashed state.
Subsequent subscriptions immediately crash with this error.

```clojure
(require '[missionary.core :as m])

;; memoize get request, this one will fail
(def fetch-page (m/memo (m/via m/blk (slurp "http://clojur.org"))))

;; perform get request now
(fetch-page
  (partial prn :success)
  (partial prn :failure))
;; after resolution failure
:failure #error{,,,}

;; reuse memoized error
(fetch-page
  (partial prn :success)
  (partial prn :failure))
;; immediately
:failure #error{,,,}
```

### Graceful shutdown
A publisher spawns its underlying effect on the first subscription and keeps the process alive as long as at least one
subscription is active. When the subscription count falls to zero, the process is cancelled and the publisher state is
reset to its initial state. Values produced by the underlying effect after cancellation are transferred to the last
subscriber.

```clojure
(require '[missionary.core :as m])
(import 'missionary.Cancelled)

;; memoize a long-lived task,
;; recovering from cancellation
(def long-sleep
  (m/memo
    (m/sp
      (try (m/? (m/sleep 60000))
           (catch Cancelled _
             :cancelled)))))

;; spawn the task now
(def ps1
  (long-sleep
    (partial prn :success1)
    (partial prn :failure1)))

;; task is already started, no action taken
(def ps2
  (long-sleep
    (partial prn :success2)
    (partial prn :failure2)))

;; unsubscribe, do not cancel because
;; another subscription is active
(ps1)
:failure #error{,,,}

;; last subscription, do cancel now
(ps2)
:success :cancelled
```

## The subscription DAG

### The publisher order
Publishers are [totally ordered](https://en.wikipedia.org/wiki/Total_order) and support comparison with `compare`.

The publisher order defines which subscriptions are allowed, it is defined as follows :
1. a publisher is inferior to its ancestors
2. a publisher is inferior to its younger siblings

A publisher is the parent of another publisher if the latter was constructed synchronously with an event from the
former. Two publishers are siblings if they're both orphans or if they have the same parent, the younger is the one
constructed after.

A publisher can only subscribe to an inferior publisher. Cycles are therefore not allowed, subscriptions are the edges
of a [directed acyclic graph](https://en.wikipedia.org/wiki/Directed_acyclic_graph) for which the publisher order is
a topological ordering.

Example of publisher ordering rule 1 :
```clojure
(require '[missionary.core :as m])

(def !x (atom 0))

;; <y can subscribe to <x,
;; because <y is an ancestor of <x
(def <y
  (m/signal
    (m/cp (let [<x (m/signal (m/watch !x))]
            (m/?< <x)))))
```

Example of publisher ordering rule 2 :
```clojure
(require '[missionary.core :as m])

(def <x (m/signal (m/cp :foo)))

;; <y can subscribe to <x,
;; because <x and <y are siblings and <x was constructed before <y
(def <y (m/signal (m/latest str <x)))
```

### Propagation turns
A propagation turn is the set of publishers synchronously activated by a given event.

Whenever a publisher emits an event, this event must be dispatched to its subscriptions. Each subscription can in turn
emit synchronous events on a downstream publisher, and so on. The event dispatch ordering matters, and the correct
behavior is to ensure each upstream subscription was activated before allowing a publisher to dispatch its own
synchronous events. Therefore, publisher event dispatch is scheduled according to the publisher order.

Example :
```clojure
(require '[missionary.core :as m])

(def !x (atom 0))

;; <x is activated by watch
(def <x (m/signal (m/watch !x)))

;; <y subscribes to <x twice
(def <y (m/signal (m/latest + <x <x)))

;; reduce subscribes to <y
(def ps
  ((m/reduce (fn [_ x] (prn :> x)) nil <y)
   (partial prn :success)
   (partial prn :failure)))
:> 0
;; the propagator ensures both <x subscriptions
;; are activated before <y can dispatch to reduce
;; when reduce transfers, latest can see both changes
;; the inconsistent state 1 is never observed
(swap! !x inc)
:> 2
```

If a publisher emits an event that synchronously activates an inferior publisher, this publisher is scheduled for
another propagation turn immediately after the current one.