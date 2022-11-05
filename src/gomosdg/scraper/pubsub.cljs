(ns gomosdg.scraper.pubsub
  (:require [cljs-aws.sns :as sns]
            [cljs-aws.sqs :as sqs]
            [cljs.core.async :refer [go <!]]
            [cljs-aws.base.config :as config]))

(defn throw-or-print [{:keys [error] :as data}]
  (if error 
    (throw (ex-info error nil)) 
    (println data))
  data)

(defn- create-topic-or-get-arn! [topic-name]
  (go (-> (<! (sns/create-topic {:name topic-name}))
          (throw-or-print)
          :topic-arn)))


(defn get-encoder [encoder]
  (case encoder
    :json
    #(js/JSON.stringify (clj->js %))

    :edn
    pr-str))


(defn publish! [topic-name message & {:keys [encoder]}]
  (go (let [encode (or (get-encoder encoder) pr-str)
            topic-arn (<! (create-topic-or-get-arn! topic-name))]
        (throw-or-print (<! (sns/publish {:topic-arn          topic-arn
                                          :message            (encode message)}))))))


(defn push! [queue-name message & {:keys [encoder]}]
  (go (let [encode (or (get-encoder encoder) pr-str)
            queue-url (-> (<! (sqs/create-queue {:queue-name queue-name}))
                          (throw-or-print)
                          :queue-url)]
        (throw-or-print (<! (sqs/send-message {:queue-url queue-url
                                               :message-body (encode message)}))))))

(config/set-region! "us-east-1")

(comment
  (go (create-topic-or-get-arn "Hello"))

  (publish! "hello" {:something "Beautiful"})
  (let [topic-arn (create-topic-or-get-arn "my-topic")]
    (throw-or-print (<! (sns/publish {:topic-arn          topic-arn
                                      :subject            "test"
                                      :message            (str "Todays is " (js/Date.))
                                      :message-attributes {"attr" {:data-type    "string"
                                                                   :string-value "value"}}})))))

