(defproject cogmind-scores "0.1.0-SNAPSHOT"
  :description "Scoresheet tools"
  :url ""
  :license {:name "Private"
            :url ""}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [clojusc/protobuf "3.5.1-v1.1"]
                 [org.clojure/tools.cli "1.0.194"]
                 ; [org.clojure/data.json "0.2.7"]
                 ; [metosin/jsonista "0.2.5"]
                 ; [incanter "1.9.3"]
                 ; [hiccup "1.0.5"]
                 ]
  :main ^:skip-aot cogmind-scores.main
  :java-source-paths ["Protobuf"])
