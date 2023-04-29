(ns spacewar.kone
  "Main engine of spacewar! game."
  (:require
   [sutils.geom :as gm]
   [sutils.rf :as rfu]
   [spacewar.prep :as pr]
   [spacewar.obj :as obj]

   [spacewar.rfm.subs :as subs]
   ))


        ;;;; game state  ;;;;

(defn initialize-state
  ([mode] (initialize-state mode nil))
  ([mode previous-state]
   (assert (pr/game-modes mode)
           (str mode " not in set " pr/game-modes "."))
   (let [settings (if previous-state
                    (:settings previous-state)
                    {:rounds 5
                     :gravity-well :blackhole
                     :controls {
                                :p1 {:thrust "a" :fire "d" :left "s" :right "t"}
                                :p2 {:thrust "o" :fire "i" :left "n" :right "e"}}})]
     {:state
      {:mode mode
       :settings settings
       :star-list (pr/make-star-list)
       :cursor (when (#{:menu :options :controls :end} mode)
                 (let [cursor-ship ([pr/ship1 pr/ship2] (rand-int 2)) ]
                   (obj/make-ship-cursor (pr/cursor-pos mode) cursor-ship
                                         (if (= mode :controls) 2.5 3))))
       :scene (when (#{:single :versus} mode)
                (let [[p1x p1y] pr/p1spawn
                      [p2x p2y] pr/p2spawn
                      controls (:controls settings)
                      [controls-one controls-two] ((juxt :p1 :p2) controls)]
                  {
                   :ship1 (obj/make-ship p1x p1y controls-one pr/ship1 1.5
                             (/ Math/PI 4) pr/play-laser-1 true)
                   :ship2 (obj/make-ship p2x p2y controls-two pr/ship2 1.5
                             (/ (* -3 Math/PI) 4) pr/play-laser-2 true)
                   :gravity-well (obj/make-gravity-well)
                   :score {}
                   :paused false}))}
      :previous (when previous-state
                  {:mode (:mode previous-state)
                   :score (get-in previous-state [:scene :score])
                   :ships {:ship1 (get-in previous-state
                                          [:scene :ship1 :active])
                           :ship2 (get-in previous-state
                                          [:scene :ship2 :active])}})
      :key-input {:pressed
                    (transient #{})
                    ;; #{}

                  :single #{}}} )))


        ;;;; game functions ;;;;
(defn switch-mode
  ([db mode-opts]
   (switch-mode db mode-opts false))
   ([db mode-opts silent-pred]
  (when-not silent-pred
    (if (= mode-opts :menu) (pr/play-laser-2) (pr/play-laser-1)))
  (let [cur-state (:state db)
        current-pos (get-in cur-state [:cursor :current])
        temp-mode (if (vector? mode-opts) (mode-opts current-pos) mode-opts)
        new-mode (if (= :previous temp-mode)
                   (rfu/<sub [::subs/previous-mode]) temp-mode)
        {:keys [state previous]} (initialize-state new-mode cur-state)]
  (assoc db :state state :previous previous)
  )))

(defn move-cursor [db dir]
  (update-in db [:state :cursor] #(obj/update-sprite % dir)))

(defn handle-options-enter [db])

        ;;;; controls scene functions ;;;;

(defn invalid-key-string-alert []
  (js/alert (str "only one of \n"
                 "a-z, A-Z, 0-9\n"
                 "or\n"
                 "'up', 'down', 'left' or 'right'\n"
                 "allowed as control key strings.")))

(defn process-key-string [s]
  (let [alpha (range 97 (+ 97 26))
        nums (range 48 (+ 48 10))
        an-list (map String/fromCharCode (into alpha nums))
        valid-keyset  (into #{"up" "down" "left" "right"} an-list)
        down-string (.toLowerCase s)
        valid-string (valid-keyset down-string)]
    (if valid-string
      (case valid-string
        "up" "ArrowUp"
        "down" "ArrowDown"
        "left" "ArrowLeft"
        "right" "ArrowRight"
        valid-string)
      (invalid-key-string-alert))))

(defn set-control-key [db vect]
  (let [player (if (= (vect 0) :p1) "1" "2")
        action (name (vect 1))
        print-action (if (= action "thrust") "rockets" action)
        prompt-string (str "input player " player " " print-action " key" )
        temp-key (js/prompt prompt-string)
        new-key (process-key-string temp-key)]
    (if new-key
      (let [full-vec (into [:state :settings :controls] vect)]
        (assoc-in db full-vec new-key))
        db)))

(defn handle-controls-enter [db]
  (let [current-pos (get-in db [:state :cursor :current])]
    (case current-pos
      0 (set-control-key db [:p1 :thrust])
      1 (set-control-key db [:p1 :fire])
      2 (set-control-key db [:p1 :left])
      3 (set-control-key db [:p1 :right])

      4 (set-control-key db [:p2 :thrust])
      5 (set-control-key db [:p2 :fire])
      6 (set-control-key db [:p2 :left])
      7 (set-control-key db [:p2 :right])

      8 (switch-mode db :menu)
      ))
  )

(defn remove-pressed-key [db key]
  ;; (update-in db [:key-input :pressed] #(disj % key)))  ;; sets
  (update-in db [:key-input :pressed] #(disj! % key))) ;; for transient sets

(defn add-pressed-key [db key]
  ;; (update-in db [:key-input :pressed] #(conj % key)) ;; sets
  (update-in db [:key-input :pressed] #(conj! % key))  ;; for transients sets
  )

(defn switch-pause-flag [db]
  (update-in db [:state :scene :paused] #(if % false true)))

        ;;;; cofx functions ;;;;

(defn no-op [{db :db :as cofx}] {:db db})

(defn switch-mode-cofx
  [{db :db} mode-vec]
  {:db (switch-mode db mode-vec)})

(defn move-cursor-cofx
  [{db :db} dir]
  {:db (move-cursor db dir)})

(defn handle-options-enter-cofx
  [{db :db}]
  {:db (handle-options-enter db)})

(defn handle-controls-enter-cofx
  [{db :db}]
  {:db (handle-controls-enter db)})

(defn remove-pressed-key-cofx
  [{db :db} key]
  {:db (remove-pressed-key db key)})

(defn add-pressed-key-cofx
  [{db :db} key]
  {:db (add-pressed-key db key)})

(defn switch-pause-flag-cofx
  [{db :db}]
  {:db (switch-pause-flag db)})

        ;;;; keymaps ;;;;

(def cursor-screen-actions
  {"ArrowUp" #(move-cursor-cofx % :up)
   "ArrowDown" #(move-cursor-cofx % :down)})

(def menu-down-actions
  (let [handle-select-fn  #(switch-mode-cofx %
                                             [:single :versus
                                              ;; :options
                                              :controls
                                              :credits] )      ]
  (merge cursor-screen-actions
         {"Enter" handle-select-fn}
         {" " handle-select-fn}
         )))

(def credit-down-actions
  {"Escape" #(switch-mode-cofx % :menu) }
  )

(def options-down-actions
  (let [handle-select-fn #(handle-options-enter-cofx %)]
  (merge cursor-screen-actions credit-down-actions
         {"Enter" handle-select-fn}
         {" " handle-select-fn}
         )))

(def controls-down-actions
  (let [handle-select-fn #(handle-controls-enter-cofx %)]
    (merge cursor-screen-actions credit-down-actions
           {"Enter" handle-select-fn}
           {" " handle-select-fn}
         )))

(def end-down-actions
  (let [handle-select-fn #(switch-mode-cofx % [:previous :menu])]
  (merge cursor-screen-actions credit-down-actions
         {"Enter" handle-select-fn}
         {" " handle-select-fn}
         )))

(def game-down-actions
  (merge credit-down-actions
         {" " #(switch-pause-flag-cofx %)}
         ))

(def down-actions-by-mode
  {:menu  menu-down-actions
   :single game-down-actions
   :versus game-down-actions
   :options nil ;;;options-down-actions
   :controls controls-down-actions
   :credits credit-down-actions
   :end end-down-actions
})

        ;;;; key-handlers ;;;;

(defn handle-key-down
  [{db :db :as cofx} kw {:keys [code key shift alt] :as data}]
  (let [p-one-keys ((juxt :thrust :fire :left :right)
                    (rfu/<sub [::subs/p-one-keys]))
        p-two-keys ((juxt :thrust :fire :left :right)
                    (rfu/<sub [::subs/p-two-keys]))
        mode (:mode (:state db))

        ckey-set (set (if (= mode :versus)
                        (into p-one-keys p-two-keys)
                        p-one-keys))
        action (get-in down-actions-by-mode [(:mode (:state db)) key])]

    (cond action (action cofx)
          (ckey-set key) (add-pressed-key-cofx cofx key)
          :else (no-op cofx))
 ) )

(defn handle-key-up
  [{db :db :as cofx} kw {:keys [event key shift alt] :as data}]
  (remove-pressed-key-cofx cofx key)
  )


        ;;;; game scene updates ;;;;

(defn rotate-vector [vect angle]
  (let [[vx vy] vect]
    [(- (* vx (Math/cos angle)) (* vy (Math/sin angle)))   ;; x
     (+ (* vy (Math/cos angle)) (* vx (Math/sin angle)))])) ;; y


(defn check-collision
  "Check if `sprite1` 'collides' with `sprite2`."
  [sprite1 sprite2]
  (let [[p1c p2c] (mapv obj/get-corners [sprite1 sprite2])
        ;; make (p1c 0) origin
        [ox oy] (p1c 0) ;; get relative origin
        trans-fn (fn [[x y]] [(- x ox) (- y oy)])
        ;; translate to relative origin
        [p1cT p2cT] (mapv #(mapv trans-fn %) [p1c p2c])
        ;; angle to alighn p1 bounding box
        angle (Math/atan2 (second (p1cT 2)) (first (p1cT 2)))
        ;; [p1cTR p2cTR] (mapv #(mapv trans-fn %) [p1c p2c])
        ;; rotate vectors to align
        p1cTR (mapv #(rotate-vector % angle) p1cT)
        p2cTR (mapv #(rotate-vector % angle) p2cT)
        ;; calculate extreme points of bounding boxes
        p1x-list (map first p1cTR)
        p1y-list (map second p1cTR)
        p2x-list (map first p2cTR)
        p2y-list (map second p2cTR)
        [p1-left p1-right] [(apply min p1x-list) (apply max p1x-list)]
        [p1-top p1-bot]  [(apply min p1y-list) (apply max p1y-list)]
        [p2-left p2-right] [(apply min p2x-list) (apply max p2x-list)]
        [p2-top p2-bot]  [(apply min p2y-list) (apply max p2y-list)]
        ]
    (and (< p2-left p1-right) (< p1-left p2-right)
         (< p2-top p1-bot) (< p1-top p2-bot))
    ))

(defn check-array-collision
  "Check if the objects contained in vec1 'collide' with the objects contained in vec2.
   Returns a vector of the vectors. Uses loop to iterate."
  [vec1 vec2]
  (let [idx1 (count vec1)
        idx2 (count vec2)]
    (loop [i 0, rvec [vec1 vec2]]
      (if (>= i idx1)
        rvec
        (recur (inc i)
               (loop [j 0 jvec rvec]
                 (if (>= j idx2)
                   jvec
                   (recur (inc j)
                          (if (check-collision (vec1 i) (vec2 j))
                            [(update vec1 i obj/explode-sprite)
                             (update vec2 j obj/explode-sprite)]
                            jvec)))))))))


(defn check-array-collision-dsq
  "Check if the objects contained in vec1 'collide' with the objects contained in vec2.
   Returns a vector of the vectors. Uses doseq to iterate over transients
  (for runtime speed optimization)."
  [vec1 vec2]
  (let [idx1 (count vec1)
        idx2 (count vec2)
        rv1 (transient vec1)
        rv2 (transient vec2)]
    (doseq [i (range idx1) j (range idx2)]
      (let [sp1 (rv1 i)
            sp2 (rv2 j)]
      (when (check-collision sp1 sp2)
        (assoc! rv1 i (obj/explode-sprite sp1))
        (assoc! rv2 j (obj/explode-sprite sp2)))))
    (mapv persistent! [rv1 rv2])))


(defn js-rotate-vector
  ""
  ^array [^array vect ^long angle]
  (let [vx (aget vect 0)
        vy (aget vect 1)]
    #js[(- (* vx (Math/cos angle)) (* vy (Math/sin angle)))   ;; x
        (+ (* vy (Math/cos angle)) (* vx (Math/sin angle)))]))

(defn js-translate-origin
  "Translate `point` to be relative to `origin`"
  ^array [^array point ^array origin]
  (let [ox (aget origin 0)
        oy (aget origin 1)
        x (aget point 0)
        y (aget point 1)]
    #js[(- x ox) (- y oy)]))


(defn js-check-collision
  "Check if `sprite1` collides with `sprite2` using javascript functions
  (for speed uptimizations)."
  ^boolean [^record sprite1 ^record sprite2]
  (let [p1c (clj->js (obj/get-corners sprite1))
        p2c (clj->js (obj/get-corners sprite2))
        ;; make (p1c 0) origin
        new-origin (aget p1c 0) ;; get relative origin

        ;; translate relative to new origin
        p1cT (.map p1c #(js-translate-origin % new-origin))
        p2cT (.map p2c #(js-translate-origin % new-origin))

        ;; angle to alighn p1 bounding box
        angle (Math/atan2 (aget p1cT 2 1) (aget p1cT 2 0))

        ;; rotate vectors to align
        p1cTR (.map p1cT #(js-rotate-vector % angle))
        p2cTR (.map p2cT #(js-rotate-vector % angle))

        ;; calculate extreme points of bounding boxes
        p1-left (apply Math/min (.map p1cTR #(aget % 0))) ;; these seem faster
        p1-right  (apply Math/max (.map p1cTR #(aget % 0)) )
        p1-top (apply Math/min (.map p1cTR #(aget % 1)))
        p1-bot   (apply Math/max (.map p1cTR #(aget % 1)))
        p2-left (apply Math/min (.map p2cTR #(aget % 0)))
        p2-right  (apply Math/max (.map p2cTR #(aget % 0)))
        p2-top (apply Math/min (.map p2cTR #(aget % 1)))
        p2-bot   (apply Math/max (.map p2cTR #(aget % 1)))
        ]

    (and (< p2-left p1-right) (< p1-left p2-right)
         (< p2-top p1-bot) (< p1-top p2-bot))))



(defn js-check-array-collision
  "Check if the objects contained in vec1 'collide' with the objects contained in vec2.
   Returns a javascript array of the javascript array versions of the vectors
   (for runtime speed optimization). Uses loop to iterate."
  ^array [^vector vec1 ^vector vec2]
  (let [arr1 (into-array vec1)
        arr2 (into-array vec2)
        idx1 (alength arr1)
        idx2 (alength arr2)]
    (loop [i 0]
      (if (>= i idx1)
        #js[arr1 arr2]
        (do (loop [j 0]
              (if  (>= j idx2)
                nil
                (do (let [sp1 (aget arr1 i)
                          sp2 (aget arr2 j)]
                      (when (js-check-collision sp1 sp2)
                        (.splice arr1 i 1 (obj/explode-sprite sp1))
                        (.splice arr2 j 1 (obj/explode-sprite sp2))))
                    ;; (.splice arr1 i 1 (inc sp1))
                    ;; (.splice arr2 j 1 (inc sp2)))))
                    (recur (inc j)))))
            (recur (inc i)))))))


(defn js-check-array-collision-dsq
  "Check if the objects contained in vec1 'collide' with the objects contained in vec2.
   Returns a javascript array of the javascript array versions of the vectors
   (for runtime speed optimization). Uses doseq to iterate."
  ^array [^vector vec1 ^vector vec2]
  (let [arr1 (into-array vec1)
        arr2 (into-array vec2)
        idx1 (alength arr1)
        idx2 (alength arr2)]
    (doseq [^integer i (range idx1) ^integer j (range idx2)]
      (let [sp1 (aget arr1 i)
            sp2 (aget arr2 j)]
      (when (js-check-collision sp1 sp2)
          (.splice arr1 i 1 (obj/explode-sprite sp1))
          (.splice arr2 j 1 (obj/explode-sprite sp2)))))
        #js[arr1 arr2]))


(defn ai-ship-update
  "Update the computer controlled ship `ai-shp` relative to the player controlled ship
    `p-shp`."
  [ai-shp p-shp]
  (let [t-ai-shp (obj/update-sprite ai-shp)]
    (if (true? (:active t-ai-shp))
      (let [[p1x p1y] (obj/get-pos p-shp)
            [p2x p2y] (obj/get-pos t-ai-shp)
            {:keys [thrusters center shape core]} t-ai-shp
            {:keys [rotation xspeed yspeed]} core
            body-rotation (:rotation pr/body-vals)
            shot-dist (:distance pr/shot-vals)

            [dx dy] [(- p1x p2x) (- p1y p2y)]
            p1r (Math/hypot dx dy)
            delta1 (rem (- (Math/atan2 dy dx) rotation) gm/tau)
            delta2 (if (< delta1 Math/PI) delta1 (- delta1 gm/tau))
            delta3 (if (< delta2 (- Math/PI)) (+ delta2 gm/tau) delta2)
            delta4 (if (< (Math/abs delta3) body-rotation)
                     delta3
                     (* (Math/sign delta3) body-rotation))
            nrotation (+ rotation delta4)
            ;; updates
            ;; tcore (obj/update-sprite core)
            show-shape (pr/rotate-shape shape center nrotation)
            [n-shot-time nshots] (if (and (< p1r (* 1.5 shot-dist))
                                          (< delta4 body-rotation))
                                   (obj/fire-shot t-ai-shp)
                                   ((juxt :shot-timeout :shots) t-ai-shp))
            [nthrust txsp tysp] (if (or (> (count (:shots p-shp)) 2)
                                        (and (> p1r (* 1.5 shot-dist))
                                             (< delta4 body-rotation)))
                                  (obj/fire-thrusters t-ai-shp)
                                  [false xspeed yspeed])
            [nxsp nysp] (pr/regulate-speed txsp tysp) ;; REVIEW: necessary?
            ncore (assoc core :rotation nrotation :xspeed nxsp :yspeed nysp)
            ]
        (assoc t-ai-shp :core ncore :show-shape show-shape :thrusters nthrust
               :shots nshots :shot-timeout n-shot-time )
        )
      t-ai-shp)))

(defn update-scene [db]
  (cond (or (not (#{:single :versus} (get-in db [:state :mode])))
            (get-in db [:state :scene :paused]))
          db
        (not (.hasFocus js/document)) (assoc-in db [:state :scene :paused] true)
        :else
        (let [cur-state (:state db)
              scene (:scene cur-state)
              {:keys [ship1 ship2]} scene
              tshp1 (obj/add-gravity (obj/update-sprite ship1))
              tshp2 (obj/add-gravity (if (= (:mode cur-state) :single)
                                       (ai-ship-update ship2 tshp1)
                                       (obj/update-sprite ship2)))
              [shots1 shots2] (mapv :shots [tshp1 tshp2])
              collision-array-1 (conj shots1 tshp1)
              collision-array-2 (conj shots2 tshp2)

              ;; [collided-array-1 collided-array-2] (check-array-collision
              ;; ;; [collided-array-1 collided-array-2] (check-array-collision-dsq
              ;;                                      collision-array-1
              ;;                                      collision-array-2)
              ;; nshp1 (assoc (last collided-array-1)
              ;;              :shots (vec (butlast collided-array-1)))
              ;; nshp2 (assoc (last collided-array-2)
              ;;              :shots (vec (butlast collided-array-2)))

              ;; using js arrays
              ;; [collided-array-1 collided-array-2] (js-check-array-collision
              [collided-array-1 collided-array-2] (js-check-array-collision-dsq
                                                   collision-array-1
                                                   collision-array-2)
              nshp1 (assoc (.pop collided-array-1)
                           :shots (vec collided-array-1))
              nshp2 (assoc (.pop collided-array-2)
                           :shots (vec collided-array-2))

              [ac1 ac2] (mapv :active [nshp1 nshp2])
              ndb (assoc-in db [:state :scene]
                            (merge scene {:ship1 nshp1 :ship2 nshp2} ))
              ]
          (cond (or (not ac1) (not ac2))
                (let [{:keys [state previous]} (initialize-state :end (:state ndb))]
                  (assoc db :state state :previous previous))
                :else
                ndb)
         ;;  ;; (println "update")
         ;;  ;; (println (map (juxt obj/get-pos :active) [nshp1 nshp2]))
         ;; (assoc-in db [:state :scene] (merge scene {:ship1 tshp1 :ship2 tshp2} ))
         ;; (assoc-in db [:state :scene] (merge scene {:ship1 nshp1 :ship2 nshp2} ))
          ;; collision-array-1
          ;; collided-array-2
         ;; nshp2

          ;; [0]

          )

        )
  )

(defn update-scene-cofx
  [{db :db}]
  {:db (update-scene db)})

(defn game-tick [{db :db :as cofx}]
  ;; (println "tick")
  (update-scene-cofx cofx)
  )
