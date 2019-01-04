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
   [cmiles74.signal.secret :as secret]
   [cmiles74.signal.account :as account]
   [cmiles74.signal.signal :as signal])
  (:use [slingshot.slingshot :only [try+ throw+]])
  (:import
   [org.whispersystems.libsignal IdentityKeyPair]
   [org.whispersystems.libsignal.util KeyHelper]))

;; We have two primary types of data:
;;   - account
;;   - session
;;
;; Both of these are managed as maps of data, the account information persists
;; over all Signal sessions, the session data only lives as long as we are
;; actively interacting with the Signal service.
;;
;;  {:account {...}
;;   :session {...}}
;;
;; The storage methods store only the map of data they are designed to store;
;; the account/store function will only persist the account data and it knows
;; where to find it.

;;
;; Create a new account
;;
;; (def a (account/create "4135557878"))

;;
;; Store the account to the default location.
;;
;; (account/store a)

;;
;; Load the account from the default location.
;;
;; (def a (account/load))

;;
;; Create a signal manager and returns a new map representing the account and
;; the current session data.
;;
;; (def s (signal/start-session a))

;;
;; Request SMS verification code
;;
;; Registering will un-register your other device. You can link that device in
;; later.
;;
;; (signal/register-sms s)

;;
;; Verify the code
;;
;; Verification will complete registration and un-register your other device.
;; You will need to link in that device later.
;;
;; (def s (signal/verify-code s CODE PIN))
;;
;; Store the updated account from inside the session
;;
;; (account/store s)

