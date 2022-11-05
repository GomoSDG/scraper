(ns gomosdg.scraper.extractors
  (:require  ["cheerio" :as cheerio]
             [clojure.string :as str]
             [medley.core :as m]))

(defmulti extract (fn [[type :as selector] _] type))

(defmethod extract :name-value-list
  [[_ root key-selector value-selector] dom]
  (let [$ (.load cheerio dom)]

    (as-> ($ root) x
      (.map x (fn [i e]
                (assoc {}
                       :name (extract key-selector (.html (.load ^js cheerio e)))
                       :value (extract value-selector (.html (.load ^js cheerio e))))))
      (array-seq x)
      (js->clj x))))

(defmethod extract :name-value-map
  [[_ root key-selector value-selector coerce] dom]
  (let [$ (.load cheerio dom)]

    (->> (extract [:name-value-list root key-selector value-selector] dom)
         (filter :name)
         (map (fn [{:keys [name value]}]
                (assoc {}
                       name
                       value)))
         (apply merge)
         (m/map-keys #(-> (str/lower-case %)
                          (str/replace " " "-")
                          (keyword))))))

(defmethod extract :attr
  [[_ attr-name tag] dom]
  (let [$ (.load cheerio dom)
        tag-el ($ ^js tag)]

    (.attr ^js tag-el attr-name)))

(defmethod extract :href-after-last
  [[_ tag re] dom]
  (let [href (extract [:attr "href" tag] dom)]
    (last (str/split href re))))

(defmethod extract :or
  [[_ selector1 selector2] dom]
  (if (empty? (extract selector1 dom))
    (extract selector2 dom)
    (extract selector1 dom)))

(defmethod extract :text
  [[_ tag regex] dom]
  (let [$ (.load cheerio dom)
        re (if (nil? regex)
             #".*"
             regex)]

    (->> ($ tag)
         (.text)
         (str/trim)
         (re-seq re)
         (apply str))))

(defmethod extract :identity
  [[_ val] _]
  val)

(defmethod extract :number
  [[_ tag] dom]
  (-> (extract [:text tag #"\d+"] dom)
      (js/parseInt)))

(defmethod extract :float
  [[_ tag] dom]
  (let [$ (.load cheerio dom)
        text (-> ($ tag)
                 (.text)
                 (str/trim))]
    (as-> text x
      (re-seq #"(\d+(?:\.\d+)?)" x)
      (first x)
      (second x)
      (js/parseFloat x))))

(defmethod extract :flag
  [[_ tag] dom]
  (let [$ (.load cheerio dom)]

    (-> ($ tag)
        (.-length)
        (> 0))))

(defmethod extract :re-group
  [[_ tag re group] dom]
  (as-> (extract [:text tag] dom) $
    (re-find re $)
    (nth $ group)))
