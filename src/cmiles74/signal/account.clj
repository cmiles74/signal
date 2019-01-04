(ns cmiles74.signal.account
  (:gen-class)
  (:require
   [taoensso.timbre :as timbre
    :refer (log  trace  debug  info  warn  error  fatal  report
                 logf tracef debugf infof warnf errorf fatalf reportf
                 spy get-env log-env)]
   [taoensso.timbre.profiling :as profiling
    :refer (pspy pspy* profile defnp p p*)]
   [cmiles74.common.config :as config]
   [cmiles74.signal.secret :as secret])
  (:use [slingshot.slingshot :only [try+ throw+]])
  (:import
   [org.whispersystems.libsignal IdentityKeyPair]
   [org.whispersystems.libsignal.util KeyHelper]
   [org.whispersystems.signalservice.internal.util Base64]))

;; default name of the configuration file
(def default-config-file ".cmiles74-signal.yml")

;;
;; Creating Account
;;

(defn create
  ([phone-number] (create "+1" phone-number))
  ([country-code phone-number]
   {:account
    {:username (str country-code phone-number)
     :password (secret/password)
     :registered false
     :signaling-key (secret/signaling-key)
     :registration-id (KeyHelper/generateRegistrationId false)
     :identity-keypair (KeyHelper/generateIdentityKeyPair)
     :profile-key (secret/secret-bytes 32)}}))

;;
;; Loading and Storing Account
;;

(defn encode-storage
  "Transforms a map of account data for storage to disk."
  [config-map]
  (assoc config-map :account
         (merge (:account config-map)
                 {:profile-key (Base64/encodeBytes
                                (get-in config-map [:account :profile-key]))
                 :identity-keypair (Base64/encodeBytes
                                    (.serialize (get-in config-map [:account :identity-keypair])))})))

(defn decode-storage
  "Transforms a map of account data from disk into a format we can use."
  [config-map]
  (assoc config-map :account
         (merge (:account config-map)
                {:profile-key (Base64/decode
                               (get-in config-map [:account :profile-key]))
                 :identity-keypair (IdentityKeyPair.
                                    (Base64/decode (get-in config-map [:account :identity-keypair])))})))

(defn store-file
  "Stores the provided map of account data to to the provided path with the
  specified file name."
  ([config-map] (store-file (System/getProperty "user.home") default-config-file config-map))
  ([path-in filename config-map]
   (let [config-map-persist (encode-storage config-map)]
     (try+
      (config/save-config-file path-in
                               filename
                               config-map-persist)
      true
      (catch Exception exception
        (throw+ {:type :config-save-fail :message (.getMessage exception)}))))))

(defn load-file
  "Loads the configuration file from the provided path, returns a map with the
  account values."
  [path-in]
  (let [config (config/load-config-file path-in
                                        default-config-file
                                        {})]
    (if (nil? (get-in config [:account :username]))
      (throw+ {:type :no-config :message "No configuration file found"}))
    (decode-storage config)))

(defn store
  "Saves the provided account data to the default configuration file."
  [account]
  (store-file account))

(defn load
  "Loads the customer's account from the default configuration file."
  []
  (try+
   (load-file nil)
   (catch [:type :no-config] {:keys [message]}
     (warn message))))
