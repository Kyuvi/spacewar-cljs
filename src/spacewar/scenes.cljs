(ns spacewar.scenes
  {:author "Kyuvi"
   :license {:name "GPL-3.0 WITH Classpath-exception-2.0"
             :url "https://www.gnu.org/licenses/gpl-3.0.html"}}
  (:require [reagent.core :as rg :refer [atom]]
            [reagent.dom :as rd]
            [re-frame.core :as rf]
            [sutils.canvas :as cvu]
            [sutils.rf :as rfu]
            [spacewar.hershey :as hsu]
            [spacewar.prep :as pr]
            [spacewar.obj :as obj]
            [spacewar.kone :as kn]
            [spacewar.rfm.subs :as subs]))

(defn draw-game-scene [ctx state]
  (let [scene (:scene state)
        {:keys [ship1 ship2 gravity-well paused]} scene
        ]
    (run! #(obj/draw-sprite % ctx) [ship1 ship2 gravity-well])
    (when paused
      (hsu/write-centered ctx (/ (:height pr/game-view) 2) "PAUSED" 4 ))))

(defn characterize-string
  "Returns a character that represents the arrow keys otherwise the string `s`."
  [s]
  (case s
    "ArrowUp" "↑"
    "ArrowDown" "↓"
    "ArrowLeft" "<"
    "ArrowRight" ">"
     s))

(defn draw-menu-scene [ctx state]
  (let [{:keys [p1 p2]} (get-in state [:settings :controls])
        p1-keys (apply str (map characterize-string (vals p1)))
        p2-keys (apply str (map characterize-string (vals p2)))
        t1 "SPACEWAR!"
        t2 "IN CLOJURESCRIPT"
        o1 "1p start"
        o2 "2p start"
        o3  "options"
        o4  "credits"
        b1 (str p1-keys " - player 1     player 2 - " p2-keys)
        b2 "enter - select            esc - menu"
        b3 "space - select/pause"
        b4 pr/version
        cursor (:cursor state)]
    (run!
      (fn [[txt ypos size]]
        (hsu/write-centered ctx ypos txt size ))
      [[t1 88 5] [t2 158 2.7]
       [o1 240 2] [o2 290 2] [o3 340 2] [o4 390 2]
       [b1 470 1.2] [b2 500 1.2] [b3 530 1.2] [b4 570 1]])
    (obj/draw-sprite cursor ctx)))

(defn draw-credits-scene [ctx]
  (let [t1 "SPACEWAR!"
        t2 "in clojurescript"

        o1 "This is an attempt of making the game spacewar! using"
        o2 "clojurescript, reagent and re-frame. You can find more"
        o3 "information about the project on it's codeberg page:"
        o4 "https://codeberg.org/kyuvi/spacewar-in-cljs"
        o5 "An ode to Steve 'slug' Russell,"
        o6 "who wrote the first video game (spacewar!) and the first"
        o7 "lisp interpreter (keeping the s-expressions :)."
        o8  "Also Thanks to John McCarthy, Richard Stallman and Rich Hickey"
        o9  "for Lisp, GNU Emacs (and the GPL) and Clojure respectively,"
        o10 "and thanks to Luxedo for his excellent javascript version at"
        o11 "https://github.com/luxedo/spacewar-almost-from-scratch"

        c1 "This project is released under the GNU GPL3 license."
        c2 "Copyright (C) 2023 Kyuvi"
        c3 ""

        b1 "esc - menu"
        b4 pr/version]
    (run!
     (fn [[txt ypos size]]
       (hsu/write-centered ctx ypos txt size ))
     [[t1 55 4] [t2 105 2]
      [o1 170 1] [o2 190 1] [o3 210 1] [o4 230 0.8] [o5 270 1] [o6 290 1]
      [o7 310 1] [o8 330 1] [o9 350 1][o10 380 1] [o11 400 0.8]
      [c1 480 0.9][c2 500 0.9][c3 520 0.9]
      [b1 555 1] [b4 575 1]])))

;; (defn draw-options-scene [ctx state])

(defn draw-controls-scene [ctx state]
  (let [{:keys [p1 p2]} (get-in state [:settings :controls])
        t1 "CONTROLS"
        t2 "PLAYER 1"
        spacer1 "      "
        spacer2 "         "
        o1 (str "ROCKETS: " (characterize-string (:thrust p1))
                spacer1
                "FIRE: " (characterize-string (:fire p1)))
        o2 (str "LEFT: " (characterize-string (:left p1))
                spacer2
                "RIGHT: " (characterize-string (:right p1)))
        t3 "PLAYER 2"
        o3 (str "ROCKETS: " (characterize-string (:thrust p2))
                spacer1
                "FIRE: " (characterize-string (:fire p2)))
        o4 (str "LEFT: " (characterize-string (:left p2))
                spacer2
                "RIGHT: " (characterize-string (:right p2)))
        o5 "MENU" ; "OPTIONS"
        b1 "valid control keys are: either one of a-z/0-9"
        b2 "or 'up', 'down', 'left' or 'right'."
        ;; b1 "esc - menu"
        b4 pr/version
        cursor (:cursor state)]
    (run! (fn [[txt ypos size]]
            (hsu/write-centered ctx ypos txt size ))
          [[t1 55 4]
           [t2 145 2.5]
           [o1 200 2][o2 250 2]
           [t3 300 2.5]
           [o3 355 2][o4 405 2] [o5 450 2.5]
           [b1 500 1] [b2 520 1] [b4 575 1]])
    (obj/draw-sprite cursor ctx)))

(defn draw-end-scene [ctx state]
  (let [prev (rfu/<sub [::subs/previous])
        {:keys [ship1 ship2]} (:ships prev)
        winner (cond (and (not (true? ship1)) (not (true? ship2))) "draw"
                      (not ship2) 1
                      (not ship1) 2)
        t1 "GAME OVER"
        s1 (if (= winner "draw") winner
               (if (= (:mode prev) :single)
                 (str  " YOU " (if (== winner 1) "WON " "LOST "))
                 (str " PLAYER " winner " WON ")))
        c1 "play again"
        c2 "menu"
        b4 pr/version
        cursor (:cursor state)]
    (run!
     (fn [[txt ypos size]]
       (hsu/write-centered ctx ypos txt size ))
     [[t1 100 5]
      [s1 200 3]
      [c1 350 2] [c2 400 2]
      [b4 575 1]])
    (obj/draw-sprite cursor ctx)))
