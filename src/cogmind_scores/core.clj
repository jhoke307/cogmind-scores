(ns cogmind-scores.core)

(require '[clojure.java.io :refer [as-file]])
(require '[clojure.string :as s])
(require '[clojure.set])
(require '[protobuf.core :as protobuf])
(import '(Protobuf ScoresheetOuterClass))
(import '(Protobuf ArchivedScoresheet))
(import 'clojure.lang.Reflector)

; utilities
(defn snake-case-kw [str]
  (-> str
      (s/replace #"_" "-")
      s/lower-case
      keyword))

(defn enum-info [enum-desc]
  (map (juxt
         #(. % getName)
         #(. % getNumber)
         #(.. % getOptions (getExtension Protobuf.ScoresheetOuterClass/enumDisplayName)))
       (. enum-desc getValues)))

(defn enum-desc-name-map [enum-desc]
  (into {}
        (for [[name num desc] (enum-info enum-desc)]
          [(if (= 0 num) nil (snake-case-kw name)) desc])))

(defn enum-desc [container enum-name]
  (let [qualified-name (str container "$" enum-name)
        klass (resolve (symbol qualified-name))
        method "getDescriptor"
        args (into-array [])]
    (Reflector/invokeStaticMethod klass method args)))

(defn scoresheet-name-map
  "Given an enum-name, return a map from the keyword value that will appear
  in the parsed protobuf's mapping to the human readable string provided in
  the protobuf definition."
  [enum-name]
  (-> (enum-desc "Protobuf.ScoresheetOuterClass" enum-name)
      enum-desc-name-map))

; Cache maps for names
(def map-names (scoresheet-name-map "MapType"))
(def difficulty-names (scoresheet-name-map "DifficultyType"))
(def special-mode-names (scoresheet-name-map "SpecialModeType"))
(def movement-input-type-names (scoresheet-name-map "MovementInputType"))

; TODO I wonder if I can map the enum path (:header :special-mode) to
; the type class automatically to get the name map...

(def mutation-names
  {:imprinted "Imprinted"
   :farcom "Farcom"
   :dc "DC"
   :rif "RIF"
   :crm "CRM"})

; protobuf loading
(def protodef (protobuf/create Protobuf.ArchivedScoresheet$ArchivedPostScoresheetRequest))
(defn read-scores [f]
  (let [all-bytes (java.nio.file.Files/readAllBytes
                    (.toPath
                      (as-file f)))]
    (try
      (protobuf/bytes-> protodef all-bytes)
      (catch Exception e (str "caught exception on " f ": " (.getMessage e))))))

(defn safe-pos? [x] (and (number? x) (pos? x)))
(defn leaderboard-data [pb]
  (let
    [{scores :scoresheet} pb
     bonus (-> scores :bonus)
     destroyed-mainc (-> scores :bonus :destroyed-mainc)
     destroyed-architect (-> scores :bonus :destroyed-architect)
     win (-> scores :header :win)]
  {:player-id (-> scores :meta :player-id),
   :player-name (-> scores :header :player-name),
   :link (str "https://cogmind-api.gridsagegames.com/scoresheets/" (-> pb :bucket-name)),
   :win win,
   ; :win-type (-> scores :game :win-type),
   :final-location (-> scores :cogmind :location)
   :special-mode (-> scores :header :special-mode)
   :difficulty (-> scores :header :difficulty)
   :challenges (-> scores :challenges :challenges)
   :score (-> scores :performance :total-score)
   :pacifist (:pacifist bonus)
   :high-alert-combat-kills (:high-alert-combat-kills bonus)
   :combat-kills (-> scores :stats :kills :combat-hostiles-destroyed)
   :mutations (cond-> []
                (:was-imprinted bonus) (conj :imprinted)
                (:aligned-with-farcom bonus) (conj :farcom)
                (:used-data-conduit bonus) (conj :dc)
                (:used-rif-installer bonus) (conj :rif)
                (:used-core-reset-matrix bonus) (conj :crm))
   :movement (get-in scores [:options :movement])
   :final-depth (if win
                  0
                  (-> scores :route :entries last :location :depth))
   :boss-kills (+ (if (safe-pos? destroyed-mainc) 1 0)
                  (if (safe-pos? destroyed-architect) 1 0))
   :regions-visited (-> scores :performance :regions-visited :count)
   :date (-> scores :header :run-end-date)}))

(defn render-location [{mapkey :map depth :depth}]
  (str depth "/" (get map-names mapkey mapkey)))

(defn render-entry [data score-keyfn]
  (str (:player-name data) " "
       (if (:win data) "Ascended" (render-location (:final-location data)))
       (apply str (repeat (:boss-kills data) \+)) " "
       "[url=" (:link data) "]"
       ; (:score data) "[/url] "
       (score-keyfn data) "[/url] "
       (s/join ", "
               (apply conj
                 (:challenges data)
                 (map mutation-names (:mutations data))))))


; Basic plan:
; Read all scores into a vector?
; Partition by difficulty, special modes
; Sort - pick highest for each user in each category (high scores /
; furthest area reached)
; (group-by (juxt :player-id :special-mode) all-leaderboard-data)


(defn process-score-data [f dir]
  (let [files (filter #(.isFile %) (file-seq (as-file dir)))]
    (map #(f (read-scores %)) files)))

(defn read-score-data [dir]
  (let [files (filter #(.isFile %) (file-seq (as-file dir)))]
    (map read-scores files)))

(defn read-data [dir]
  (let [files (filter #(.isFile %) (file-seq (as-file dir)))]
    (map #(leaderboard-data (read-scores %)) files)))

(def all-leaderboard-data
  (let [files (filter #(.isFile %) (file-seq (as-file ".")))]
    (map #(leaderboard-data (read-scores %)) files)))


(def ^:private default-num-scores 10)

;(pprint
;  (take 10
;        (sort-by #(or (:score %) 0)
;                 (for [[id scores] player-groups]
;                   (apply max-key #(or (:score %) 0) scores))
(defn group-scores [data]
  (group-by (juxt :difficulty
                  :special-mode
                  #(and (seq (:challenges %)) "Challenge")) data))

(defn score-keyfn [score]
  (or (:score score) 0))
(defn pacifist-keyfn [score]
  (+ (* 1000000000 (:pacifist score)) (or (:score score) 0)))

(defn top-scores
  ([scores] (top-scores scores score-keyfn))
  ([scores keyfn]
   (sort-by keyfn #(compare %2 %1)
            (for [[id player-scores] (group-by :player-id scores)]
              (apply max-key keyfn player-scores)))))
(defn top-scores-pacifist [scores]
  (top-scores scores pacifist-keyfn))

(defn render-group-name [[difficulty special challenge]]
  (if (nil? special)
    (str (difficulty-names difficulty) " " challenge)
    (str (special-mode-names special) " "
         (difficulty-names difficulty) " "
         challenge)))

(defn sort-groups [group-names]
  (sort-by identity
           (fn [[adiff amode achal] [bdiff bmode bchal]]
             (compare [achal amode adiff] [bchal bmode bdiff]))
           group-names))

(defn render-group-scores
  ([data] (render-group-scores data score-keyfn))
  ([data keyfn] (render-group-scores data default-num-scores keyfn))
  ([data num-scores keyfn]
   (let [grouped-score-map (group-scores data)
         sorted-groups (sort-groups (keys grouped-score-map))]
     (doseq [group-name sorted-groups]
       (let [scores (get grouped-score-map group-name)]
         (println (render-group-name group-name))
         (println
           (s/join "\n"
                   (map-indexed #(str (inc %1) ". " %2)
                                (map #(render-entry % keyfn)
                                     (take num-scores
                                           (top-scores scores keyfn))))))
         (println))))))

; TODO Use this in above
(defn render-score-data
  ([data] (render-score-data data score-keyfn))
  ([data keyfn]
   (s/join "\n"
           (map-indexed #(str (inc %1) ". " %2)
                        (map #(render-entry % keyfn) data)))))

; (def test-dir "cogmind-scores-20200108T070002")
; (-> (process-score-data leaderboard-data test-dir)
; render-group-scores)

(defn- get-score [scoresheet]
  (get-in scoresheet [:scoresheet :performance :total-score]))

(defn stats [scoresheets]
  {:total (count scoresheets),
   :total500 (count (filter #(>= (get-in % [:scoresheet :performance :total-score]) 500 scoresheets))),
   })

; (frequencies (process-score-data #(get-in % [:scoresheet
; :challenges])) test-dir)

(defn score-freq
  ([key dir] (score-freq key dir identity))
  ([key dir kmap]
   (clojure.set/rename-keys
     (frequencies (process-score-data #(get-in % key) dir)) kmap)))

(defn interesting-conditions [datum]
  (let [mutations (:mutations datum)
        win (if (:win datum) 1 0)]
    (as-> {} m
      (assoc m :overall [1 win])
      (assoc m (difficulty-names (:difficulty datum)) [1 win])
      (assoc m (movement-input-type-names (:movement datum)) [1 win])
      (reduce #(assoc %1 %2 [1 win]) m mutations))))

; Beautiful with map, but it builds a chain of thunks N deep, one for
; each step in the reduce. I haven't understood how to get around it
; without switching to mapv - it should be possible to fold the thunks
; together somehow? See https://stackoverflow.com/a/24959561
(defn count-wins [ldata]
  (reduce (fn [a b] (merge-with #(mapv + %1 %2) a b))
          {}
          (map interesting-conditions ldata)))

(defn bb-win-table [windata]
  (str "[table]\n"
       "[tr][td]Total[/td][td]Win[/td][td]Win Rate (%)[/td][/tr]\n"
       (apply str (map (fn [[k [total win]]]
                         (str "[tr]"
                              "[td]" k "[/td]"
                              "[td]" total "[/td]"
                              "[td]" win "[/td]"
                              "[td]" (format "%.1f" (float (* 100 (/ win total)))) "[/td]"
                              "[/tr]\n")) windata))
       "[/table]"))

; 4 Feb 2020
; Question: how likely is it to get each RIF ability?

(defn rif-stats [score-data]
  (let [get-rif #(-> % :stats :bothacking :used-rif-installer)
        get-route-entries #(-> % :scoresheet :route :entries)
        get-events #(filter seq (map get-rif (get-route-entries %)))
        get-keys #(set (flatten (map keys %)))
        count-map #(into {} (for [k (get-keys %)]
                              [k (map (fn [entry]
                                        (if (contains? entry k) 1 0))
                                      %)]))
        rif-games (filter #(seq (-> % :scoresheet get-rif)) score-data)
        add-seq (fn [a b]
                  (let [vlen (max (count a) (count b))
                        apad (take vlen (concat a (repeat 0)))
                        bpad (take vlen (concat b (repeat 0)))]
                    (map + apad bpad)))
        rif-game-counts (map #(-> % get-events count-map) rif-games)]
    (reduce #(merge-with add-seq %1 %2)
            {}
            rif-game-counts)))

(defn rif-table [stats]
  (apply str (for [[k v] stats]
               (str "[tr][td]" k "[/td]"
                    (apply str (map #(str "[td]" % "[/td]") v))
                    "[/tr]\n"))))

; Another question: how common are various kinds of wins?

(defn win-stats [ldata]
  (let [loc-to-win {:map-w00 0
                    :map-w01 1
                    :map-w02 2
                    :map-w03 3
                    :map-w04 4
                    :map-w05 5
                    :map-w06 6}
        get-win (fn [game]
                 (let [loc (-> game :final-location :map)
                       win-type (get loc-to-win loc loc)
                       win-str (str "w" win-type)
                       bosses (:boss-kills game)]
                   (apply str win-str (repeat bosses "+"))))]
    (->> ldata
         (filter :win)
         (map get-win)
         frequencies)))

(defn win-table [ldata]
  (let [stats (win-stats ldata)]
    (str (apply str "[table]\n"
                "[tr][td]Win[/td][td]-[/td][td]+[/td][td]++[/td][/tr]\n"
           (for [idx (range 0 7)]
             (let [wstr (str "w" idx)
                 none (get stats wstr)
                 one (get stats (str wstr "+"))
                 two (get stats (str wstr "++"))]
               (str "[tr]"
                    "[td]" wstr "[/td]"
                    "[td]" none "[/td]"
                    "[td]" one "[/td]"
                    "[td]" two "[/td]"
                    "[/tr]\n"))))
         "[/table]\n")))
