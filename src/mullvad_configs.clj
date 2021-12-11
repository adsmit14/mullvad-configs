(ns mullvad-configs
  (:require [clojure.string :as string]
            [clojure.java.io :as io])
  (:import [java.nio.file FileSystems]
           [java.nio.file Paths]
           [java.net URI]
           [java.nio.file Files]
           [java.nio.file CopyOption]
           [java.nio.file StandardCopyOption]))

(defn country-city [zip] (let [[_ country city] 
                               (re-find #"mullvad_.+_(.+)_(.+)\.zip", zip)]
                           [country city]))

  (defn config-name [config]
    (.getName config (- (.getNameCount config) 1)))

  (defn regionalize [path country city]
    (str (.toString (.getParent path)) (str country "-" city "-" (config-name path))))

  (defn get-files [path]
    (iterator-seq (.iterator (Files/walk path (into-array java.nio.file.FileVisitOption '())))))

  (defn get-roots [fs] (iterator-seq (.iterator (.getRootDirectories fs))))

  (defn regionalize-zip [zip] (let [[country city] (country-city zip)
                                    fs (FileSystems/newFileSystem (Paths/get (URI/create (str "file://" zip))) nil)
                                    files (filter #(and (not (nil? %)) (string/ends-with? (.toString %) ".conf"))
                                                  (get-files (first (get-roots fs))))]
                                (doseq [source files] (let [target (.resolveSibling source (regionalize source country city))]
                                                        (println "Renaming" (.toString source) "to" (.toString target))
                                                        (Files/move source target
                                                                    (into-array CopyOption [StandardCopyOption/REPLACE_EXISTING]))))
                                (.close fs)))

  (defn mullvad-zips [location] (map #(.getAbsolutePath %)
                                     (filter #(and (.isFile %)
                                                   (re-find #"mullvad_wireguard_.+\.zip" (.getName %)))
                                             (.listFiles (io/file location)))))


  (defn -main
    "Extracts and regionalizes wireguard configuration zip files."
    [& args]
    (doseq [zip (mullvad-zips (first args))]
      (regionalize-zip zip)))
