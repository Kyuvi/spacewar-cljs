(ns spacewar.view
  {:author "Kyuvi"
   :license {:name "GPL-3.0 WITH Classpath-exception-2.0"
             :url "https://www.gnu.org/licenses/gpl-3.0.html"}}

  (:require [reagent.core :as rg]
            [reagent.dom :as rd]
            ;; [sutils.browser :as bru]
            [sutils.canvas :as cvu]
            [sutils.rf :as rfu]
            ;; [sutils.hershey :as hsh
            ;;  :refer [canvas-write-text canvas-write-centered]
            ;;  :rename {canvas-write-text write-h
            ;;           canvas-write-centered write-hc}]
            [spacewar.hershey :as hs]
            [spacewar.obj :as obj]
            [spacewar.scenes :as sn]
            [spacewar.prep :as pr]
            [spacewar.rfm.subs :as subs]
            )
  )

(def canvas-style {:max-width "80%" :max-height "80%" :margin-top "2%"
                   ;; :position "relative"
                           })

(defn spcanv []
  (let [{:keys [width height radius]} pr/game-view
        update-view
        (fn [comp]
          (let [
                ;; game-ctx (.getContext (.-firstChild (rd/dom-node comp)) "2d")
                ;; mask-ctx (.getContext (.-lastChild (rd/dom-node comp)) "2d")
                mask-ctx (cvu/get-context (.-firstChild (rd/dom-node comp)))
                game-ctx (cvu/get-context (.-lastChild (rd/dom-node comp)))
                state (get (rg/props comp) :state)
                {:keys [mode star-list]} state
                ;; stars (:star-list state)
                ;; mode (:mode state)
                ;; (set! (.-shadowBlur game-ctx) 20)
                ]

            ;; mask
            (set! (.-globalCompositeOperation mask-ctx) "xor")
            (cvu/fill-rect mask-ctx 0 0 width height "black" )
            (cvu/fill-circle mask-ctx (/ width 2) (/ height 2) (inc radius))

            ;; main-screen
            (pr/draw-border game-ctx)
            (pr/draw-stars game-ctx star-list)
            ;; (set! (.-lineWidth game-ctx) 1)
            (case mode
              :menu
              (sn/draw-menu-scene game-ctx state )
              (:single :versus)
              (sn/draw-game-scene game-ctx state)
              :controls
              (sn/draw-controls-scene game-ctx state)
              :credits
              (sn/draw-credits-scene game-ctx)
              :end
              (sn/draw-end-scene game-ctx state)

              )
                ;; (set! (.-shadowColor mask-ctx) "#080")
                ;; (set! (.-shadowOffsetX mask-ctx) 0)
                ;; (set! (.-shadowOffsetY mask-ctx) 0)
                ;; (set! (.-shadowBlur mask-ctx) 20)
            ;; (write-h )
            ;; (write-h game-ctx 200 200 "hei" 4 :color pr/vector-color)
            ;; (hs/write-text game-ctx 200 200 "hei" 4 :color pr/vector-color)
            ;; (hs/write-text game-ctx 0 0 "hei" 4 :color pr/vector-color)

            ;; (cvu/fill-rect mask-ctx 0 0 width height )
            ))]
    (rg/create-class
     {:component-did-mount update-view
      :component-did-update update-view
      :reagent-render
      (fn []
        [:div.game
         [:canvas {:id "mask-canv"
                   :width width :height height
                   :shadow-color "#080"
                   ;; :shadow-offset-x 0
                   ;; :shadow-offset-y 0
                   ;; :shadow-blur 20
                   ;; :style canvas-style
                   :style (conj canvas-style {:position "absolute"})
                   }]
         [:canvas {:id "game-canv"
                   :width width :height height
                   :style canvas-style
                   ;; :style (conj canvas-style {:position "absolute"})
                   }]]
        )
      })))

(defn spcanv-outer []
  (let [state (rfu/<sub [::subs/state])]
    [(spcanv) {:state state}]))
