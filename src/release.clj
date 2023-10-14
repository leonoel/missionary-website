(ns release
  (:require [cljs.build.api :as cljs]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [garden.core :as garden]
            [glow.html :as g.html]
            [glow.parse :as g.parse]
            [hiccup.util :as util]
            [hiccup2.core :as h]
            [hiccup.page :as h.page]
            [nextjournal.markdown :as md]
            [nextjournal.markdown.transform :as md.transform]
            [nextjournal.markdown.parser :as md.parser]
            [thi.ng.color.core :as c])
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
(def style "style.css")

(defn write-js! [release {:keys [dev?]}]
  (cljs/build
    (merge
      {:main          "client.main"
       :output-to     (str (io/file release app))}
      (if dev?
        {:asset-path "/js"
         :output-dir (str (io/file release "js"))}
        {:optimizations :advanced}))))

(defn gradient [{[w h] :size [x y] :position color :color}]
  (str "radial-gradient(ellipse " w " " h " at " x " " y ", " color ", transparent)"))

(defn scale-alpha [color s]
  (let [c (c/css color)]
    @(c/as-css (c/rgba (c/red c) (c/green c) (c/blue c) (* s (c/alpha c))))))

(defn write-css! [release {:keys [assets]}]
  (let [{:keys [text-color max-width text-font-family code-font-family
                body-background body-gradients link-color headings code-background pre-padding
                code-special code-literal code-comment code-string code-symbol
                footer-font-size block-margin]}
        (edn/read-string (read! assets "style.edn"))]
    (spit (touch! release style)
      (garden/css
        ;; reset http://meyerweb.com/eric/tools/css/reset/
        [:html :body :div :span :applet :object :iframe :h1 :h2 :h3 :h4 :h5 :h6 :p :blockquote :pre :a :abbr :acronym
         :address :big :cite :code :del :dfn :em :img :ins :kbd :q :s :samp :small :strike :strong :sub :sup :tt :var
         :b :u :i :center :dl :dt :dd :ol :ul :li :fieldset :form :label :legend :table :caption :tbody :tfoot :thead
         :tr :th :td :article :aside :canvas :details :embed :figure :figcaption :footer :header :hgroup :menu :nav
         :output :ruby :section :summary :time :mark :audio :video
         {:border 0
          :margin 0
          :padding 0
          :font 'inherit}]
        [:article :aside :details :figcaption :figure :footer :header :hgroup :menu :nav :section
         {:display 'block}]
        [:ol :ul {:list-style 'none}]
        [:blockquote :q {:quotes 'none}
         [:&:before :&:after {:content "''"}]
         [:&:before :&:after {:content 'none}]]
        [:table {:border-collapse 'collapse
                 :border-spacing 0}]

        [:html :body {:height "100%"}]
        [:header :article :footer
         {:width "100%"
          :max-width max-width
          :margin 'auto}]
        [:body {:color                 text-color
                :hyphens               'auto
                :text-align            'justify
                :font-family           text-font-family
                :display               'flex
                :flex-flow             'column
                :background-attachment 'fixed
                :background-color      body-background
                :background-image      (str/join "," (map gradient body-gradients))}]
        [:main {:overflow 'auto
                :flex      1
                :display   'flex
                :flex-flow 'column}]
        [:article {:flex 1}
         (into []
           (map-indexed
             (fn [i {:keys [margin dimming font-size]}]
               [(keyword (str "h" (inc i)))
                {:color (scale-alpha text-color dimming)
                 :font-size font-size
                 :margin (str margin " 0")}
                [:a {:color (scale-alpha link-color dimming)}]]))
           headings)
         [:p :pre :ul :table {:margin (str block-margin " 0")}]
         [:ul {:padding-left "1rem"}
          [:li {:display 'list-item
                :list-style-type 'disc}]]]
        [:footer
         [:ul {:display 'flex
               :justify-content 'space-between}
          [:li {:font-size footer-font-size}]]]
        [:a {:color link-color
             :text-decoration 'none}
         [:&:hover {:text-decoration 'underline}]]
        [:pre :code {:font-family code-font-family
                     :background-color code-background}]
        [:pre
         {:overflow-x 'auto
          :padding pre-padding}
         [:.exception
          :.repeat
          :.conditional
          :.core-fn
          :.definition
          :.special-form
          :.macro
          {:color code-special}]
         [:.number
          :.boolean
          :.nil
          :.keyword
          {:color code-literal}]
         [:.comment
          {:color code-comment}]
         [:.string
          :.character
          :.regex
          {:color code-string}]
         [:.variable
          :.reader-char
          :.s-exp
          :.symbol
          {:color code-symbol}]]))))

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
   [:link {:rel "preconnect" :href "https://fonts.googleapis.com"}]
   [:link {:rel "preconnect" :href "https://fonts.gstatic.com" :crossorigin "true"}]
   [:link {:rel "stylesheet" :href "https://fonts.googleapis.com/css2?family=Lexend:wght@300&family=Source+Code+Pro&display=swap"}]
   [:link {:type "text/css"
           :href (util/to-uri (str "/" style))
           :rel "stylesheet"}]
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
                   [:header [:a {:href "/about.html"} "About"] search-input]
                   [:main
                    [:h1 "Missionary"]
                    [:section (md->hiccup assets "fragments/one-sentence")]
                    [:section (md->hiccup assets "fragments/one-code-block")]
                    [:section (md->hiccup assets "fragments/one-paragraph")]
                    [:section (md->hiccup assets "fragments/documentation")]
                    [:section (md->hiccup assets "fragments/source")]
                    [:section (md->hiccup assets "fragments/support")]
                    [:section (md->hiccup assets "fragments/quote")]]]))
   :not-found (fn [{}]
                (html5
                  [:head]
                  [:body
                   [:h1 "Not found"]]))
   :topic     (fn [{:keys [assets path dev?]}]
                (html5
                  (page-head dev?)
                  [:body
                   [:header [:a {:href "/"} "Home"] search-input]
                   [:main
                    [:article
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