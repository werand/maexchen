(ns maexchen.udp
  (:require
   [clojure.core.async :as async :refer [>! <! alts! chan put! go]])
  (:import
   [java.io IOException]
   [java.net InetSocketAddress]
   [java.nio ByteBuffer CharBuffer]
   [java.nio.channels DatagramChannel SelectionKey Selector]
   [java.nio.charset Charset]
   [java.util ArrayList Collection]))

(def utf8 (Charset/forName "utf-8"))

(defn open-channels
  [context]
  (swap! context assoc
         :channel
         (let [channel (DatagramChannel/open)]
           (do
             (.socket (.bind channel nil))
             (.configureBlocking channel false)))
         :input  (chan)
         :output (chan)))

(defn close-channels
  [context]
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
  "Reads from udp channel and writes the message to the core.async channel out.
   Stops reading when the core.async channel is closed."
  [channel out]
  (go
    (let [selector (Selector/open)
          selection-key (.register channel selector (SelectionKey/OP_READ))]
      (loop []
        (if (> (.select selector 1000) 0)
          (do
            (-> selector
                (.selectedKeys)
                (.remove selection-key))
            (when
                (and
                 (.isReadable selection-key)
                 (>! out (read-message-from-channel channel)))
              (recur)))
          (recur))))))

(defn send-message
  [host port channel message]
  (let [destination (InetSocketAddress. host port)]
    (.send channel (.encode utf8 message) destination)))
