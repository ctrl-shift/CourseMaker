(defproject course-converter "1.0"
  :description "A simple converter of waypoints to race course file for Just Cause 2 MP"
  :license {:name "GNU Lesser General Public License v3"
            :url "https://www.gnu.org/licenses/lgpl-3.0-standalone.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [cheshire "5.3.1"]
                 [seesaw "1.4.4"]]
  :main course-converter.core)
