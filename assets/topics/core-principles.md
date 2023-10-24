# Core principles

Missionary is a synchronous functional effect system. A *functional effect system* is a means to represent effects as
values and derive composite effects from primitive ones. *synchronous* means the composition model includes the notion
of simultaneity, i.e. when two events are considered to happen at the same logical instant due to a causal relationship.
Effects describe dynamic dataflow graphs, i.e. how to set up and maintain a variable set of processes and how data must
be transferred from one to another.

Effects can be synchronous or asynchronous. In the context of a value, an effect is implicitly asynchronous and
designates a function conforming to one of the two effect protocols, *task* and *flow*. Tasks produce a single result,
flows produce many results and support flow control via a push-pull transfer mechanism. Both define an immutable recipe
to produce values that may not be immediately available, and implement a shared protocol to interact with the consumer,
i.e. the next stage of the pipeline. They also support graceful shutdown and failure.

Purely functional [effect composition](/operators.html) with [optional memoization](/publishers.html) is expressive
enough to represent any dynamic [DAG](https://en.wikipedia.org/wiki/Directed_acyclic_graph) dataflow topology with
supervision, and cycles can be implemented in userland using [ports](/ports.html). The synchronous nature of the
composition model, associated with bidirectional flow transfers, allows for atomic propagation of change, a.k.a.
[glitch](https://en.wikipedia.org/wiki/Reactive_programming#Glitches) avoidance.

## Supervised dataflow
The production of asynchronous values implies two major constraints :
* allocation of resources that must be eventually released, including all shared resources that need to keep track
of event handlers. When the dataflow graph is dynamic, resources that are not needed any more must be disposed, because
bad cleanup discipline leads to suboptimal resource usage and memory leaks.
* dealing with the uncertainty of the environment, including the fact that events may not happen within a reasonable
time span or not at all due to some temporary unavailability. This materializes as errors, which require appropriate
reporting and recovering strategies with sane defaults.

Supervision is the general concept behind the management of these constraints, it is a natural fit for functional
effect systems because effect composition captures the dataflow topology, which defines where errors must propagate (in
the same direction as data) and how to recursively cancel the successive stages (in the opposite direction).

## Synchronous effects
A synchronous effect is an action that is performed immediately. The lifecycle of asynchronous effects is described as
a succession of synchronous effects over time, and the composition model also supports userland synchronous effects.
While it is possible to wrap synchronous effects in tasks or flows, doing so has no benefit because they do not need to
be managed. Instead, synchronous effects should be provided to operators allowing to evaluate user code.

Example : perform the `prn` synchronous effect in the reducing function passed to `reduce`
```clojure
(require '[missionary.core :as m])

(def !x (atom 0))

(def ps
  ((m/reduce (fn [_ x] (prn :> x)) nil (m/watch !x))
   (partial prn :success)
   (partial prn :failure)))
:> 0
(swap! !x inc)
:> 1
```

Note : in the missionary API, [publisher](/publishers.html) and [port](/ports.html) constructor calls are synchronous
effects.

## Computation costs
Missionary synchronicity semantics relies on detection of reentrant synchronous effects. For this reason, user code
evaluation is never implicitly transferred to another thread, and the system will never make the decision to run any
parts of the program concurrently if not explicitly told to do so. This mechanism makes the pervasive assumption that
the computation cost of user-provided code is negligible, and the burden of checking is left to the programmer.

Any expensive computation is asynchronous by definition, and should be bound to a `java.util.concurrent.Executor` using
[`via`](/api/missionary.core/via.html). This is especially true for blocking operations, but can also apply to
CPU-intensive operations. Computation cost is an engineering concern that is not directly related to thread blocking.
In practice, some blocking operations can be considered inexpensive, e.g. acquiring a mutex under low contention. A
cost is relative to a time budget, what is cheap in some context may not be acceptable in another.