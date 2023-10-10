(ns client.main
  (:refer-clojure :exclude [time])
  (:require [client.dom :as d]
            [client.net :as n]
            [clojure.string :as str]
            [hyperfiddle.incseq :as i]
            [missionary.core :as m]
            [goog.style :as s])
  (:import (goog.dom TagName NodeType)
           (goog.events EventType))
  (:require-macros [client.main :refer [topics]]))

(defn get-article [path info]
  (m/sp (assoc info :article (-> (str "/" path ".html")
                               (n/fetch "document")
                               (m/?)
                               (.getElementsByTagName "article")
                               (.item 0)))))

(defn join-map [m]
  (apply m/join (fn [& xs] (zipmap (keys m) xs)) (vals m)))

(defn map-vals [f m]
  (reduce-kv (fn [m k v] (assoc m k (f k v))) {} m))

(def get-articles
  (->> (topics)
    (map-vals get-article)
    (join-map)
    (m/memo)))

(defn score [[path {:keys [article]}] <needle]
  (let [title (.item (.getElementsByTagName article "h1") 0)
        title-child (.item (.-childNodes title) 0)
        suggestion (d/element "div")
        highlight (d/element "span")
        before-highlight (d/text "")
        inside-highlight (d/text "")
        after-highlight (d/text "")]
    (s/setStyle highlight "background" "yellow")
    (.appendChild highlight inside-highlight)
    (.appendChild suggestion title-child)
    [suggestion
     (if-some [reference (when (and (= (.-nodeType title-child) (.-ELEMENT NodeType))
                                 (= (.-CODE TagName) (.-tagName title-child)))
                           (d/get-text! (.item (.-childNodes title-child) 0)))]
       (let [exact (name (symbol reference))]
         (.replaceChildren title-child)
         (.appendChild title-child before-highlight)
         (.appendChild title-child highlight)
         (.appendChild title-child after-highlight)
         (m/latest
           (fn [needle]
             (if-some [start (when-not (= "" needle) (str/index-of reference needle))]
               (let [end (+ start (count needle))]
                 (d/set-text! before-highlight (subs reference 0 start))
                 (d/set-text! inside-highlight (subs reference start end))
                 (d/set-text! after-highlight (subs reference end))
                 (if (= exact needle) 2 1))
               (do (d/set-text! before-highlight "")
                   (d/set-text! inside-highlight "")
                   (d/set-text! after-highlight reference)
                   0)))
           <needle))
       (m/cp 0))]))

(defn autocomplete [path->page input]
  (let [container (d/element "div")
        <needle (->> (d/events input (.-KEYUP EventType))
                  (m/reductions (constantly nil))
                  (m/relieve {})
                  (m/sample (fn [_] (str/lower-case (.-value input)))))
        div->score (into {} (map (fn [e] (score e <needle))) path->page)]
    (.appendChild (.-parentNode input) container)
    (->> (vals div->score)
      (apply m/latest
        (fn [& scores]
          (->> scores
            (zipmap (keys div->score))
            (sort-by (comp - val))
            (eduction
              (filter (comp pos? val))
              (map key)))))
      (i/diff-by identity)
      (m/reduce d/mount-items container))))

((m/sp (m/? (m/? (m/join autocomplete get-articles (d/wait-for #(.getElementById js/document "search"))))))
 (.-log js/console) (.-error js/console))