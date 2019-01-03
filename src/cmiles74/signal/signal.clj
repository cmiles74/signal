(ns cmiles74.signal.signal
  (:require
   [taoensso.timbre :as timbre
    :refer (log  trace  debug  info  warn  error  fatal  report
                 logf tracef debugf infof warnf errorf fatalf reportf
                 spy get-env log-env)]
   [taoensso.timbre.profiling :as profiling
    :refer (pspy pspy* profile defnp p p*)]
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

;; signal configuration
(defonce signal-config
  {:signal
   {:user-agent "cmiles74-signal"
    :signal-url "https://textsecure-service.whispersystems.org"
    :cdn-url "https://cdn.signal.org"}})

(defonce bcp-provider-index
  (Security/addProvider (new BouncyCastleProvider)))

(defn config
  "Returns a new SignalServiceConfiguration instance that is configured with the
  provided configuration map."
  [config]
  (let [trust-store (trust/create)]
    (new SignalServiceConfiguration
         (into-array [(new SignalServiceUrl (get-in signal-config [:signal :signal-url]) trust-store)])
         (into-array [(new SignalCdnUrl (get-in signal-config [:signal :cdn-url]) trust-store)])
         (into-array [(new SignalContactDiscoveryUrl (get-in signal-config [:signal :signal-url]) trust-store)]))))

(defn manager
  "Returns a new SignalServiceAccountManager instance that is configured with the
  provided configuration map."
  [config-in]
  (let [service-config (config config-in)]
    (new SignalServiceAccountManager
         service-config
         (get-in config-in [:account :username])
         (get-in config-in [:account :password])
         (get-in signal-config [:signal :user-agent]))))

(defn register-sms
  [manager-in]
  (try+
   (.requestSmsVerificationCode manager-in)
   true
   (catch Exception exception
     (throw+ {:type :register-fail :message (.getMessage exception)}))))

(defn register-voice
  [manager-in]
  (try+
   (.requestVoiceVerificationCode manager-in)
   true
   (catch Exception exception
     (throw+ {:type :register-fail :message (.getMessage exception)}))))


