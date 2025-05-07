(ns release
  (:require [cljs.build.api :as cljs]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [glow.html :as g.html]
            [glow.parse :as g.parse]
            [hiccup.util :as util]
            [hiccup2.core :as h]
            [hiccup.page :as h.page]
            [nextjournal.markdown :as md]
            [nextjournal.markdown.transform :as md.transform]
            [nextjournal.markdown.parser :as md.parser])
  (:import (java.io File)))

(defn clean! [^File f]
  (when (.isDirectory f)
    (run! clean! (.listFiles f)))
  (.delete f))

(defn touch! [root path]
  (doto (io/file root path)
    (-> .getParentFile .mkdirs)))

(defn read! [root path]
  (slurp (io/file root path)))

(def app "app.js")

(def css
  [#_ {:name "github-markdown" :type :remote :url  "https://raw.githubusercontent.com/sindresorhus/github-markdown-css/gh-pages/github-markdown-light.css"}
   {:name "style"           :type :local  :path "style.css"}])

(defn write-js! [release {:keys [dev?]}]
  (cljs/build
    (merge
      {:main          "client.main"
       :output-to     (str (io/file release app))}
      (if dev?
        {:asset-path "/js"
         :output-dir (str (io/file release "js"))}
        {:optimizations :advanced}))))

(defn write-css! [release {:keys [assets]}]
  (run! (fn [{:keys [name type] :as item}]
          (spit (touch! release (str name ".css"))
            (case type
              :remote (slurp (:url item))
              :local (read! assets (:path item))))) css))

(def hiccup-renderers
  (assoc md.transform/default-hiccup-renderers
    :code (fn [_ctx {:keys [language content]}]
            (assert (= language "clojure") "Unsupported language.")
            (into [:pre] (map (comp g.html/hiccup-transform g.parse/parse :text)) content))
    :plain (fn [ctx node]
             (or (:text node)
               (keep (partial md.transform/->hiccup
                       (assoc ctx ::md.transform/parent node))
                 (:content node))))
    :internal-link (fn [_ctx {:keys [text attrs]}]
                     [:a.internal {:href (str (:href attrs) ".html")} text])))

(def md-doc
  (update md.parser/empty-doc :text-tokenizers conj
    md.parser/internal-link-tokenizer))

(defn md->hiccup [assets path]
  (md.transform/->hiccup hiccup-renderers
    (md/parse md-doc (read! assets (str path ".md")))))

(defn html5 [& hiccup]
  (h/html (h.page/doctype :html5)
    (into [:html {:lang "en"}] hiccup)))

(defn page-head [dev?]
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
   (map (fn [{:keys [name]}]
          [:link {:type "text/css"
                  :href (util/to-uri (str "/" name ".css"))
                  :rel "stylesheet"}]) css)
   [:script {:type  "text/javascript"
             :src   (util/to-uri (str "/" app))
             :async (not dev?)}]])

(def search-input
  [:input#search {:type "search"}])

(def template
  {:home      (fn [{:keys [assets dev?]}]
                (html5
                  (page-head dev?)
                  [:body
                   [:header [:a {:href "/about.html"} "About"] search-input [:hr]]
                   [:main
                    [:article {:class "markdown-body"}
                     (next (md->hiccup assets "topics/home"))]]]))
   :not-found (fn [{}]
                (html5
                  [:head]
                  [:body
                   [:h1 "Not found"]]))
   :topic     (fn [{:keys [assets path dev?]}]
                (html5
                  (page-head dev?)
                  [:body
                   [:header [:a {:href "/"} "Home"] search-input [:hr]]
                   [:main
                    [:article {:class "markdown-body"}
                     (next (md->hiccup assets (str "topics/" path)))]
                    [:footer (md->hiccup assets "fragments/footer")]]]))})

(defn read-pages! [assets]
  (edn/read-string (read! assets "pages.edn")))

(defn write-pages! [release opts]
  (reduce-kv (fn [_ path page]
               (spit (touch! release (str path ".html"))
                 ((template (:template page)) (assoc opts :path path :page page))))
    nil (read-pages! (:assets opts))))

(defn build [release opts]
  (doto release
    (clean!)
    (write-js! opts)
    (write-css! opts)
    (write-pages! opts)))

(defn -main [& _]
  (build (io/file "release") {:assets (io/file "assets")}))