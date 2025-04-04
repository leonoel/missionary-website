# `missionary.core/?`

## Usage
* `(? task)`

## Description
The asynchronous parking [synchronizer](/synchronizers.html). A `task` process is spawned and its result is returned
when it completes. If the process crashes, the error is rethrown.

## Examples
Synchronize on task completion :
```clojure
(require '[missionary.core :as m])

(def main (m/sp (m/? (m/sleep 1000)) :foo))

(def ps (main #(prn :success %) #(prn :failure %)))
;; after 1s
:success :foo
```

Recover from task failure :
```clojure
(require '[missionary.core :as m])
(import 'java.io.IOException)

(def fetch-page (m/via m/blk (slurp "https://clojur.org")))
(def main
  (m/sp
    (try (m/? fetch-page)
         (catch IOException _
           :io-error))))

(def ps (main #(prn :success %) #(prn :failure %)))
:success :io-error
```

## Synchronicity
* `task` spawn is synchronous with park call
* park return is synchronous with `task` completion

## See also
* [`sp`](/api/missionary.core/sp.html)
* [`ap`](/api/missionary.core/ap.html)