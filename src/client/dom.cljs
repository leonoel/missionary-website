(ns client.dom
  (:require [hyperfiddle.incseq :as i]
            [missionary.core :as m]
            [goog.events :as e]))

(defn element [t]
  (.createElement js/document t))

(defn text [s]
  (.createTextNode js/document s))

(defn set-text! [n s]
  (set! (.-nodeValue n) s))

(defn get-text! [n]
  (.-nodeValue n))

(defn spine-mount [s k]
  (fn [e]
    (m/observe
      (fn [_]
        (s k {} e)
        #(do (s k {} nil))))))

(defn events [e t]
  (m/observe
    (fn [!]
      (e/listen e t !)
      #(e/unlisten e t !))))

(defn mount-items [element {:keys [grow shrink degree permutation change]}]
  (let [children (.-childNodes element)
        move (i/inverse permutation)
        size-before (- degree grow)
        size-after (- degree shrink)]
    (loop [i size-before
           c change]
      (if (== i degree)
        (reduce-kv
          (fn [_ i e]
            (.replaceChild element e
              (.item children (move i i))))
          nil c)
        (let [j (move i i)]
          (.appendChild element (c j))
          (recur (inc i) (dissoc c j)))))
    (loop [p permutation
           i degree]
      (if (== i size-after)
        (loop [p p]
          (when-not (= p {})
            (let [[i j] (first p)]
              (.insertBefore element (.item children j)
                (.item children (if (< j i) (inc i) i)))
              (recur (i/compose p (i/rotation i j))))))
        (let [i (dec i)
              j (p i i)]
          (.removeChild element (.item children j))
          (recur (i/compose p (i/rotation i j)) i))))
    element))

(defn clock [d]
  (m/ap
    (loop []
      (m/amb nil
        (do (m/? (m/sleep d))
            (recur))))))

(def document-changes
  (if js/MutationObserver
    (m/observe
      (fn [on-change]
        (on-change nil)
        (let [obs (js/MutationObserver. on-change)]
          (.observe obs js/document
            (js-obj
              "childList" true
              "subtree" true))
          #(.disconnect obs))))
    (clock 100)))

(defn wait-for [f]
  (->> document-changes
    (m/sample (fn [_] (f)))
    (m/eduction (remove nil?))
    (m/reduce (comp reduced {}) nil)))
