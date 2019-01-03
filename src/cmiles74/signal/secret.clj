(ns cmiles74.signal.secret
  (:require
   [taoensso.timbre :as timbre
    :refer (log  trace  debug  info  warn  error  fatal  report
                 logf tracef debugf infof warnf errorf fatalf reportf
                 spy get-env log-env)]
   [taoensso.timbre.profiling :as profiling
    :refer (pspy pspy* profile defnp p p*)]
   [clj-yaml.core :as yaml]
   [cmiles74.signal.signal :as signal])
  (:use [slingshot.slingshot :only [try+ throw+]])
  (:import
   [java.security SecureRandom]
   [org.whispersystems.libsignal IdentityKeyPair]
   [org.whispersystems.libsignal.util KeyHelper]
   [org.whispersystems.signalservice.internal.util Base64]))

(defn new-secure-random
  "Returns a new SecureRandom instance."
  []
  (try+
   (SecureRandom/getInstance "NativePRNG")
   (catch Exception exception
     (warn (str "Error while initializing SecureRandom:"
                (.getMessage exception)))
     (SecureRandom.))))

(def secure-random (new-secure-random))

(defn secret-bytes
  "Returns a byte array of random data of the requested size."
  [size]
  (let [byte-data (byte-array size)]
    (.nextBytes secure-random byte-data)
    byte-data))

(defn secret-string
  "Returns a Base64 encoded string derived from a byte array of random data of the
  requested size."
  [size]
  (Base64/encodeBytes (secret-bytes size)))

(defn group-id
  "Returns a new Signal Group ID."
  [] (secret-bytes 16))

(defn password
  "Returns a new Signal password."
  [] (secret-string 18))

(defn profile-key
  "Returns a new Signal profile key."
  [] (secret-bytes 32))

(defn signaling-key
  "Returns a new Signal signaling key."
  [] (secret-string 52))

(defn create-account
  ([phone-number] (create-account "+1" phone-number))
  ([country-code phone-number]
   {:account
    {:username (str country-code phone-number)
     :password (password)
     :signaling-key (signaling-key)
     :registration-id (KeyHelper/generateRegistrationId false)
     :identity-keypair (KeyHelper/generateIdentityKeyPair)}}))

(defn transform-account-for-storage
  [config-map]
  (assoc config-map :account
         (merge (:account config-map)
                {:identity-keypair (Base64/encodeBytes
                                    (.serialize (get-in config-map [:account :identity-keypair])))})))

(defn transform-account-from-storage
  [config-map]
  (assoc config-map :account
         (merge (:account config-map)
                {:identity-keypair (IdentityKeyPair.
                                    (Base64/decode (get-in config-map [:account :identity-keypair])))})))
