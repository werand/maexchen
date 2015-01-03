;;
;; The protocol is described here: https://github.com/janernsting/maexchen/blob/master/protokoll.markdown
;;
;;  The order of the rolls 11 to 66 is reversed.
(ns maexchen.bot
  (:require
   [clojure.core.async :as async :refer [>! <! alts! chan put! go]]
   [maexchen.core :as mia :refer [MiaBot]]
   [clojure.string :as string]))

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
  (create-msg "ANNOUNCE" announcement token))

(defn rolled [parts]
  (announce (second parts) (token parts)))

(defrecord SimpleBot [botname]
  mia/MiaBot

  (register-bot
    [bot]
    (register botname))

  (process-mia-message
    [bot message]
    (let [parts (string/split message #";")]
      (condp = (first parts)
        "ROUND STARTING"    (round-starting parts)
        "YOUR TURN"         (turn parts)
        "ROLLED"            (rolled parts)
        nil))))


;; Start the bot as follows (from core namespace):
;; (start-bot (maexchen.bot.SimpleBot. "clojure") "localhost" 9000)
