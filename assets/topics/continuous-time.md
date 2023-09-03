# Continuous time

An entity has continuous time semantics if its state is defined at every point in time. The lifecycle of a continuous
variable involves a succession of mutations and samplings, where each mutation invalidates the previous state, and
sampling can be delayed to the last moment. Because it is always in a valid state, readers and writers do not have to
synchronize their transfer rate. Continuous variables can be composed freely, without any consideration of how and when
changes are performed and observed.

Temporal continuity can be seen as a generalization of [clojure's approach to identity and state](https://clojure.org/about/state).
All clojure reference types, and by extension all databases, have continuous time semantics. It is one of the core
principles of the original formulation of [FRP](https://en.wikipedia.org/wiki/Functional_reactive_programming) and is a
special case of the more general idea of *resolution independance*. [According to Conal Elliott](http://conal.net/blog/posts/why-program-with-continuous-time) :

> Another name for “continuous” is “resolution-independent”, and thus able to be transformed in time and space with
> ease and without propagating and amplifying sampling artifacts.

Note : Is missionary FRP ? It turns out missionary's approach is significantly different from [purely functional FRP](https://wiki.haskell.org/Functional_Reactive_Programming).
Moreover, another core principle of FRP is denotational design, which is defined in subjective or relative terms like
simplicity, rigor, and elegance. Until missionary gets its denotational accreditation by the FRP police, it should be
considered, at best, FRP-inspired. Nevertheless, the concept you should ultimately care about is continuous time
because that's what makes your reactive programs composable.

## Continuous flow definition
In missionary, a continuous variable is described as a flow of successive states, with additional properties :
- *initialized* : it is immediately ready to transfer on boot.
- *decoupled* : its transfer rate has no influence on upstream.

These properties define an implicit contract between producers and consumers. If a consumer expects a continuous flow,
the flow must validate these properties. If a producer provides a continuous flow, the flow is guaranteed to validate
these properties. A flow that doesn't validate these properties is said to be discrete.

Note : initialization is usually checked by operators, but decoupling is not. Providing a coupled flow to a continuous
consumer will not result in a failure but can lead to unexpected results (e.g. backpressure errors, stale values).

## Building continuous flows

### From a reference
[`watch`](/api/missionary.core/watch.html) can be used to reflect the state of any reference. References are [ports](/ports.html),
they are fundamentally imperative and should therefore be used sparingly. Valid use cases for references include
interoperability and cycle definitions.

Example : define the successive states of a reference.
```clojure
(def !x (atom 0))
(def x-state (m/watch !x))
```

### From an event stream
The general strategy to define a continuous flow from a discrete flow of events is :
1. Define the successive states as a reduction of the event stream : `(m/reductions rf init events)`
2. Relieve the reduction to discard all states but the latest : `(m/relieve {} states)`

Note : `{}` is called *discard* and is just a concise way to express `(fn [_ x] x)`. This function is the essence of
continuous time, so get used to it !

Example : compute how many times the user clicked on the browser window.
```clojure
;; discrete
(def click-events
  (m/observe
    (fn [!]
      (.addEventListener js/window "click" !)
      (.removeEventListener js/window "click" !))))

;; continuous
(def click-count
  (->> click-events
    (m/reductions (fn [count _] (inc count)) 0)
    (m/relieve {})))
```

## Continuous flow composition

Missionary provides functions consuming and producing continuous flows, and therefore preserving the continuous flow
contract. That's the true benefit of this abstraction - as long as your problem can be described as a composition of
functions in continuous time, you can rely on these operators and forget about the contract.

### Derivation
[`latest`](/api/missionary.core/latest.html) is the continuous time concurrent [operator](/operators.html).
Use it to derive continuous flows from other ones by application of a function to the current states.

Example : compute the sum of two inputs.
```clojure
(def !x (atom 0))
(def !y (atom 0))
(def x+y (m/latest + (m/watch !x) (m/watch !y)))
```

### Sharing
[`signal`](/api/missionary.core/signal.html) is the continuous time [publisher](/publishers.html). Use it to memoize
the current state of a continuous flow in order to share it with multiple consumers. `latest` and `signal` are
sufficient to represent all static [DAG](https://en.wikipedia.org/wiki/Directed_acyclic_graph)s, providing respectively
fan-in and fan-out.

Example : the classic diamond test - one node depends on another through multiple paths. A correct implementation must
not leak intermediate computations (a.k.a. glitches), therefore the resulting state must always be `true`.
```clojure
(def !input (atom 0))
(def <input (m/signal (m/watch !input)))
(def <diamond (m/signal (m/latest = <input <input)))
```

### Switching
[`cp`](/api/missionary.core/cp.html) is the continuous time sequential [operator](/operators.html). Use it to define
dynamic topologies.

Example : depending on a variable flag, reflect the state of an input port or invariably stay `nil`.
```clojure
(def !flag (atom false))
(def <flag (m/signal (m/watch !flag)))
(def <maybe-input (m/signal (m/cp (when (m/?< <flag) (m/?< <input)))))
```

Note for category theorists : `latest` implements [applicative functor](https://en.wikipedia.org/wiki/Applicative_functor),
`cp` implements [monad](https://en.wikipedia.org/wiki/Monad_(functional_programming)), and `signal` provides
[memoization](https://en.wikipedia.org/wiki/Memoization).

## Using continuous flows
Continuous flows are flows. Therefore, all flow consumers accept continuous flows and see them as a succession of
states.

### Reacting to state changes
In general, it is not recommended to perform side effects in the function provided to `latest` or in the `cp` body.
Effects will be run on sampling, so if you care about ordering of effects you're making an implicit assumption about
the sampling rate, which breaks the principle of resolution independence.

On the other hand, running effects in the discrete part of the pipeline is perfectly safe.

Example : use [`reduce`](/api/missionary.core/reduce.html) to print successive states of a continuous flow. The state
will be sampled as fast as possible, i.e. for each successive change.
```clojure
(m/reduce (fn [_ x] (println "clicked" x "times")) click-count)
```

### Controlled sampling
[`sample`](/api/missionary.core/sample.html) can consume continuous flows and inject the state in an event stream. The
sampling rate is synchronized with the stream transfer rate.

Example : capture the state of an input whenever the user clicks on the browser window.
```clojure
(m/sample (fn [x _] x) <maybe-input click-events)
```
