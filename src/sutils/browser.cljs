(ns sutils.browser
  "Clojurescript utilities for interacting with the browser")

(enable-console-print!)

(defn clog
  "Print one arg to console transparently."
  [arg]
  (js/console.log arg)
  arg)

(defn get-id-element
  "Get an element by id."
  [id]
  (js/document.getElementById id))

(defn add-listener
  "Add an event listener to an element."
  [ele event handler]
  (.addEventListener ele event handler))

;; sound

(defn play-audio
  "Play an audio file."
  [file]
  (.play (js/Audio. file)))

(defn play-audio-el
  "Play an audio element."
  [id]
  (.play (get-id-element id)))
