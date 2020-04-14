(ns cogmind-scores.main
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [cogmind-scores.core :as impl]))

(def cli-options
  [["-d" "--data DIRECTORY" "Data directory"
    :validate [#(.isDirectory (io/file %))
               "Must be a directory"]]
   ["-h" "--help"]])

(defn show-usage [summary]
  (println (str/join \newline
             ["Usage: [options] <command>"
             summary
             "Command is one of:"
             "leaders [num-entries]    Generate leader boards"])))

(defn show-errors [errors]
  (println (str/join \newline (concat ["Errors occurred:"] errors))))

(defn as-int [x]
  (if (number? x) (int x)
    (Integer/parseInt x)))

(defn show-leaders
  ([options] (show-leaders options 10))
  ([options num-entries]
   (if (empty? (:data options))
     (show-errors ["Must specify --data directory."])
     (let [data-dir (:data options)
           score-data (impl/read-data data-dir)]
       (impl/render-group-scores score-data
                                 (as-int num-entries)
                                 impl/score-keyfn)))))

(defn -main [& args]
  (let [{:keys [options arguments errors summary]}
        (parse-opts args cli-options)]
    (cond (:help options) (show-usage summary)
          errors (show-errors errors)
          (empty? arguments) (show-usage summary)
          (= "leaders" (first arguments))
          (apply show-leaders options (next arguments))
          :else (show-errors [(str "Unknown command '" (first arguments) "'")]))
    ))
