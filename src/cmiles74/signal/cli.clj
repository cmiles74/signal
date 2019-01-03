(ns cmiles74.signal.cli
  (:gen-class)
  (:require
   [taoensso.timbre :as timbre
    :refer (log  trace  debug  info  warn  error  fatal  report
                 logf tracef debugf infof warnf errorf fatalf reportf
                 spy get-env log-env)]
   [taoensso.timbre.profiling :as profiling
    :refer (pspy pspy* profile defnp p p*)]
   [cmiles74.signal.account :as account]
   [cmiles74.signal.signal :as signal])
  (:use [slingshot.slingshot :only [try+ throw+]]))

;;
;; Bootstrapping functions
;;

(defn main
  "Bootstraps the application and handles command line arguments."
  [& args]
  (info "Hello from the Signal Client!"))

(defn -main
  "The bootstrapping function used to start the application."
  [& args]
  (apply main args))
