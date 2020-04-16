(ns cogmind-scores.dlprotobuf
  (:require [clojure.java.io :as io]))

(def ^:private protobuf-uri "https://raw.githubusercontent.com/Kyzrati/cogmind-scoresheet/master/scoresheet.proto")
(def ^:private protobuf-file "resources/proto/scoresheet.proto")

; Thanks https://stackoverflow.com/a/19297746 for the copy technique
(defn -main [& args]
  (if (not (.exists (io/as-file protobuf-file)))
    (do
      (println "Downloading" protobuf-uri)
      (with-open [src (io/input-stream protobuf-uri)
                  dst (io/output-stream protobuf-file)]
        (io/copy src dst))
      (println "Saved to" protobuf-file))
    (println "Skipping download since" protobuf-file "exists")))
