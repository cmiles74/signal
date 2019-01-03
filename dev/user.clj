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
   [cmiles74.signal.signal :as signal])
  (:use [slingshot.slingshot :only [try+ throw+]]))

;;
;; Create a new configuration file
;;
;; (def c (cli/store-user-config "4135557878"))

;;
;; Load the configuration file
;;
;; (def c (cli/load-user-config))

;;
;; Create a signal manager
;;
;; (def m (signal/manager c))

