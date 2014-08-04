(ns course-converter.core
  (:use cheshire.core
        cheshire.generate
        seesaw.core
        seesaw.border
        seesaw.chooser
        )
  (:import java.text.NumberFormat
           java.text.DecimalFormat
           java.util.Locale
           java.io.File)
  (:gen-class))

(defn generate-output-map [name
                           authors
                           type
                           numlaps
                           weather-severity
                           spawns
                           checkpoints]
  "Generates course data based on given converted coordinates"
  {
   "name" name
   "authors" authors
   "type" type
   "numLaps" numlaps
   "timeLimitSeconds" 800
   "weatherSeverity" weather-severity
   "prizeMoney" 10000
   "spawns" spawns
   "checkpoints" checkpoints
   })

(defn generate-spawn [waypoint car-models]
  (merge 
   waypoint
   {
    "modelIds" car-models
    "templates" ["." "."]
    "decals" []
    }))

(defn generate-spawns [waypoints car-models]
  (map #(generate-spawn % car-models) waypoints))

(defn generate-checkpoint [waypoint]
  (merge 
   (dissoc waypoint "angle") 
   {
    "radius" 10
    "actions" []
    "type" 7
    "useIcon" false
    "validVehicles" []
    }))

(defn generate-checkpoints [waypoints]
  (map generate-checkpoint waypoints))

(defn convert-to-course-format [filename coursename authors type numlaps 
                                numcheckpoints car-models weather-severity]
  (let [file-str (slurp filename)
        waypts (get (parse-string file-str) "waypoints")
        num-spawns (- (count waypts) numcheckpoints)
        spawns (generate-spawns (take-last num-spawns waypts) car-models)
        checkpoints (generate-checkpoints (take numcheckpoints waypts))
        ]
    (generate-string
     (generate-output-map 
      coursename
      authors
      type
      numlaps
      weather-severity
      spawns
      checkpoints))))

(defn create-gui! []
  (native!)
  (let [f (frame :title "JC2Mp course converter")
        filepicker (button :text "Choose")
        course-name-input (text)
        authors-input (text)
        type-picker (combobox :model ["Linear" "Circuit"])
        number-laps-input (text :text "1")
        number-checkpoints-input (text)
        car-ids-input (text)
        weather-severity-input (text :text "-1")
        submit-button (button :text "Convert")
        result-label (label)
        directory-name (atom nil)
        file-path (atom nil)]
    (config! f :content 
               (grid-panel 
                :columns 2
                :items ["Waypoints json file" filepicker
                        "Course name" course-name-input
                        "Authors (comma separated)" authors-input
                        "Type" type-picker
                        "Number of laps" number-laps-input
                        "Number of checkpoints" number-checkpoints-input
                        "Car Ids (comma separated)" car-ids-input
                        "Weather severity" weather-severity-input
                        submit-button result-label
                        ]
                :border (empty-border :thickness 10)
                :hgap 10
                :vgap 10))
    (listen filepicker :mouse-clicked 
            (fn [e] 
              (let [file (choose-file :type :open)
                    filepath (.getAbsolutePath file)
                    dirname (.getParent file)]
                (config! filepicker :text (.getName file))
                (config! result-label :text "")
                (reset! directory-name dirname)
                (reset! file-path filepath))))
    (listen submit-button :mouse-clicked 
            (fn [e] 
              (let [coursename (config course-name-input :text)
                    authors-string (config authors-input :text)
                    authors (clojure.string/split authors-string #",")
                    type (str (selection type-picker))
                    nlaps (Integer/valueOf (config number-laps-input :text))
                    ncheckpoints (Integer/valueOf (config number-checkpoints-input :text))
                    car-ids-string (config car-ids-input :text)
                    car-ids (map #(Integer/valueOf %) 
                                 (clojure.string/split car-ids-string #","))
                    weather-severity-string (config weather-severity-input :text)
                    weather-severity (let [wsfloat (Double/valueOf weather-severity-string)]
                                       (if (= wsfloat -1.0) -1 wsfloat))
                    converted-string (convert-to-course-format @file-path 
                                                               coursename 
                                                               authors
                                                               type
                                                               nlaps
                                                               ncheckpoints
                                                               car-ids
                                                               weather-severity)]
                (spit (java.io.File. (str @directory-name 
                                          (File/separator) coursename 
                                          ".course")) converted-string)
                (config! result-label :text "done"))))
    (-> f pack! show!)))



(defn -main []
  ;write coordinates in proper way
  (add-encoder java.lang.Double
               (fn [c generator]
                 (let [fmt (NumberFormat/getNumberInstance Locale/UK)
                       _ (.setGroupingUsed fmt false)
                       _ (.setMaximumFractionDigits fmt 13)]
                   (.writeRawValue generator (. fmt format c)))))
  (create-gui!))
