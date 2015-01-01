(ns maexchen.core
  (:require
   [clojure.core.async :as async :refer [>! <! alts! chan put! go]]
   [maexchen.udp :as udp]
   [maexchen.bot :as bot]))


(def context (atom {}))


(defn start-bot
  [botname host port]
  (do
    (udp/open-channels context)
    (let [{:keys [channel input]} @context
          send-msg (partial udp/send-message host port channel)]
      (send-msg (bot/register botname))
      (bot/go-message input send-msg)
      (udp/go-listen channel input))))


(defn stop-bot
  []
  (udp/close-channels context))
