# `missionary.core/attempt`

## Usage
* `(attempt task)`

## Description
Returns a task always succeeding with a zero-argument function returning the result of `task`. If the `task` process
failed, the function rethrows the exception.

## Examples
Implementation of `all-settled`, an operator running tasks concurrently and collecting all results in a vector of
thunks no matter their status.
```clojure
(require '[missionary.core :as m])
(import java.io.IOException)

(defn all-settled [& tasks]
  (->> tasks
    (map m/attempt)
    (apply m/join vector)))

(def task1 (m/via m/blk (slurp "https://clojur.org")))
(def task2 (m/via m/blk (slurp "https://clojure.org")))

;; returns the result of task1 if successful,
;; recover with the result of task2.
(def main
  (m/sp
    (let [[x y] (m/? (all-settled task1 task2))]
      (try (x) (catch IOException _ (y))))))

(def ps (main #(prn :success %) #(prn :failure %)))
;; after server response
:success ",,,"
```

## See also
* [`absolve`](/api/missionary.core/absolve.html)