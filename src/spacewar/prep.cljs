(ns spacewar.prep
  {:author "Kyuvi"
   :license {:name "GPL-3.0 WITH Classpath-exception-2.0"
             :url "https://www.gnu.org/licenses/gpl-3.0.html"}}
  (:require
   [sutils.canvas :as cvu]
   [sutils.geom :as gmu]
   ))

        ;;;; game constant values ;;;;

(def version "v0.01")

;; (def game-map {:fps 60
;;                :dimensions {:width 550 :height 550 :radius 275}
;;                :modes  #{:menu :single :versus :end :options :controls :credits}
;;                })

(def game-view {:fps 60 :width 604 :height 604 :radius 302})
;; (def game-vals {:fps 60 :width 550 :height 550 :radius 275})

(def game-modes "Set of game modes"
  #{:menu :single :versus :end :options :controls :credits})
                                        ;
(def score-view {:width 100 :height 40})

(def vector-color "#0F0")

(def gravity-wells #{:blackhole :wormhole :pulsar}) ;; TODO: vector? random from set?

(def body-vals {:rotation (* 4 (/ Math/PI 180)), :speed 1, :accel 1})
(def shot-vals {:distance 250, :speed 2, :size 5, :interval 300})

(def thruster-vals {:speed 0.01 :length 10})

(def ^:consr blast-size 50)

(def ^:const gravity-well-size 12 )
(def ^:const gravity-val 50 )

(def p1spawn [150 150])

(def p2spawn [450 450])

(def ship1 [[[8 0] [1 2] [-1 2] [-8 1] [-8 -1] [-1 -2] [1 -2] [8 0]]
            [[-1  2] [-6  4] [-8  4] [-5  1.5]]
            [[-1 -2] [-6 -4] [-8 -4] [-5 -1.5]]])

(def ship2 [[[8 0] [1 2] [-8 2] [-8 -2] [1 -2] [8 0]]
            [[-1  2] [-6  4] [-8  4] [-8  2]]
            [[-1 -2] [-6 -4] [-8 -4] [-8 -2]]
            [[8 0] [-8 0]]])


(def cursor-pos {:menu [[190 250] [190 300] [200 350] [200 400]]
                 :options []
                 :controls [[80 210] [365 210] [80 260] [345 260] [80 365]
                            [365 365] [80 415] [345 415] [225 460]]
                 :end [[180 360] [230 410]]})

(def ^:const star-count 40)

(defn make-star-list
  ([] (make-star-list star-count))
  ([qty]
   (let [xc (/ (:width game-view) 2)
         yc (/ (:height game-view) 2)
         rand-star-pos (fn []
                         (let [dist (rand (:radius game-view))
                               theta (rand (* 2 Math/PI))]
                           [(+ (* dist (Math/cos theta)) xc)
                            (+ (* dist (Math/sin theta)) yc)]))
         star-list (repeatedly qty rand-star-pos)]
     star-list)))

(defn rotate-vector [pos-vec center-vec angle]
  (let [[cx cy] center-vec
        [tx ty] [(- (pos-vec 0) cx) (- (pos-vec 1) cy)]
        x (- (* tx (Math/cos angle)) (* ty (Math/sin angle)))
        y (+ (* ty (Math/cos angle)) (* tx (Math/sin angle)))]
    [x y]))

(defn rotate-shape [shape center-vec angle]
  (mapv (fn [v]
          (mapv (fn [vect]
                  (rotate-vector vect center-vec angle))
                v))
        shape))


        ;;;; game drawing functions ;;;;

(defn draw-stars
  ([ctx star-list] (draw-stars ctx star-list vector-color))
  ([ctx star-list color]
   (let [old-line-width (.-lineWidth ctx)]
     (doseq [[x-pos y-pos] star-list]
       ;; (cvu/draw-point ctx (first pos) (second pos) :width 1 :color color))))
       (cvu/draw-point ctx x-pos y-pos :width 1 :color color))
     ;; reset line width which draw-point increases by 1
     (set! (.-lineWidth ctx) old-line-width))))


(defn draw-border
  ([ctx] (draw-border ctx game-view vector-color))
  ([ctx screen-map color]
   (let [{:keys [width height radius]} screen-map
         [xc yc] [(/ width 2) (/ height 2)]]
     (cvu/draw-circle ctx xc yc radius :width 1 :color color))
   ))


        ;;;; game sounds ;;;;

(defn make-audio-element
  "Make an new audio element(object) from file of filename `sound-file-name`
   in the ./audio folder located in same folder as this projects html file."
  [sound-file-name]
  ;; (let [sound-folder  "./resources/public/audio/"]
  (let [sound-folder  "./audio/"] ;; start path from (index.)html file
    (new js/Audio (str sound-folder sound-file-name))))

;; (def gsounds
;;   ;; (let [sound-folder  "resources/public/audio/"]
;;   (let [sound-folder  "./audio/"]
;;   {:explosion (str/join sound-folder "334266__projectsu012__short-explosion-1.wav")
;;    :laser1   (str/join sound-folder "344511__jeremysykes__laser03.wav")
;;    :laser2   (str/join sound-folder "268168__shaun105__laser.wav")
;;    :thrusters  (str/join sound-folder "238283__meroleroman7__8-bit-noise.wav")
;;    }))

;; (def explosion-sound (new js/Audio (:explosion gsounds) ))
;; (def laser-1-sound (new js/Audio (:laser1 gsounds) ))
;; (def laser-2-sound (new js/Audio (:laser2 gsounds) ))
;; (def thruster-sound (new js/Audio (:thrusters gsounds) ))

(def explosion-sound (make-audio-element "334266__projectsu012__short-explosion-1.wav"))
(def laser-1-sound (make-audio-element "344511__jeremysykes__laser03.wav"))
(def laser-2-sound (make-audio-element "268168__shaun105__laser.wav"))
(def thruster-sound (make-audio-element "238283__meroleroman7__8-bit-noise.wav"))

(defn sound-factory [audio start stop]
  (if (.-paused audio)
    (.play audio)
    (js/setTimeout (fn [] (.pause audio)
                     (set! (.-currentTime audio) start )) stop)))

(defn play-explosion [] (sound-factory explosion-sound 0 300))
(defn play-laser-1 [] (sound-factory laser-1-sound 0 300))
(defn play-laser-2 [] (sound-factory laser-2-sound 0 300))
(defn play-thrusters [] (sound-factory thruster-sound 100 350))

        ;;;; miscellenious functions ;;;;

(defn check-num [n] ;; TODO: change to valid-num?
  (and (number? n) (not (.isNaN js/Number n)) (js/isFinite n) ))


(defn fill-explosion
  "Fill an area defined by `radius` with `qty` 'particles' of debris."
  [radius qty]
  (repeatedly qty
              (fn []
                (let [theta (rand gmu/tau)
                      r (rand radius)
                      [x y] [(* r (Math/cos theta)) (* r (Math/sin theta))]]
                  [[x y] [(inc x) (inc y)]]))))

(defn regulate-speed
  "Ensure `xspeed` and `yspeed` are within acceptable limits
  (defined in 'spacewar.prep/body-vals'). "
  [xspeed yspeed]
  (let [max-speed (:speed body-vals)
        cur-speed (Math/hypot xspeed yspeed)]
    (if (> cur-speed max-speed)
      [(* xspeed (/ max-speed cur-speed)) (* yspeed (/ max-speed cur-speed))]
      [xspeed yspeed])
    ))
