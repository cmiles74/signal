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
;; (def c (account/load))

;;
;; Create a signal manager
;;
;; (def m (signal/manager c))

;;
;; Request SMS verification code
;;
;; (signal/register-sms m)


