(ns maexchen.core
  (:require
   [clojure.core.async :as async :refer [>! <! alts! chan put! go]]
   [clojure.string :as string])
  (:import
   [java.io IOException]
   [java.net InetSocketAddress]
   [java.nio ByteBuffer CharBuffer]
   [java.nio.channels DatagramChannel SelectionKey Selector]
   [java.nio.charset Charset]
   [java.util ArrayList Collection]))

(def utf8 (Charset/forName "utf-8"))
(def context (atom {}))


(defn open-channels
  []
  (swap! context assoc
         :channel
         (let [channel (DatagramChannel/open)]
           (do
             (.socket (.bind channel nil))
             (.configureBlocking channel false)))
         :input  (chan)
         :output (chan)))


(defn close-channels
  []
  (do
    (let [{ :keys [input output channel]} @context]
      (async/close! input)
      (async/close! output)
      (.close (:channel @context)))
    (swap! context {})))


(defn read-message-from-channel
  [channel]
  (let [bytes (ByteBuffer/allocateDirect 1000)
        _ (.receive channel bytes)
        _ (.flip bytes)
        decoded (.decode utf8 bytes)]
    (.toString decoded)))


(defn go-listen
  [channel out]
  (go
    (let [selector (Selector/open)
          selection-key (.register channel selector (SelectionKey/OP_READ))]
      (loop []
        (if (< 0 (.select selector 1000))
          (do
            (-> selector
                (.selectedKeys)
                (.remove selection-key))
            (when (.isReadable selection-key)
              (when (>! out (read-message-from-channel channel))
                (recur))))
          (recur))))))


(defn send-message
  [host port channel message]
  (let [destination (InetSocketAddress. host port)]
    (.send channel (.encode utf8 message) destination)))


(defn create-msg
  [& msgparts]
  (string/join ";" msgparts))


(defn- token [parts]
  (nth parts 2))


(defn round-starting [parts]
  (create-msg "JOIN" (second parts)))


(defn turn [parts]
  (create-msg "ROLL" (second parts)))


(defn announce [announcement token]
  (println "Announcment: " announcement)
  (create-msg "ANNOUNCE" announcement token))


(defn rolled [parts]
  (announce (second parts) (token parts)))


(defn go-message
  [in send-msg]
  (go
    (loop []
      (let [message (<! in)]
        (when-not (nil? message)
          (let [parts (string/split message #";")]
            (when-let [new-message (condp = (first parts)
                                     "ROUND STARTING"    (round-starting parts)
                                     "YOUR TURN"         (turn parts)
                                     "ROLLED"            (rolled parts)
                                     nil)]
              (send-msg new-message)))
          (recur))))
    (println "go-message ended")))


(defn register [botname]
  (create-msg "REGISTER" botname))


(defn start-bot
  [botname host port]
  (do
    (open-channels)
    (let [send-msg (partial send-message host port (:channel @context))]
      (send-msg (register botname))
      (go-message (:input @context) send-msg)
      (go-listen (:channel @context) (:input @context)))))


(defn stop-bot
  []
  (close-channels))
