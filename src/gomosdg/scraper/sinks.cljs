(ns gomosdg.scraper.sinks
  (:require [cljs.core.async :refer [<! go-loop]]
            [gomosdg.scraper.pubsub :as pubsub]))

(defprotocol Sink
  (listen [this ch]
    "Listens for incoming messages from the channel"))


(deftype SQS [data]
  Sink
  (listen [_ ch]
    (go-loop []
      (let [entity (<! ch)]
        (pubsub/push! (:queue-name data)
                      entity
                      :encoder (:encoder data)))
      (recur))))


(defmulti get-sink :type)


(defmethod get-sink :sqs
  [{:keys [queue-name encoder]}] 
  (SQS. {:queue-name queue-name
         :encoder    encoder}))