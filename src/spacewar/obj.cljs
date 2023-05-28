(ns spacewar.obj
  {:author "Kyuvi"
   :license {:name "GPL-3.0 WITH Classpath-exception-2.0"
             :url "https://www.gnu.org/licenses/gpl-3.0.html"}}
  (:require [sutils.canvas :as cvu]
            [spacewar.prep :as pr]
            [spacewar.rfm.subs :as subs]
            [sutils.rf :as rfu]
            [sutils.geom :as gm]))

(defprotocol SpriteProtocol
  (get-pos [sprite] "Get position of `sprite` as [x y].")
  (get-core-vals [sprite])
  (reset-sprite [sprite x y]
    [sprite x y rotation xspeed yspeed])
  (respawn-sprite [sprite]
    [sprite xpeeed yspeed angle] )
  (rotate-vector [sprite vect])
  (add-gravity [sprite] [sprite sx sy gravity])
  (update-sprite [sprite] [sprite input])

  (explode-sprite [sprite] [sprite ctx] )
  (draw-sprite [sprite ctx] "Draw  `sprite` to a canvas of context `ctx`.")
  (get-corners [sprite])
  (shot-explode [shot ctx] )
  ;; Ship only
  (get-extremes [ship])
  (get-rear [ship])
  (get-tip [ship])
  (fire-shot [ship])
  (fire-thrusters [ship])
  (update-rotation [ship] [ship angle])
  (evade [ship])
  )


(defrecord SpCore [x y xspeed yspeed center rotation]
  SpriteProtocol
  (get-pos [sp] [(:x sp)(:y sp)])
  (get-core-vals [sp] ((juxt :xspeed :yspeed :center :rotation) sp))
  (rotate-vector [sp vect]
    (let [[cx cy] (:center sp)
          rt (:rotation sp)
          [vx vy] [(- (vect 0) cx) (- (vect 1) cy)]
          x (- (* vx (Math/cos rt)) (* vy (Math/sin rt)))
          y (+ (* vy (Math/cos rt)) (* vx (Math/sin rt)))]
      [x y]))
  (reset-sprite [sp x y]
     (reset-sprite sp x y false false false))
  (reset-sprite
    [sp x y rotation xspeed yspeed]
    (let [[oxspeed oyspeed orot] ((juxt :xspeed :yspeed :rotation) sp)
          nxspeed (if (pr/valid-num? xspeed) xspeed oxspeed)
          nyspeed (if (pr/valid-num? yspeed) yspeed oyspeed)
          nrotation (if (pr/valid-num? rotation) rotation orot) ;; REVIEW: update-rotation
          ]
      (assoc sp :x x :y y :xspeed nxspeed :yspeed nyspeed :rotation nrotation)
      )
    )
  (respawn-sprite
    [sp]
    (respawn-sprite sp false false false))
  (respawn-sprite
    [sp xspeed yspeed angle]
    (let [location (if angle angle (rand gm/tau))
          {:keys [radius width height]}  pr/game-view
          x (+ (* (- radius 10) (Math/cos location)) (/ width 2))
          y (+ (* (- radius 10) (Math/sin location)) (/ height 2))
          rotation (rand gm/tau)
          ]
      (reset-sprite sp x y rotation xspeed yspeed)))
  (update-sprite [sp]
    (let [{:keys [radius width height]}  pr/game-view
          [x y] (get-pos sp)
          {:keys [xspeed yspeed]} sp
          [nx ny] [(+ x xspeed) (+ y yspeed)]
          sprite-to-border (- radius (Math/hypot (- nx (/ width 2))
                                                 (- ny (/ height 2))))]
      ;; REVIEW
      (if (<= sprite-to-border 0) ;; check border collision
        (let [angle (+ Math/PI (Math/atan2 (- y (/ height 2)) (- x (/ width 2))))
              x (+ (* (- radius 10) (Math/cos angle)) (/ width 2))
              y (+ (* (- radius 10) (Math/sin angle)) (/ height 2))]
          (reset-sprite sp x y))
        (assoc sp :x nx :y ny))))
  (add-gravity [sp]
    (add-gravity sp (/ (:width pr/game-view) 2)
                 (/ (:height pr/game-view) 2) pr/gravity-val))
  (add-gravity [sp sx sy gravity-val]
    (let [[x y] (get-pos sp)
        [dx dy] [(- x sx ) (- y sy)]
        F (/ gravity-val (Math/pow (Math/hypot dx dy) 2))
        F-neg (- F)
        angle (Math/atan2 dy dx)
        [fx fy] [(* F-neg (Math/cos angle) ) (* F-neg (Math/sin angle))]
        [xspeed yspeed] ((juxt :xspeed :yspeed ) sp)
        accel (:accel pr/body-vals)]
    (assoc sp :xspeed (+ xspeed (if (< fx accel) fx accel ))
           :yspeed (+ yspeed (if (< fy accel) fy accel ))))))

(defn make-spcore [x y xspeed yspeed center rotation]
  (->SpCore x y xspeed yspeed center rotation))

(defrecord Shot [core fpos direction size distance sound  ]
  SpriteProtocol
  (get-pos [sp] (get-pos (:core sp)))
  (get-core-vals [sp] (get-core-vals (:core sp)))
  (rotate-vector [sp vect] (rotate-vector (:core sp) vect))
  (get-corners [sp]
    (let [[x y] (get-pos sp)
          clist
          (map #(rotate-vector sp %)
               [[-1 3] [1 3] [-1 -3] [1 -3]])]
      (mapv (fn [[nx ny]] [(+ x nx ) (+ y ny)]) clist )))
  (draw-sprite [sp ctx]
    (let [[x y] (get-pos sp)
          [xf yf] (:fpos sp)]
      (cvu/draw-line ctx x y xf yf)))
  (update-sprite [sp]
    (let [tcore (update-sprite (:core sp))
          [x y] (get-pos tcore)
          {:keys [size direction]} sp
          xf (+ x (* (Math/cos direction) size))
          yf (+ y (* (Math/sin direction) size))
          distance (+ (:speed pr/shot-vals) (:distance sp)) ]
      (assoc sp :core tcore :distance distance :fpos [xf yf])))
  (explode-sprite [sp]
    (let [dist (:distance sp)
          ex-dist(- (:distance pr/shot-vals) 10 )]
      (if (< dist ex-dist)
        (assoc sp :distance ex-dist)
        sp))))

(defn make-shot [x y direction sound]
  (->Shot (make-spcore x y
                        (* (Math/cos direction) (:speed pr/shot-vals))
                        (* (Math/sin direction) (:speed pr/shot-vals))
                        [0 0]
                        direction)
          [(+ x 5) (+ y 5)] direction (:size pr/shot-vals) 0 sound))


(defrecord Ship [core keys shape show-shape
                 ext size shots shot-timeout sound active]
  SpriteProtocol
  (get-pos [sp] (get-pos (:core sp)))
  (get-core-vals [sp] (get-core-vals (:core sp)))
  (rotate-vector [sp vect] (rotate-vector (:core sp) vect))
  (get-extremes [sp]
    ((juxt :left :right :top :bottom) (:ext sp)))
  (get-rear [sp]
    (let [[x y] (get-pos sp)
          [xc yc] (get-in sp [:core :center])
          [left _ top bottom] (get-extremes sp)
          size (:size sp)
          [rx ry] (rotate-vector sp [(+ (* left size) xc)
                                     (+ (/ (+ top bottom) 2) (* size yc))])]
      [(+ rx x) (+ ry y)]))
  (get-tip [sp]
    (let [[x y] (get-pos sp)
          [xc yc] (get-in sp [:core :center])
          [_ right top bottom] (get-extremes sp)
          size (:size sp)
          [rx ry] (rotate-vector sp [(+ (* right size) xc)
                                     (+ (/ (+ top bottom) 2) (* size yc))])]
      [(+ rx x) (+ ry y)]))
  (get-corners [sp]
    (let [[x y] (get-pos sp)
          [xc yc] (get-in sp [:core :center])
          [left right top bottom] (get-extremes sp)
          clist (mapv #(rotate-vector sp %)
                      [[left top] [right top] [left bottom] [right bottom]])
        ]
      (mapv (fn [[nx ny]] [(+ x nx ) (+ y ny)]) clist)))
  (draw-sprite [shp ctx]
    (let [[x y] (get-pos shp)
          {:keys [size thrusters shots active show-shape]} shp
          ]
    ;; draw ship
    (doseq [line show-shape]
      (cvu/draw-seq ctx (mapv (fn [[xp yp]] [(+ x (* size xp))
                                             (+ y (* size yp))]) line) ))
    ;; draw thruster fire
    (when (and thrusters active)
      (let [[xb yb] (get-rear shp)
            rotation (:rotation (:core shp))
            fire-length (* size (:length pr/thruster-vals) (Math/random))
            fire-array [[xb yb] [(- xb (* fire-length (Math/cos rotation)))
                                 (- yb (* fire-length (Math/sin rotation)))]]]
        (cvu/draw-seq ctx fire-array )))
    ;; draw shots
    (run! #(draw-sprite % ctx) shots)
    ))
  (reset-sprite [shp x y] ) ;; TODO:
  (update-sprite [shp]
    (let [tcore (update-sprite (:core shp))
          [x y] (get-pos tcore)
          t-rotation (:rotation tcore)
          remove-shots-fn (fn [shots]
                          (mapv #(update-sprite %)
                                (remove #(> (:distance %)
                                            (:distance pr/shot-vals))
                                        shots))
                            )
          [gx gy g-size] ((juxt :x :y :size) (rfu/<sub [::subs/gwell]))
          gwell-proximity (Math/hypot (- x gx) (- y gy))
          ncore (if (<= gwell-proximity g-size)
                  (respawn-sprite tcore 0 0 false)
                    tcore)
          active (:active shp)
          {:keys [rotation xspeed yspeed]} ncore
          ]
      (cond (true? active)
        (let [pressed-keys (rfu/<sub [::subs/pressed])
              {:keys [thrust fire left right]} (:keys shp)
              ;; shots fired
              [n-shot-time nshots] (if (pressed-keys fire)
                                     (fire-shot shp)
                                     ((juxt :shot-timeout :shots) shp))
              ;; turn ship
              n-rotation (if (some pressed-keys [left right])
                           (let [rot-speed (:rotation pr/body-vals)]
                             (rem ((if (pressed-keys right)
                                     + -) rotation rot-speed)
                                  gm/tau))
                           ;; (do (println "rot " pressed-keys )
                               ;; REVIEW: getting here when dir keys pressed
                               rotation
                               ;; )
                           )
              show-shape (if-not (= n-rotation t-rotation)
                           (pr/rotate-shape (:shape shp) (:center ncore) n-rotation)
                          (:show-shape shp))
              ;; fire-thrusters
              [nthrust txsp tysp] (if (pressed-keys thrust)
                                    (fire-thrusters shp)
                                    [false xspeed yspeed])
              ;; regulate speed
              [nxsp nysp] (pr/regulate-speed txsp tysp)]
          (assoc shp :core (assoc ncore :xspeed nxsp :yspeed nysp
                                  :rotation n-rotation)
                 :thrusters nthrust :shots (remove-shots-fn nshots)
                 :shot-timeout n-shot-time
                 :show-shape show-shape))
        ;; exploding ship
        (number? active)
        (let [nactive (inc active)
              extremes (get-extremes shp)
              sp-radius (apply max (map #(Math/abs %) extremes))
              blast-shape (condp > active
                             17  (pr/fill-explosion sp-radius pr/blast-size)
                             34 (pr/fill-explosion (* 2 sp-radius)
                                                pr/blast-size)
                             51 (pr/fill-explosion (* 5 sp-radius)
                                                pr/blast-size)
                             68 (pr/fill-explosion sp-radius pr/blast-size)
                             75 [])]
          (assoc shp :thrusters false :shots (remove-shots-fn shots)
                 :show-shape blast-shape
                 :active (if (>= active 70) false nactive)))
        ;; explosion finished
        (false? active)
        (assoc shp :thrusters false :shots (remove-shots-fn shots)
               ))))
  (add-gravity [shp]
    (add-gravity shp (/ (:width pr/game-view) 2)
                 (/ (:height pr/game-view) 2) pr/gravity-val))
  (add-gravity [shp sx sy gravity-val]
    (assoc shp :core (add-gravity (:core shp) sx sy gravity-val)))
  (fire-shot [shp]
    (let [now (js/Date.now)
          {:keys [shot-timeout shots]} shp
          ushots (mapv #(update-sprite %) shots) ]
      (if (>= now shot-timeout)
        (let [[xt yt] (get-tip shp)
              rotation (get-in shp [:core :rotation])
              shot-sound-fn (:sound shp)
              ]
          (shot-sound-fn)
          [(+ now (:interval pr/shot-vals))
           (conj shots (make-shot xt yt rotation sound))])
        [shot-timeout shots])))
  (fire-thrusters [shp]
    (let [{:keys [rotation xspeed yspeed]} (:core shp)
          thrust-speed (:speed pr/thruster-vals)
          t-xspeed (+ xspeed (* thrust-speed (Math/cos rotation)))
          t-yspeed (+ yspeed (* thrust-speed (Math/sin rotation)))]
      (pr/play-thrusters)
      [true t-xspeed t-yspeed]
      ))
  (update-rotation [shp angle]
    (when (pr/valid-num? angle)
      (let [shape (:shape shp)
            center (get-in shp [:core :center])
            ncore (assoc (:core shp) :rotation angle)
            show-shape (pr/rotate-shape shape center angle)
            ]
        (assoc shp :show-shape show-shape :core ncore)
        )))
  (explode-sprite [shp]
    (if (true? (:active shp))
      (do (pr/play-explosion)
          (assoc shp :active 0))
      shp)

    )
  (evade [shp]) ;; TODO:
  )

(defn make-ship [x y keys shape size rotation sound active]
  (let [shape-list (apply concat shape)
        x-list (map first shape-list)
        y-list (map second shape-list)
        [left right] [(apply min x-list) (apply max x-list)]
        [top bottom] [(apply min y-list) (apply max y-list)]
        [xc yc] [(/ (+ left right) 2) (/ (+ top bottom) 2)]
        spcore (make-spcore x y 0 0 [xc yc] rotation)]
    (->Ship spcore
          keys shape
          (pr/rotate-shape shape [xc yc] rotation)
          {:left left :right right :top top :bottom bottom}
          size [] (js/Date.now) sound active)))

(defrecord ShipCursor [ship pos-xs timeout current ]
  SpriteProtocol
  (get-pos [sp] (get-pos (:ship sp)))
  (draw-sprite [sp ctx] (draw-sprite (:ship sp) ctx))
  (update-sprite [sp]
    (if (> (js/Date.now) (:timeout sp))
      (when (or (.-isDown 38))
        (pr/play-thrusters)
        (assoc sp :current (dec (:current sp)) :timeout (+ (js/Date.now) 200)))))
 (update-sprite [sp dir]
   (if (> (js/Date.now) (:timeout sp))
     (let [ {:keys [ship pos-xs current]} sp
           pos-len (count pos-xs)
           tcurr (case dir
                  :up (dec current)
                  :down (inc current)
                  :else current)
           ncurr (cond (>= tcurr pos-len ) 0
                       (< tcurr 0) (dec pos-len)
                       :else tcurr)
           ntime (+ (js/Date.now) 200)
           [nx ny] (pos-xs ncurr)]
      (pr/play-thrusters)
       (assoc sp :ship (assoc ship :core (assoc (:core ship) :x nx :y ny ))
              :timeout ntime
              :current ncurr))
     sp)))

(defn make-ship-cursor [pos-xs shape size]
  (let [[x y] (first pos-xs)]
  (->ShipCursor (make-ship x y {} shape size 0 nil false)
                pos-xs (+ (js/Date.now) 200) 0)))

(defrecord GravityWell [x y size]
  SpriteProtocol
  (get-pos [sp] [(:x sp) (:y sp)])
  (draw-sprite [sp ctx]
    (let [{:keys [x y size]} sp
          vsize (rand size)
          angle (rand gm/tau)
          sx (+ x (* vsize (Math/cos angle)))
          sy (+ y (* vsize (Math/sin angle)))
          ex (- x (* vsize (Math/cos angle)))
          ey (- y (* vsize (Math/sin angle)))]
      (cvu/draw-line ctx sx sy ex ey)))
  ;; (update-sprite [sp])
  )

(defn make-gravity-well
  ([]
   (let [{:keys [width height]} pr/game-view]
     (make-gravity-well (/ width 2) (/ height 2) pr/gravity-well-size)))
  ([x y size]
  (->GravityWell x y size)))
