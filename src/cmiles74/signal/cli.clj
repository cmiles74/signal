(ns cmiles74.signal.cli
  (:gen-class)
  (:require
   [taoensso.timbre :as timbre
    :refer (log  trace  debug  info  warn  error  fatal  report
                 logf tracef debugf infof warnf errorf fatalf reportf
                 spy get-env log-env)]
   [taoensso.timbre.profiling :as profiling
    :refer (pspy pspy* profile defnp p p*)]
   [cmiles74.common.config :as config]
   [cmiles74.signal.secret :as secret]
   [cmiles74.signal.signal :as signal])
  (:use [slingshot.slingshot :only [try+ throw+]]))

;; default name of the configuration file
(def default-config-file ".cmiles74-signal.yml")

(defn store-account-file
  "Stores the provided map of account data to to the provided path with the
  specified file name."
  ([config-map] (store-account-file (System/getProperty "user.home") default-config-file config-map))
  ([path-in filename config-map]
   (let [config-map-persist (secret/transform-account-for-storage config-map)]
     (try+
      (config/save-config-file path-in
                               filename
                               config-map-persist)
      (catch Exception exception
        (throw+ {:type :config-save-fail :message (.getMessage exception)}))))))

(defn load-account-file
  "Loads the configuration file from the provided path, returns a map with the
  account values."
  [path-in]
  (let [config (config/load-config-file path-in
                                        default-config-file
                                        {})]
    (if (nil? (get-in config [:account :username]))
      (throw+ {:type :no-config :message "No configuration file found"}))
    (secret/transform-account-from-storage config)))

(defn store-account
  [account]
  (store-account-file account))

(defn load-account
  "Loads the customer's account from the default configuration file."
  []
  (try+
   (load-account-file nil)
   (catch [:type :no-config] {:keys [message]}
     (warn message))))

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
