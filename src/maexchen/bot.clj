(ns maexchen.bot
  (:require
   [clojure.core.async :as async :refer [>! <! alts! chan put! go]]
   [clojure.string :as string]))


(declare process-message)


(defn go-message
  "Main program loop"
  [in send-msg]
  (go
    (loop []
      (let [message (<! in)]
        (when-not (nil? message)
          (process-message send-msg message)
          (recur))))
    (println "go-message ended")))


(defn- token [parts]
  (nth parts 2))


(defn create-msg
  [& msgparts]
  (string/join ";" msgparts))


(defn register [botname]
  (create-msg "REGISTER" botname))


(defn round-starting [parts]
  (create-msg "JOIN" (second parts)))


(defn turn [parts]
  (create-msg "ROLL" (second parts)))


(defn announce [announcement token]
  (println "Announcment: " announcement)
  (create-msg "ANNOUNCE" announcement token))


(defn rolled [parts]
  (announce (second parts) (token parts)))


(defn process-message
  [send-msg message]
  (let [parts (string/split message #";")]
            (when-let [new-message (condp = (first parts)
                                     "ROUND STARTING"    (round-starting parts)
                                     "YOUR TURN"         (turn parts)
                                     "ROLLED"            (rolled parts)
                                     nil)]
              (send-msg new-message))))
