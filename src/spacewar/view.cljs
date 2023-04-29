(ns spacewar.view
  {:author "Kyuvi"
   :license {:name "GPL-3.0 WITH Classpath-exception-2.0"
             :url "https://www.gnu.org/licenses/gpl-3.0.html"}}
  (:require [reagent.core :as rg]
            [reagent.dom :as rd]
            [sutils.canvas :as cvu]
            [sutils.rf :as rfu]
            [spacewar.hershey :as hs]
            [spacewar.obj :as obj]
            [spacewar.scenes :as sn]
            [spacewar.prep :as pr]
            [spacewar.rfm.subs :as subs]))

(def canvas-style {:max-width "80%" :max-height "80%" :margin-top "2%"
                   ;; :position "relative"
                           })

(defn spcanv []
  (let [{:keys [width height radius]} pr/game-view
        update-view
        (fn [comp]
          (let [mask-ctx (cvu/get-context (.-firstChild (rd/dom-node comp)))
                game-ctx (cvu/get-context (.-lastChild (rd/dom-node comp)))
                state (get (rg/props comp) :state)
                {:keys [mode star-list]} state]
            ;; mask
            (set! (.-globalCompositeOperation mask-ctx) "xor")
            (cvu/fill-rect mask-ctx 0 0 width height "black" )
            (cvu/fill-circle mask-ctx (/ width 2) (/ height 2) (inc radius))

            ;; main-screen
            (pr/draw-border game-ctx)
            (pr/draw-stars game-ctx star-list)
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
              (sn/draw-end-scene game-ctx state))))]
    (rg/create-class
     {:component-did-mount update-view
      :component-did-update update-view
      :reagent-render
      (fn []
        [:div.game
         [:canvas {:id "mask-canv"
                   :width width :height height
                   :shadow-color "#080"
                   :style (conj canvas-style {:position "absolute"})
                   }]
         [:canvas {:id "game-canv"
                   :width width :height height
                   :style canvas-style
                   }]])})))

(defn spcanv-outer []
  (let [state (rfu/<sub [::subs/state])]
    [(spcanv) {:state state}]))
