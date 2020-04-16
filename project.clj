(defproject cogmind-scores "0.1.0-SNAPSHOT"
  :description "Scoresheet tools"
  :url ""
  :license {:name "Private"
            :url ""}
  :plugins [[lein-protobuf "0.5.0"]]
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [clojusc/protobuf "3.5.1-v1.1"]
                 [org.clojure/tools.cli "1.0.194"]
                 ; [org.clojure/data.json "0.2.7"]
                 ; [metosin/jsonista "0.2.5"]
                 ; [incanter "1.9.3"]
                 ; [hiccup "1.0.5"]
                 ]
  ; It looks like lein-protobuf has code to look for protoc on the path
  ; but it seems to be falling back to the download and build method for
  ; some reason. Just hardcode it.
  :protoc "/usr/bin/protoc"
  :main ^:skip-aot cogmind-scores.main
  :prep-tasks ["download-protobuf" "protobuf" "compile"]
  :profiles {:download-protobuf {:prep-tasks ^:replace []
                          :main cogmind-scores.dlprotobuf
                          :source-paths ^:replace ["dlprotobuf-src"]}}
  :aliases {"download-protobuf" ["with-profile" "+download-protobuf" "run"]}
  )
