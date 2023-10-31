# Missionary
Supervised dataflow programming for clojure and clojurescript.

```clojure
(require '[missionary.core :as m])

(def !input (atom 1))
(def main                                      ; this is a reactive computation, the println reacts to input changes
  (let [<x (m/signal (m/watch !input))         ; continuous signal reflecting atom state
        <y (m/signal (m/latest + <x <x))]      ; derived computation, diamond shape
    (m/reduce (fn [_ x] (prn x)) nil <y)))     ; discrete effect performed on successive values

(def dispose!
  (main
    #(prn ::success %)
    #(prn ::crash %)))                         ; prints 2
(swap! !input inc)                             ; prints 4
                                               ; Each change on the input propagates atomically through the graph.
                                               ; 3 is an inconsistent state and is therefore not computed.

(dispose!)                                     ; cleanup, deregisters the atom watch
```

For full-stack web developers who struggle to build correct and glitch-free applications, Missionary is a reactive programming toolkit based on process supervision, that lets you build massively concurrent, fine-grained reactive systems on both frontend and backend. Unlike promises and go-routines that make it easy to write a broken program, Missionary’s combinators provide the right set of constraints to let you build a fully composable program that is also correct and bug-free.

[Quickstart](https://github.com/dustingetz/missionary-quickstart/blob/main/src/quickstart.cljs)

[Documentation](/documentation.html)

[Source](https://github.com/leonoel/missionary)

Support : clojurians slack #missionary

> [Above all, I hope we don't become missionaries. - Alan J. Perlis](https://mitp-content-server.mit.edu/books/content/sectbyfn/books_pres_0/6515/sicp.zip/full-text/book/book-Z-H-3.html)