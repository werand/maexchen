(ns maexchen.core
  (:require
   [clojure.core.async :as async :refer [>! <! alts! chan put! go]]
   [maexchen.udp :as udp]))

(def context (atom {}))

(defprotocol MiaBot
  (register-bot
    [this]
    "Create the join message.")

  (process-mia-message
    [this message]
    "Process the mia message from the server, results in an optional response."))

(defn- go-process-mia-messages
  "Main program loop"
  [bot in send-msg]
  (go
    (loop []
      (let [message (<! in)]
        (when-not (nil? message)
          (let [response (process-mia-message bot message)]
            (when-not (nil? response)
              (send-msg response)))
          (recur))))))

(defn start-bot
  [bot host port]
  (do
    (udp/open-channels context)
    (let [{:keys [channel input]} @context
          send-msg (partial udp/send-message host port channel)]
      (send-msg (register-bot bot))
      (go-process-mia-messages bot input send-msg)
      (udp/go-listen channel input))))

(defn stop-bot
  []
  (udp/close-channels context))
