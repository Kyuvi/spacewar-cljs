(ns sutils.canvas
  "Clojurescript utilies for working with the html canvas."
  (:require [sutils.browser :as bru])
  )

(defn get-context [canvas] (.getContext canvas "2d" ))

(defn get-id-context [id] (get-context (bru/get-id-element id)))

(defn stroke-color [ctx] (.-strokeStyle ctx))
(defn line-width [ctx] (.-lineWidth ctx))
(defn fill-color [ctx] (.-fillStyle ctx))

(defn set-stroke-color! [ctx color] (set! (.-strokeStyle ctx) color))
(defn set-line-width! [ctx width] (set! (.-lineWidth ctx) width))
(defn set-fill-color! [ctx color] (set! (.-fillStyle ctx) color))

#_
(defmacro reseting-style [[ctx width color fill reset] & body]
  (let [old-width (when (and reset width) (.-lineWidth ctx))
        old-stroke (when (and reset stroke) (.-strokeStyte ctx))
        old-fill (when (and reset fill) (.-fillStyle ctx))]
    '(do
       ~@body
       (when ~old-width (set! (.-lineWidth ~ctx) ~old-width))
       (when ~old-stroke (set! (.-strokeStyle ~ctx) old-stroke))
       (when ~old-fill (set! (.-fillStyle ~ctx) ~old-fill))
    )))

(defn draw-line
  "Draw a line on `ctx` from `sx`,`sy` to `ex`,`ey`.
  `:width` and `:color` can be set by those keywords.
  Note: Setting width and color sets for ctx not just this function."
  [ctx sx sy ex ey & {:keys [width color]}]
  (when width (set! (.-lineWidth ctx) width))
  (when color (set! (.-strokeStyle ctx) color))
  (doto ctx
    (.beginPath)
    (.moveTo sx sy)
    (.lineTo ex ey)
    (.stroke))
  )

(defn draw-seq
  "Draw a line on `ctx` connecting the points contained by `sequ` which should
  be a collection of collections with count 2.
  `:width` and `:color` can be set by those keywords.
  Note: Setting width and color sets for ctx not just this function."
  [ctx xs & {:keys [width color] }]
  (when width (set! (.-lineWidth ctx) width))
  (when color (set! (.-strokeStyle ctx) color))
  (.beginPath ctx)
  (apply #(.moveTo ctx % %2) (first xs))
  ;; (apply ctx.moveTo (first array ))
  (doseq [pos (rest xs)]
    (apply #(.lineTo ctx % %2) pos))
  (.stroke ctx)
  )

(defn draw-point
  "Draw a point on `ctx` at point `x`,`y` .
  `:width` and `:color` can be set by those keywords, :width defaults to 1.
  Note: Setting width and color sets for ctx not just this function.
        This function increses lineWidth on ctx by 1 or width if given."
  [ctx x y & {:keys [width color] :or {width 1}}]
  (draw-line ctx x y (+ x width) (+ y width) :width (+ 1 width) :color color)
  )

(defn draw-rect
  "Draw a rectangle on `ctx` from `x`,`y` with width `width` and height,`height`.
  `:line-width` and `:color` can be set by those keywords.
  Note: Setting line-width and color sets for ctx not just this function."
  [ctx x y width height & {:keys [line-width color]}]
  (when line-width (set! (.-lineWidth ctx) line-width))
  (when color (set! (.-strokeStyle ctx) color))
  (doto ctx
    (.beginPath)
    (.strokeRect x y width height)
    (.stroke))
  )

(defn draw-arc
  "Draw an arc of radius `r` anti-clockwise on `ctx` centered at `x`,`y`
   starting at angle `a-start` to angle `a-end` with width `width` and height,`height`.
  line width and color can be set with `:lwidth` and `:color` respectively.
  Note: Setting line-width and color sets for ctx not just this function."
  [ctx x y r a-start a-end & {:keys [width color]}]
  (when width (set! (.-lineWidth ctx) width))
  (when color (set! (.-strokeStyle ctx) color))
  (doto ctx
    (.beginPath)
    (.arc x y r a-start a-end)
    (.stroke))
  )

(defn draw-circle
  "Draw a circle on `ctx` centered at `x`,`y` with radius `r`
  line width and color can be set with `:lwidth` and `:color` respectively.
  Note: Setting line-width and color sets for ctx not just this function."
  [ctx x y r & {:keys [width color]}]
  (draw-arc ctx x y r 0 (* 2 Math/PI) :width width :color color)
  )


(defn fill-rect
  "Draw a rectangle on `ctx` from `x`,`y` with width `width` and height,`height`.
  and fill with color `color` which defaults to the current  flll color of `ctx`
  Note: Setting fill color sets for ctx not just this function."
  ([ctx x y width height]
   (.fillRect ctx x y width height))
  ([ctx x y width height color]
   (set! (.-fillStyle ctx) color)
   (fill-rect ctx x y width height)
  ))

(defn fill-square
  "Draw a square on `ctx` from `x`,`y` with dimensions of `size`.
  and fill with color `color` which defaults to the current  flll color of `ctx`
  Note: Setting fill color [or shadow color] sets it for ctx not just this function."
  ([ctx x y size]
   (.fillRect ctx x y size size))
  ([ctx x y size color]
   (set! (.-fillStyle ctx) color)
   (.fillRect ctx x y size size))
  ([ctx x y size color shadow-pred]
   (set! (.-fillStyle ctx) color)
   (.fillRect ctx x y size size)
   (when shadow-pred
   (set! (.-shadowColor ctx) color))))

(defn fill-circle
  "Draw a circle on `ctx` centered at `x`,`y` with radius `r`
  and fill with color `color` which defaults to the current  flll color of `ctx`
  Note: Setting fill color sets for ctx not just this function."
  ([ctx x y r ]
   (doto ctx
    (.beginPath)
    (.arc x y r 0 (* 2 Math/PI))
    (.fill)))
  ([ctx x y r color]
   (set! (.-fillStyle ctx) color)
   (fill-circle ctx x y r)
   )
  )
