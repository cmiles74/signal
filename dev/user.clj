(ns user
  (:require
   [taoensso.timbre :as timbre
    :refer (log  trace  debug  info  warn  error  fatal  report
                 logf tracef debugf infof warnf errorf fatalf reportf
                 spy get-env log-env)]
   [taoensso.timbre.profiling :as profiling
    :refer (pspy pspy* profile defnp p p*)]
   [clj-yaml.core :as yaml]
   [cmiles74.signal.cli :as cli]
   [cmiles74.signal.trust :as trust])
  (:use [slingshot.slingshot :only [try+ throw+]])
  (:import
   [java.security Security]
   [org.bouncycastle.jce.provider BouncyCastleProvider]
   [org.whispersystems.signalservice.api.push TrustStore]
   [org.whispersystems.signalservice.internal.configuration SignalServiceConfiguration]
   [org.whispersystems.signalservice.internal.configuration SignalServiceUrl]
   [org.whispersystems.signalservice.internal.configuration SignalCdnUrl]
   [org.whispersystems.signalservice.internal.configuration SignalContactDiscoveryUrl]
   [org.whispersystems.signalservice.api SignalServiceAccountManager]))

(defonce bcp-provider-index
  (Security/addProvider (new BouncyCastleProvider)))

(defn load-user-config
  []
  (try+
   (cli/load-config-file nil)
   (catch [:type :no-config] {:keys [message]}
     (warn message))))


(defn signal-config
  [config]
  (let [trust-store (trust/create)]
    (new SignalServiceConfiguration
         (into-array [(new SignalServiceUrl (get-in config [:signal :signal-url]) trust-store)])
         (into-array [(new SignalCdnUrl (get-in config [:signal :cdn-url]) trust-store)])
         (into-array [(new SignalContactDiscoveryUrl (get-in config [:signal :signal-url]) trust-store)]))))

(defn signal-manager
  [config]
    (let [service-config (signal-config config)]
    (new SignalServiceAccountManager
         service-config
         (get-in config [:account :username])
         (get-in config [:account :password])
         (get-in config [:signal :user-agent]))))


