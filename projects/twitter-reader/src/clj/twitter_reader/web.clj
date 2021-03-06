(ns twitter-reader.web
  "Entry point of the application. Exposes a websocket endpoint."
  (:require [twitter-reader.tweet-buffer :as tp]
            [twitter-reader.twitter-listener :as tl]
            [twitter-reader.conn-repository :as cr]
            [org.httpkit.server :as kit]
            [clojure.string :as str]
            [clojure.data.json :as json]))

(defn- ->ui-message
  "Creates a UI message from the latest tweet and the updated statistics"
  [stats tweet]
  (json/write-str
    {:stats (into (sorted-map) stats)
     :tweet tweet}))

(defn- handle-tweet!
  "Updates the model when new tweet arrives for the given connection and sends update to the UI"
  [tweet ws-channel]
  ;; only when connection isn't closed
  (when-let [conn (cr/get-conn ws-channel)]
    (let [buffer (tp/update-buffer {:text (:text tweet)
                                    :buffer (:tweets conn)
                                    :search-words (:search-words conn)
                                    :now (System/currentTimeMillis)})
          msg (->ui-message (:word-frequencies buffer) tweet)]
      (cr/update-tweets! ws-channel (:tweets buffer))
      (kit/send! ws-channel msg))))

(defn- ws-handler
  "Websocket request handler"
  [ts-factory request]
  (kit/with-channel request channel
    (println channel "connected")
    (let [tweet-listener (tl/create-tweet-listener ts-factory (fn [tweet]
                                                                (handle-tweet! tweet channel)))
          on-close-f (fn [status]
                       (println "channel closed: " status)
                       (cr/remove-conn! channel)
                       (tl/stop-listener! tweet-listener))
          on-receive-f (fn [data]
                         (println "received: " channel data)
                         (let [words (set (str/split data #","))]
                           (cr/reset-conn! channel words)
                           (tl/start-listener! tweet-listener words)))]
      (kit/on-close channel on-close-f)
      (kit/on-receive channel on-receive-f))))

(defn -main
  "The main function"
  [& args]
  (let [twitter-stream-factory (tl/create-twitter-stream-factory)
        handler (fn [req] (ws-handler twitter-stream-factory req))]
    (kit/run-server handler {:port 8080})
    (println "Started")))




