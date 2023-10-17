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