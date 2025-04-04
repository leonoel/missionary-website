# `missionary.core/amb=`

## Usage
* `(amb= & exprs)`

## Description
A [synchronizer](/synchronizers.html) derived from [`?>`](/api/missionary.core/flatten.html), evaluating each form
concurrently in a fresh evaluation context derived from the current one and returning results in order of availability.

## Examples
```clojure
(require '[missionary.core :as m])

(def input (m/ap (m/? (m/sleep 1000 (m/amb= :foo :bar)))))

(def main (m/reduce conj input))

(def ps (main #(prn :success %) #(prn :failure %)))
;; after 1 second
:success [:foo :bar]
```

## See also
* [`amb`](/api/missionary.core/amb.html)
* [`ap`](/api/missionary.core/ap.html)