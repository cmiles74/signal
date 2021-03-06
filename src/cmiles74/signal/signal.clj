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
   [java.util Optional]
   [java.util Locale]
   [java.net URLEncoder]
   [java.net URLDecoder]
   [org.bouncycastle.jce.provider BouncyCastleProvider]
   [org.whispersystems.signalservice.api.push TrustStore]
   [org.whispersystems.signalservice.api.crypto UnidentifiedAccess]
   [org.whispersystems.signalservice.internal.configuration SignalServiceConfiguration]
   [org.whispersystems.signalservice.internal.configuration SignalServiceUrl]
   [org.whispersystems.signalservice.internal.configuration SignalCdnUrl]
   [org.whispersystems.signalservice.internal.configuration SignalContactDiscoveryUrl]
   [org.whispersystems.signalservice.api SignalServiceAccountManager]
   [org.whispersystems.signalservice.internal.util Base64]
   [org.whispersystems.signalservice.api.util UptimeSleepTimer]))

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
             :manager (SignalServiceAccountManager.
                           service-config
                           (get-in account [:account :username])
                           (get-in account [:account :password])
                           (get-in signal-config [:signal :user-agent])
                           (UptimeSleepTimer.))})
     (catch Exception exception
       (throw+ {:type :session-fail :message (.getMessage exception)})))))

(defn register-sms
  "Registers the account and requests a verification code via SMS message.
  Requires a valid session map as provided by `start-session`.

  The account and device are not considered to be 'registered' until the
  `verify-code` function has been invoked with the verification code."
  [session]
  (try+
   (.requestSmsVerificationCode (get-in session [:session :manager]))
   true
   (catch Exception exception
     (throw+ {:type :register-fail :message (.getMessage exception)}))))

(defn register-voice
  "Registers the account and requests a verification code via voice call for the
  provided locale (or the default locale if no locale is provided). Requires a
  valid session map as provided by `start-session`.

  The account and device are not considered to be 'registered' until the
  `verify-code` function has been invoked with the verification code."
  ([session] (register-voice session (Locale/getDefault)))
  ([session locale]
   (try+
    (.requestVoiceVerificationCode (get-in session [:session :manager])
                                   locale)
    true
    (catch Exception exception
      (throw+ {:type :register-fail :message (.getMessage exception)})))))

(defn verify-code
  "Verifies the supplied account with the provided verification code; if the
  account has a registration lock set then the PIN should be provided as well.
  Returns an updated session map that contains an updated account map and should
  be saved to persistent storage (simply provided the updated session map to the
  account/store function). Requires a valid session map as provided by
  `start-session`.

  After this function completes, the account and device are considered
  'registered'."
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
                            false)  ; unrestricted unidentified access
    (assoc session :account
           (merge (:account session)
                  {:registered true}))
    (catch Exception exception
      (warn "Error verifying account access code:" (.getMessage exception))
      (throw+ {:type :verify-fail :message (.getMessage exception)})))))

(defn unregister
  "Un-registers the current account, the `fetchMessages` property for this account
  will also be set to false on the server. If this is the master device then
  messages can no longer be sent to this account. If this is a linked device,
  messages can still be sent to the account but this device will no longer
  receive them. Returns an updated session with an updated account that
  indicates the account is no longer registered, this data should be saved to
  persistent storage via the account/store function."
  [session]
  (if (get-in session [:account :registered])
    (try+
     (.setGcmId (get-in session [:session :manager]) (Optional/empty))
     (assoc session :account
            (merge (:account session)
                   {:registered false}))
     (catch Exception exception
       (throw+ {:type :unregister-fail :message (.getMessage exception)})))
    session))

(defn link-device-url
  "Provides a URL that can be used to link a new device to this account."
  [session device-name]
  (let [device-id (.getNewDeviceUuid (get-in [:session :manager]))]
    (str "tsdevice:/?uuid="
         (URLEncoder/encode device-id "utf-8")
         "&pub_key="
         (URLEncoder/encode
          (Base64/encodeBytesWithoutPadding
           (.serialize (.getPublicKey (get-in session [:account :identity-keypair])))) "utf-8"))))

(defn finish-device-registration
  "Waits for the device registration to complete (blocks until registration completes or times out)."
  [session device-name]
  (let [manager (get-in session [:session :manager])]
    (try+
     (.finishNewDeviceRegistration manager
                                   (get-in session [:account :identity-keypair])
                                   (get-in session [:account :signaling-key])
                                   false  ; supports SMS
                                   true   ; fetches messages
                                   (get-in session [:account :registration-id])
                                   device-name)
     (catch Exception exception
       (throw+ {:type :device-registration-fail :message (.getMessage exception)})))))
