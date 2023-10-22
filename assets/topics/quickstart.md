# Quickstart

We're going to build up a little reactive DOM driver with Missionary because it's a great concurrency exercise, non trivial, that you are already familiar with.

TODO: insert live demo

What's happening:
* type into input, see successive value of the input flow into a div
* dom element is fully managed, automatic cleanup

Here's the final code for that:

```clojure
(def >input
  (m/relieve {}
    (m/observe (fn [emit!]
                 (let [el (.createElement js/document "input")]
                   (.appendChild js/document.body el)
                   (.addEventListener el "input" emit!)
                   (fn []
                     (.removeEventListener el "input" emit!)
                     (.removeChild js/document.body el)))))))

; todo print to div
```

We're going to build up to it in small pieces.

# Setup your ClojureScript REPL

Clone this starter repo. In the repo, create a new .cljs file and load up missionary:

```clojure
(ns user.m-quickstart
  (:require [missionary.core :as m]))
```

Follow along in your REPL. When you screw something up, refresh the browser page which will wipe the clojurescript runtime state to a clean slate.

TODO Gif of working Calva REPL in readme of repo.

# Discrete flows

Missionary is about flows. A flow is an asynchronous sequence of future values, such as the flow of values coming out of a DOM input as you type into it.

Here, we'll use an atom instead of a dom node (for now).

```clojure
(def !a (atom 0)) ; a variable input
(def >a (m/watch !a)) ; discrete flow of successive values of variable !a
(def >b (m/zip inc >a)) ; mapping `inc` over the discrete flow, i.e. fmap
```

What do we have here?
* `>a` is a flow of successive values from observing atom `!a`, starting at 0
* `>b` is a flow that maps `inc` over those values, so it starts at 1

Ok, evaluate this in your ClojureScript REPL and look for the `1`

```clojure
(def !a (atom 0)) ; => #'user/!a
(def >a (m/watch !a)) ; => #'user/>a
(def >b (m/zip inc >a)) ; => #'user/>b
```

Where is the `1`? What happened? Actually, nothing really happened, we defined some flows, but we didn't run them. So we don't see the `1` yet.

Missionary flows are *values* that describe a dataflow computation. As values, they do not have side effect during construction. Think of them as recipes, exactly like Haskell IO actions. Indeed, Missionary computations are referentially transparent.

# A running flow is an iterative process

This section is low level, so you must run the forms *exactly* as I direct. If you get an error, refresh and start over.

Evaluate this:

```clojure
(def it (>b ; run the flow, returning a process
          #(println ::notify) ; called when a value is available to be sampled
          #(println ::terminate))) ; called when the process terminates
; :user/notify
```

* Missionary flows are programs. A running program is a process.
* `it` (i.e. iterator) is a handle to the running process, that you use to consume successive values as they become ready and notify
* `:user/notify` is printed right away, signalling to the downstream *consumer* (us) that the iterator (running flow) now has a value available to be *consumed*.

Consume the value from the iterator with `deref`:

```clojure
@it ; => 1
```

The process is now parked until the atom changes, at which point it will notify again.

**Can we deref the process again before it notifies? No**, you must not deref a process that is not in notify state, this is a *flow protocol violation*. Here is the [flow protocol](https://github.com/leonoel/flow).

Note: Manipulating raw flow iterators like this is a low level operation. Real world applications don't acutally do this except (maybe) once at the entrypoint. So in practice you won't use this interface much, except when writing tests or playing at the REPL, like right now.

**When will the process next notify?** When the atom changes. Let's do that:

```clojure
(reset! !a 5)
; :user/notify

@it ; => 6

(reset! !a 10)
; :user/notify

@it ; => 1
```

The process will run forever, until you terminate it:

```clojure
; terminate iterator process by invoking as a fn, arity 0
(it) ; => nil
; notify
```

**Why did it notify again?** Because the process has a terminal value. Consume it:

```clojure
(try @it (catch :default e (js/console.error e) e)) ; => #object[Object [object Object]]
; terminate
; missionary.Cancelled {message: 'Watch cancelled.'} -- in browser console
```

**Why the try/catch boilerplate?** Because I knew the final value would be an exception, and the ClojureScript exception isn't serializable so it will get collapsed to [Object object] en route to the remote REPL, so for this REPL transcript I opted to print it with console.log.

**Why is the final value an exception?**


# Backpressure

TODO: multiple atom changes before consume - crash. Why?
TODO: m/relieve - lazy sampling
TODO: multiple atom changes now work


# Continuous flows

# m/observe

Most real world flows are driven by some foreign event producer. `m/observe` is the basic primitive for ingesting foreign events. It encapsulates a callback.

```clojure
(def >x (m/observe (fn ctor [emit!]
                     (println "constructor")
                     (emit! 42) ; an event
                     (fn dtor []
                       (println "destructor")))))
```

* `emit!` is the missionary callback that sends a new event, here `(emit! 42)` will send an event whose value is `42`
* `ctor`
* `dtor`
* `>x`

# implement m/watch

