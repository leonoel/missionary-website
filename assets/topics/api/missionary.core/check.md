# `missionary.core/!`

## Usage
* `(!)`

The interruption checking [synchronizer](/synchronizers.html). Throws an instance of
[`Cancelled`](/api/missionary.cancelled.html) if current evaluation context is interrupted, otherwise returns `nil`.

Example : explicit interruption check when an asynchronous operation doesn't support it.
```clojure
(require '[missionary.core :as m])

;; slurp is not interruptible
(def fetch-page (m/via m/blk (slurp "https://clojure.org")))

;; this is a long-lived task, it must terminate on cancellation
;; the interruption status is polled inside the loop
(def repeat-fetch
  (m/sp
    (loop []
      (m/? fetch-page)
      (m/!) (recur))))

;; without interruption check, the call would never return
;; because timeout waits for repeat-fetch termination
(def main (m/timeout repeat-fetch 500 :timeout))

(def ps (main #(prn :success %) #(prn :failure %)))
:success :timeout
```

## Synchronicity
* interruption check is a synchronous effect.

## See also
* [`sp`](/api/missionary.core/sp.html)
* [`cp`](/api/missionary.core/cp.html)
* [`ap`](/api/missionary.core/ap.html)