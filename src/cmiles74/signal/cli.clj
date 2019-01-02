(ns cmiles74.signal.cli
  (:require
   [taoensso.timbre :as timbre
    :refer (log  trace  debug  info  warn  error  fatal  report
                 logf tracef debugf infof warnf errorf fatalf reportf
                 spy get-env log-env)]
   [taoensso.timbre.profiling :as profiling
    :refer (pspy pspy* profile defnp p p*)]
   [cmiles74.common.config :as config])
  (:use [slingshot.slingshot :only [try+ throw+]]))

;; default configuration
(def DEFAULT-CONFIG
  {:signal
   {:user-agent "cmiles74-signal"
    :signal-url "https://textsecure-service.whispersystems.org"
    :cdn-url "https://cdn.signal.org"}})

;; default name of the configuration file
(def DEFAULT-CONFIG-FILE ".cmiles74-signal.yml")

(defn load-config-file
  "Loads the configuration file from the provided path, returns a map with the
  configuration values."
  [path-in]
  (let [config (merge (config/load-config-file DEFAULT-CONFIG
                                               DEFAULT-CONFIG-FILE
                                               path-in)
                      DEFAULT-CONFIG)]
    (if (nil? (get-in config [:account :username]))
      (throw+ {:type :no-config :message "No configuration file found"}))
    config))


