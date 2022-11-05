(ns gomosdg.scraper.pipelines
  (:require [cljs.core.async :refer [go]]
            ["cheerio" :as cheerio]
            [gomosdg.scraper.extractors :as e]))

(defn get-entities-specs
  [{:keys [dom selector site-id date url]}]
  (let [root       (:root selector)
        attributes (:attributes selector)
        $          (cheerio dom)
        entities   (.find ^js $ root)] 

    (->> (.map entities (fn [i el]
                          {:dom (-> (.load ^js cheerio ^js el)
                                    (.html))
                           :site-id site-id
                           :url url
                           :attributes attributes
                           :date date
                           :type (:type selector)}))
         (array-seq))))

(defn entity-spec->attribute-specs [{:keys [dom attributes]
                                     :as payload}]
  (merge {:attribute-specs (map #(assoc {}
                                        :dom dom
                                        :name (:name %)
                                        :selector (:selector %))
                                attributes)}
         payload))

(defn parse-attribute [{:keys [dom name selector] :as attribute-specs}]
  (assoc {} name (e/extract selector dom)))

(defn parse-attributes [{:keys [attribute-specs]
                         :as payload}]
  (merge {:parsed-attributes (map parse-attribute attribute-specs)}
         payload))

(defn build-entity [{:keys [parsed-attributes site-id type date url]
                     :as payload}]
  (let [entity (apply merge parsed-attributes)]
    (merge {:entity (assoc entity
                           :site-id site-id
                           :entity-type type
                           :url url
                           :date-scraped date)}
           payload)))

(defn decorate-with-id [{:keys [entity]
                         :as payload}]
  (merge {:id (:id entity)}
         payload))

(defn decorate-with-type [{:keys [entity]
                           :as payload}]
  (merge {:type (:type entity)}
         payload))

(defn store-html [{:keys [dom type id site-id]
                   :as payload}]
  (let [fs (js/require "fs")
        dir (str "./scrapes/" (name site-id) "/" (name type) "/" id)
        file (str "/" (-> (random-uuid) (.toString)) ".html")
        print-error #(when %
                       (println "Error saving: " file %))
        write-file #(do
                      (.writeFile fs
                                  (str dir file)
                                  dom
                                  print-error)
                      (println "Wrote file: " (str dir file)))
        write-file-or-print-error #(if %
                                     (write-file)
                                     (print-error %))
        make-dir #(.mkdir fs
                          dir
                          (js-obj "recursive" true)
                          write-file-or-print-error)]

    (go (.access fs dir #(if %
                           (make-dir)
                           (write-file))))
    payload))
