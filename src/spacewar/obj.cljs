(ns spacewar.obj
  {:author "Kyuvi"
   :license {:name "GPL-3.0 WITH Classpath-exception-2.0"
             :url "https://www.gnu.org/licenses/gpl-3.0.html"}}
  (:require [sutils.canvas :as cvu]
            [spacewar.prep :as pr]
            [spacewar.rfm.subs :as subs]

            [sutils.rf :as rfu]
            [sutils.geom :as gm]
            ))

(defprotocol SpriteProtocol
  (get-pos [sprite] "Get position of `sprite` as [x y].") ;; TODO: remove spos use pos
  (get-core-vals [sprite])
  ;; (reset-sprite [sprite x y & {:keys [rotation xspeed yspeed]
                        ;; :or {rotation 1 xspeed false yspeed false}}])
  (reset-sprite [sprite x y]
    [sprite x y rotation xspeed yspeed])
  ;; (respawn-sprite [sprite & {:keys [xspeed yspeed angle]
  ;;                   :or {xspeed false yspeed false angle false} }])
  (respawn-sprite [sprite]
    [sprite xpeeed yspeed angle] )
  (rotate-vector [sprite vect]
    )
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
  ;; (fill-explosion [ship radius debris ])
  (evade [ship])
  )

;; (defrecord SPos [x y]
;;   SpriteProtocol
;;   (get-pos [sp] [(:x sp) (:y sp)]))

;; (defn make-spos [x y]
;;           (->SPos x y))

(defrecord Score [p1-vals p2-vals]
  SpriteProtocol
  (draw-sprite [sp ctx])
  (update-sprite [sp kii])
  )

(defn make-sprite [p1-map p2-map]
  (Score. p1-map p2-map))


;; (defrecord SpCore [pos xspeed yspeed center rotation]
(defrecord SpCore [x y xspeed yspeed center rotation]
  SpriteProtocol
  ;; (get-pos [sp] (get-pos (:pos sp)))
  (get-pos [sp] [(:x sp)(:y sp)])
  (get-core-vals [sp] ((juxt :xspeed :yspeed :center :rotation) sp))
  (rotate-vector [sp vect]
    (let [[cx cy] (:center sp)
          rt (:rotation sp)
          [vx vy] [(- (vect 0) cx) (- (vect 1) cy)]
          x (- (* vx (Math/cos rt)) (* vy (Math/sin rt)))
          y (+ (* vy (Math/cos rt)) (* vx (Math/sin rt)))]
      [x y]
      ))
  (reset-sprite
    [sp x y ]
     (reset-sprite sp x y false false false)
     )
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
                                                 (- ny (/ height 2))))
          ]
      ;; REVIEW
      (if (<= sprite-to-border 0) ;; check border collision
        (let [angle (+ Math/PI (Math/atan2 (- y (/ height 2)) (- x (/ width 2))))
              x (+ (* (- radius 10) (Math/cos angle)) (/ width 2))
              y (+ (* (- radius 10) (Math/sin angle)) (/ height 2))
              ]
          (reset-sprite sp x y))
        (assoc sp :x nx :y ny)
        )))
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
          ;; [lt rt lb rs]
          clist
          (map #(rotate-vector sp %)
               [[-1 3] [1 3] [-1 -3] [1 -3]])]
      (mapv (fn [[nx ny]] [(+ x nx ) (+ y ny)]) clist )))
  (draw-sprite [sp ctx]
    (let [[x y] (get-pos sp)
          [xf yf] (:fpos sp)]
      (cvu/draw-line ctx x y xf yf)
      ;; move to update
      ;; (when (<= (- (:distance pr/shot-vals) (:distance sp) ) 20 )
        ;; (shot-explode sp ctx))
        ))
  ;; (reset-sprite [sp x y] ) ;; TODO: done with (update-sprite (:core sp))
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
        sp)))
  (shot-explode [sp ctx]
    (let [[x y] (get-pos sp)
          [xf yf] (:fpos sp)
          direction (:directon sp)
          size (:size sp)
          [rcos rsin] [(Math/cos (/ Math/PI 2)) (Math/sin (/ Math/PI 2))]
          xc (* (/ size 2) (+ x (Math/cos direction)))
          yc (* (/ size 2) (+ x (Math/sin direction)))
          x0 (- x xc)
          y0 (- y yc)
          xr0 (+ (- (* x0 rcos ) (* y0 rsin) ) xc)
          yr0 (+ (* y0 rcos ) (* x0 rsin) yc)
          [x1 y1] [(- xf xc) (- yf yc)]
          xr1 (+ (- (* x1 rcos) (* y1 rsin)) xc)
          yr1 (+ (* y1 rcos) (* x1 rsin) yc)
          ]
      (cvu/draw-line ctx xr0 yr0 xr1 yr1)
      (assoc sp :size (- size 0.5)))
    ))

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
          ;; size (:size sp)
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
        (cvu/draw-seq ctx fire-array ))) ;;:width 1
    ;; draw shots
    (run! #(draw-sprite % ctx) shots)
    ))
  (reset-sprite [shp x y] ) ;; TODO:
  (update-sprite [shp]
    (let [tcore (update-sprite (:core shp))
          [x y] (get-pos tcore)
          t-rotation (:rotation tcore)
          ;; {:keys [shots active]} shp
          remove-shots-fn (fn [shots]
                            ;; (loop [rem-shots #{} rshots shots ]
                            ;;   (when (= (:distance (first shots))
                            ;;            (:distnance pr/shot-vals))
                            ;;     (conj rem-shots (first shots))
                            ;;     (update-sprite (first)))
                            ;; (mapv (fn )))
                          (mapv #(update-sprite %) ;; TODO: move to fire shots
                                (remove #(> (:distance %)
                                            (:distance pr/shot-vals))
                                        shots))
                            ;; (remove #(> (:distance %) (:distance pr/shot-vals))
                            ;;         shots)
                            )
          ;; TODO: gravity well collision
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
              [n-shot-time nshots] (if
                                       (pressed-keys fire) ;; cljs sets
                                      ;; (.has pressed-keys fire) ;; js/Sets
                                         (fire-shot shp)
                                     ;; ((juxt :shot-timeout :shots) shp)
                                         ((juxt :shot-timeout :shots) shp)
                                         ;; [(:shot-timeout shp)
                                         ;;  (mapv #(update-sprite %) (:shots shp))]
                                         )
              ;; turn ship
              ;; rotation (:rotation ncore)
              n-rotation (if
                             ;; (seq (filter pressed-keys [left right]))
                             (some pressed-keys [left right]) ;; cljs sets
                           ;; js/Sets
                             ;; (or (.has pressed-keys left) (.has pressed-keys right))
                           (let [rot-speed (:rotation pr/body-vals)]
                             ;; (mod (+ rotation (if (pressed-keys right)
                             ;;                    rot-speed
                             ;;                    (- rot-speed)))
                             (rem ((if
                                       (pressed-keys right) ;; cljs sets
                                     ;; (.has pressed-keys right) ;; js/Sets
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
              [nthrust txsp tysp] (if
                                      (pressed-keys thrust) ;; cljs sets
                                      ;; (.has pressed-keys thrust) ;; js/Sets
                                    (fire-thrusters shp)
                                    ;; [false xspeed yspeed]
                                    [false xspeed yspeed])
              ;; regulate speed
              ;; max-speed (:speed pr/body-vals)
              ;; speed (Math/hypot txsp tysp)
              ;; [nxsp nysp] (if (> speed max-speed)
              ;;                   [(* txsp (/ max-speed speed))
              ;;                    (* tysp (/ max-speed speed))]
              ;;                   [txsp tysp])
              [nxsp nysp] (pr/regulate-speed txsp tysp)
              ]
          ;; (println n-rotation rotation)
          (assoc shp :core (assoc ncore :xspeed nxsp :yspeed nysp
                                  :rotation n-rotation)
                 :thrusters nthrust :shots (remove-shots-fn nshots)
                 :shot-timeout n-shot-time
                 :show-shape show-shape)
          )
        ;; exploding ship
        (number? active)
        (let [nactive (inc active)
              extremes (get-extremes shp)
              sp-radius (apply max (map #(Math/abs %) extremes))
              blast-shape
                           ;; (case active
                           (condp > active
                             17  (pr/fill-explosion sp-radius pr/blast-size)
                             34 (pr/fill-explosion (* 2 sp-radius)
                                                pr/blast-size)
                             51 (pr/fill-explosion (* 5 sp-radius)
                                                pr/blast-size)
                             68 (pr/fill-explosion sp-radius pr/blast-size)
                             75 [])]
          (assoc shp  ; :core ncore
                 :thrusters false :shots (remove-shots-fn shots)
               :show-shape blast-shape
               :active (if (>= active 70) false nactive)
               ))
        ;; explasion finished
        (false? active)
        (assoc shp ;;:core ncore
               :thrusters false :shots (remove-shots-fn shots)
               ;; :show-shape []; (if-not (= rotation t-rotation)
                                        ;(pr/rotate-shape (:shape shp) (:center ncore) rotation)
                                ;(:show-shape shp))
               ))))
  (add-gravity [shp]
    (add-gravity shp (/ (:width pr/game-view) 2)
                 (/ (:height pr/game-view) 2) pr/gravity-val))
  (add-gravity [shp sx sy gravity-val]
    (assoc shp :core (add-gravity (:core shp) sx sy gravity-val)))
  (fire-shot [shp]
    ;; (println "fire")
    (let [now (js/Date.now)
          {:keys [shot-timeout shots]} shp
          ushots (mapv #(update-sprite %) shots) ]
      (if (>= now shot-timeout)
        (let [[xt yt] (get-tip shp)
              rotation (get-in shp [:core :rotation])
              shot-sound-fn (:sound shp)
              ]
          (shot-sound-fn)
        ;; (assoc shp :shot-timeout (+ now (:interval pr/shot-vals))
               ;; :shot (conj shots (make-shot xt yt rotation sound)))
          [(+ now (:interval pr/shot-vals))
           (conj shots (make-shot xt yt rotation sound))]
          )
        [shot-timeout shots]
        )))
  (fire-thrusters [shp]
    (let [{:keys [rotation xspeed yspeed]} (:core shp)
          thrust-speed (:speed pr/thruster-vals)
          t-xspeed (+ xspeed (* thrust-speed (Math/cos rotation)))
          t-yspeed (+ yspeed (* thrust-speed (Math/sin rotation)))
          ;; speed (Math/hypot t-xspeed t-yspeed)
          ;; max-speed (:speed pr/body-vals)
          ;; [n-xspeed n-yspeed] (if (> speed max-speed)
          ;;                       [(* t-xspeed (/ max-speed speed))
          ;;                        (* t-yspeed (/ max-speed speed))]
          ;;                       [t-xspeed t-yspeed])
          ]
      (pr/play-thrusters)
      [true t-xspeed t-yspeed]
      ;; (assoc shp :thrusters true :xspeed xspeed :yspeed yspeed)
      ))
  (update-rotation [shp angle]
    (when (pr/valid-num? angle)
      (let [shape (:shape shp)
            center (get-in shp [:core :center])
            ncore (assoc (:core shp) :rotation angle)
            ;; show-shape (mapv
            ;;             (fn [v] (mapv ;; REVIEW: which rotation
            ;;                      (fn [vect] (rotate-vector ncore vect)) v))
            ;;             shape)
            show-shape (pr/rotate-shape shape center angle)
            ]
        ;; (assoc-in shp [:rotation :core] angle)
        (assoc shp :show-shape show-shape :core ncore)
        ))) ;; TODO:
  ;; (explode-sprite [sp ctx]
  ;;   (let [active (:active sp)]
  ;;     (when active
  ;;       (let [extremes (get-extremes sp)
  ;;             ;; [left right top bottom] (get-extremes sp)
  ;;             sprite-radius (apply max (map #(Math/abs %) extremes))
  ;;             blast0 (fill-explosion sp sprite-radius pr/blast-size )
  ;;             blast1 (fill-explosion sp (* 2 sprite-radius) pr/blast-size )
  ;;             blast2 (fill-explosion sp (* 5 sprite-radius) pr/blast-size )
  ;;             blast3 (fill-explosion sp sprite-radius pr/blast-size )
  ;;             empty []
  ;;             ]
  ;;         (pr/play-explosion)
  ;;         (draw-sprite (assoc sp :show-shape blast0) ctx)
  ;;         (js/setTimeout (draw-sprite (assoc sp :show-shape blast1) ctx) 60)
  ;;         (js/setTimeout (draw-sprite (assoc sp :show-shape blast2) ctx) 120)
  ;;         (js/setTimeout (draw-sprite (assoc sp :show-shape blast3) ctx) 180)
  ;;         (js/setTimeout (draw-sprite (assoc sp :show-shape empty) ctx) 240)
  ;;         (assoc sp :active false)
  ;;        )))) ;; TODO:
  (explode-sprite [shp]
    (if (true? (:active shp))
      (do (pr/play-explosion)
          (assoc shp :active 0))
      shp)

    )
  ;; (fill-explosion [shp radius debris] ;; move out of record?
  ;;   ;; (let [theta (rand gm/tau);;  (* 2 Math/PI (Math/random) )
  ;;   ;;       r (* radius (Math/random))
  ;;   ;;       [x y] [(* r (Math/cos theta)) (* r (Math/sin theta))]]
  ;;   (loop [arr [] debris-count debris]
  ;;     (if (< (count arr) debris-count)
  ;;       arr
  ;;       (let [theta (rand gm/tau);;  (* 2 Math/PI (Math/random) )
  ;;             r (rand radius)
  ;;             [x y] [(* r (Math/cos theta)) (* r (Math/sin theta))]]
  ;;         (recur (conj arr [[x y] [(inc x) (inc y)]]) (dec debris-count)) ))))
  ;; )
  (evade [shp])

  )

(defn make-ship [x y keys shape size rotation sound active]
  (let [shape-list (apply concat shape)
        x-list (map first shape-list)
        y-list (map second shape-list)
        [left right] [(apply min x-list) (apply max x-list)]
        [top bottom] [(apply min y-list) (apply max y-list)]
        [xc yc] [(/ (+ left right) 2) (/ (+ top bottom) 2)]
        spcore (make-spcore x y 0 0 [xc yc] rotation)]
    (->Ship spcore ;;  (if active (add-gravity spcore) spcore)
     ;; (make-spcore x y 0 0 [xc yc] rotation)
          keys shape
          ;; REVIEW: write show-shape function
          ;; (mapv (fn [v] (mapv (fn [[sx sy]] [(- sx xc) (- sy yc)]) v)) shape)
          ;; (mapv #(mapv (fn [[x y]] [(- x xc) (- y yc)]) %) shape)
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
        (assoc sp :current (dec (:current sp)) :timeout (+ (js/Date.now) 200)))
      ))
 (update-sprite [sp dir]
   (if (> (js/Date.now) (:timeout sp)) ;; timeout not needed with this function?
     (let [ {:keys [ship pos-xs current]} sp
           pos-len (count pos-xs)
           tcurr (case dir
                  :up (dec current);; add play sound
                  :down (inc current) ;; add play sound
                  :else current)
           ncurr (cond (>= tcurr pos-len ) 0
                       (< tcurr 0) (dec pos-len)
                       :else tcurr)
           ntime (+ (js/Date.now) 200)
           [nx ny] (pos-xs ncurr)
           ]
      (pr/play-thrusters)
       (assoc sp :ship (assoc ship :core (assoc (:core ship) :x nx :y ny ))
              :timeout ntime
              :current ncurr
              ))
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
          ey (- y (* vsize (Math/sin angle)))
          ]
      (cvu/draw-line ctx sx sy ex ey)
      ))
  (update-sprite [sp])
  )

(defn make-gravity-well
  ([]
   (let [{:keys [width height]} pr/game-view]
     (make-gravity-well (/ width 2) (/ height 2) pr/gravity-well-size)))
  ([x y size]
  (->GravityWell x y size)))
