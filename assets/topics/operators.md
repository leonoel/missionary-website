# Operators

The vast majority of the missionary API consists of operators allowing for functional effect composition. They can take
several forms :
* value operators - a trivial effect that can be used as-is, e.g. [`never`](/api/missionary.core/never.html) [`none`](/api/missionary.core/none.html)
* macro operators - a macro returning an effect, usually from expressions to be evaluated, e.g. [`via`](/api/missionary.core/via.html) [`sp`](/api/missionary.core/sp.html) [`cp`](/api/missionary.core/cp.html) [`ap`](/api/missionary.core/ap.html)
* function operators - any pure function deriving an effect from values, which may be other effects.

In general, effects do not have equality semantics, like any other function. However, a composition of effects based on
operators is always referentially transparent, so multiple instances of a composite effect created from the same inputs
describe the same behavior and can therefore be considered the same effect, as far as `=` is not involved. In other
words, a composite effect has no identity. It can be spawned many times, the same actions will be performed over and
over again.

Example : A single task is spawned many times, each time performing a new attempt. Compare this behavior with `future`.
```clojure
(require '[missionary.core :as m])
(import 'java.io.IOException)

(defn fetch-with-retry [max-retries task]
  (m/sp
    (loop [i 0]
      (if (< i max-retries)
        (if-some [page (try (m/? task) (catch IOException _))]
          page (recur (inc i)))
        (throw (ex-info "Too many attempts." {}))))))

(def ps
  ((fetch-with-retry 3 (m/via m/blk (slurp "https://clojur.org")))
   (partial prn :success)
   (partial prn :failure)))
;; after 3 attempts
:failure #error{,,,}
```

When instead the results of an effect must be shared across multiple consumers, the pure functional approach is not
suitable anymore, because it requires to assign an identity to this effect. It has to be stated explicitly, via a
[publisher](/publishers.html) constructor.