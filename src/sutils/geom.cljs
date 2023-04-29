(ns sutils.geom)

(def ^:const pi Math/PI)

(def ^:const tau (* pi 2))

(defn deg->rad
  [a]
  (* tau (/ a 360)))

(defn rad->deg
  [a]
  (* 360 (/ a tau)))



        ;;;; 2d vector arithmetic ;;;;

(defn get-keys
  "Returns the values of the keys contained in `key-seq` from map `coll`.
  If coll is not a map, it is returned unchanged."
  [coll key-seq]
  (if (map? coll)
    (reduce #(conj %1 (get coll %2)) [] keys))
    coll)

(defn addv
   "Add two vectors/points together."
  ([x1 y1 x2 y2]
   [(+ x1 x2) (+ y1 y2)])
  ([[x1 y1] x2 y2]
   (addv x1 y1 x2 y2))
  ([[x1 y1] [x2 y2]]
   (addv x1 y1 x2 y2))
 )

(defn subv
  "Subtract the second vector/point from the first."
  ([x1 y1 x2 y2]
   [(- x1 x2) (- y1 y2)])
  ([[x1 y1] x2 y2]
   (subv x1 y1 x2 y2))
  ([[x1 y1 [x2 y2]]]
   (subv x1 y1 x2 y2)))

;; magnitude (mag) and angle (ang) of [x y] vectors
(defn mag
  "Get the magnitude of a vector."
  ([x y]
   (Math/hypot x y))
  ([point]
   (apply mag (get-keys point [:x :y]))))

(defn ang
  "Get the angle of a vector."
  ([x y]
   (Math/atan2 y x))
  ([point]
   (apply ang (get-keys point [:x :y]))))

(defn dist
  "Distance between two xy points."
  ([x1 y1 x2 y2]
   (mag (- x2 x1) (- y2 y1)))
  ([x1 y1 p2]
   (apply dist x1 y1 (get-keys p2 [:x :y]))))

;; make points
(defn xy-point
  "Turn [x y] into {:x x :y y}."
  [v]
  (zipmap v [:x :y]))

(defn polar-point
  "Turn [r a] into {:r r :a a}."
  [v]
  (zipmap v [:r :a]))

;; cartesian to polar conversion
(defn polar->xy
  "Convert polar coordinates to cartesian.
  a is in rad
  returns [x y]."
  ([r a]
   [(* r (Math/cos a)) (* r (Math/sin a))])
  ([point]
   (apply polar->xy (get-keys point [:r :a]))))

(defn xy->polar
  "Convert cartesian coordinates to polar
  returns [r a]."
  ([x y]
   [(mag x y) (ang x y)])
  ([point]
   (apply xy->polar (get-keys point [:x :y]))))

;; intersection and collison

(defn cc-hit?
  "Are these two circles colliding?
  expects two maps that each have :x :y and :r keys."
  [{x1 :x y1 :y r1 :r} {x2 :x y2 :y r2 :r}]
  (> (+ r1 r2) (dist x1 y1 x2 y2)))

;; translation
(defn trans-xy
  "Translate an xy point by dx dy yielding [x' y']."
  [x y dx dy]
  [(+ x dx) (+ y dy)])

(defn trans-polar
  "Translate an xy point by a distance (r) in a direction (a radians)
    yielding [x' y']."
  [x y r a]
  (let [[dx dy] (polar->xy r a)]
    (trans-xy x y dx dy)))
