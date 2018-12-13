(ns cmiles74.signal.trust
  (:require
   [clojure.java.io :as io])
  (:import
   [org.whispersystems.signalservice.api.push TrustStore]))

(defn create
  []
  (reify TrustStore
    (getKeyStoreInputStream [this] (io/input-stream (io/resource "whisper.store")))
    (getKeyStorePassword [this] "whisper")))
