(ns client.main
  (:refer-clojure :exclude [time])
  (:require [client.dom :as d]
            [client.net :as n]
            [clojure.string :as str]
            [missionary.core :as m]
            [goog.dom.classlist :as gdc])
  (:import (goog.dom TagName NodeType)
           (goog.events EventType))
  (:require-macros [client.main :refer [topics]]))

(def get-articles
  (let [t (topics)
        hrefs (mapv (fn [path] (str "/" path ".html")) (keys t))]
    (->> (vals t)
      (map (fn [href page]
             (m/sp (-> href
                     (n/fetch "document")
                     (m/?)
                     (.getElementsByTagName "article")
                     (.item 0)
                     (->> (assoc page :article))))) hrefs)
      (apply m/join (fn [& pages] (zipmap hrefs pages)))
      (m/memo))))

(defn score [[href {:keys [article]}] <needle]
  (let [title (.item (.getElementsByTagName article "h1") 0)
        title-child (.item (.-childNodes title) 0)
        suggestion (d/element "div")
        highlight (d/element "span")
        before-highlight (d/text "")
        inside-highlight (d/text "")
        after-highlight (d/text "")]
    (gdc/add highlight "highlight")
    (gdc/add suggestion "suggestion")
    (set! (.-onclick suggestion)
      #(set! (.-href (.-location js/window)) href))
    (.appendChild highlight inside-highlight)
    (.appendChild suggestion title-child)
    [suggestion
     (if-some [reference (when (and (= (.-nodeType title-child) (.-ELEMENT NodeType))
                                 (= (.-CODE TagName) (.-tagName title-child)))
                           (d/get-text! (.item (.-childNodes title-child) 0)))]
       (let [offset (inc (or (str/index-of reference "/") (.lastIndexOf reference ".")))
             suffix (subs (str/lower-case reference) offset)]
         (.replaceChildren title-child before-highlight highlight after-highlight)
         (m/latest
           (fn [needle]
             (if-some [index (when-not (= "" needle) (str/index-of suffix needle))]
               (let [start (+ offset index)
                     end (+ start (count needle))]
                 (d/set-text! before-highlight (subs reference 0 start))
                 (d/set-text! inside-highlight (subs reference start end))
                 (d/set-text! after-highlight (subs reference end))
                 (if (= suffix needle) 3 (if (zero? index) 2 1)))
               (do (d/set-text! before-highlight "")
                   (d/set-text! inside-highlight "")
                   (d/set-text! after-highlight reference)
                   0)))
           <needle))
       (m/cp 0))]))

(defn replace-children [parent children]
  (.apply (.-replaceChildren (.-prototype js/Element))
    parent (into-array children)) parent)

(defn autocomplete [path->page input]
  (let [container (d/element "div")
        <needle (->> (d/events input (.-KEYUP EventType))
                  (m/reductions (constantly nil))
                  (m/relieve {})
                  (m/sample (fn [_] (str/lower-case (.-value input)))))
        div->score (into {} (map (fn [e] (score e <needle))) path->page)]
    (gdc/add container "autocomplete")
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
      (m/reduce replace-children container))))

((m/sp (m/? (m/? (m/join autocomplete get-articles (d/wait-for #(.getElementById js/document "search"))))))
 (.-log js/console) (.-error js/console))