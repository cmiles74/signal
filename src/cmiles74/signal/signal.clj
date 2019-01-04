(ns cmiles74.signal.signal
  (:require
   [taoensso.timbre :as timbre
    :refer (log  trace  debug  info  warn  error  fatal  report
                 logf tracef debugf infof warnf errorf fatalf reportf
                 spy get-env log-env)]
   [taoensso.timbre.profiling :as profiling
    :refer (pspy pspy* profile defnp p p*)]
   [clojure.string :as string]
   [cmiles74.signal.trust :as trust])
  (:use [slingshot.slingshot :only [try+ throw+]])
  (:import
   [java.security Security]
   [org.bouncycastle.jce.provider BouncyCastleProvider]
   [org.whispersystems.signalservice.api.push TrustStore]
   [org.whispersystems.signalservice.api.crypto UnidentifiedAccess]
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
  "Returns a new SignalServiceConfiguration instance."
  []
  (let [trust-store (trust/create)]
    (new SignalServiceConfiguration
         (into-array [(new SignalServiceUrl (get-in signal-config [:signal :signal-url]) trust-store)])
         (into-array [(new SignalCdnUrl (get-in signal-config [:signal :cdn-url]) trust-store)])
         (into-array [(new SignalContactDiscoveryUrl (get-in signal-config [:signal :signal-url]) trust-store)]))))

(defn start-session
  "Starts a new signal session with the provided account map and returns a new map
  that include the account and the session data. This map will be used for all
  further interaction with the Signal service methods."
  [account]
  (let [service-config (config)]
    (try+
     (assoc account :session
            {:service-config service-config
             :manager (new SignalServiceAccountManager
                           service-config
                           (get-in account [:account :username])
                           (get-in account [:account :password])
                           (get-in signal-config [:signal :user-agent]))})
     (catch Exception exception
       (throw+ {:type :session-fail :message (.getMessage exception)})))))

(defn register-sms
  "Registers the account and requests a verification code via SMS message.
  Requires a valid session map as provided by `start-session`."
  [session]
  (try+
   (.requestSmsVerificationCode (get-in session [:session :manager]))
   true
   (catch Exception exception
     (throw+ {:type :register-fail :message (.getMessage exception)}))))

(defn register-voice
  "Registers the account and requests a verification code via voice message.
  Requires a valid session map as provided by `start-session`."
  [session]
  (try+
   (.requestVoiceVerificationCode (get-in session [:session :manager]))
   true
   (catch Exception exception
     (throw+ {:type :register-fail :message (.getMessage exception)}))))

(defn verify-code
  "Verifies the supplied account with the provided verification code; if the
  account has a registration lock set then the PIN should be provided as well.
  Returns an updated session map that contains an updated account map and should
  be saved to persistent storage (simply provided the updated session map to the
  account/store function). Requires a valid session map as provided by
  `start-session`."
  ([session code] (verify-code session code nil))
  ([session code registration-lock-pin]
   (try+
    (.verifyAccountWithCode (get-in session [:session :manager])
                            (string/replace code "-" "")  ; remove any dashes from the code
                            (get-in session [:account :signaling-key])
                            (get-in session [:account :registration-id])
                            true    ; fetches messages
                            registration-lock-pin
                            (UnidentifiedAccess/deriveAccessKeyFrom (get-in session [:account :profile-key]))
                            false) ; unrestricted unidentified access
    (assoc session :account
           (merge (:account session)
                  {:registered true}))
    (catch Exception exception
      (warn "Error verifying account access code:" (.getMessage exception))
      (throw+ {:type :verify-fail :message (.getMessage exception)})))))
