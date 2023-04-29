(ns spacewar.core
  {:author "Kyuvi"
   :license {:name "GPL-3.0 WITH Classpath-exception-2.0"
             :url "https://www.gnu.org/licenses/gpl-3.0.html"}}
    (:require
              [reagent.core :as reagent :refer [atom]]
              [reagent.dom :as rd]
              [re-frame.core :as rf]
              [sutils.browser :as br]
              [sutils.rf :as rfu]

              [spacewar.prep :as pr]
              [spacewar.obj :as obj]
              [spacewar.view :as vw]
              [spacewar.rfm.events :as events]
              [spacewar.rfm.subs :as subs]
              ))

(enable-console-print!)

(println "Here at src/spacewar/core.cljs.")


(defn foot-notes []
  [:div.footer
   [:h4 "Copyright© 2023 " [:a {:href "https://codeberg.org/Kyuvi" } "Kyuvi"]]
   [:p "This software is under the "
    [:a {:href "https://www.gnu.org/licenses/gpl-3.0.en.html"} "GNU GPL3"]
    " license"]])

(defn game-canv []
  [:div#game-frame
   [vw/spcanv-outer]
   ])

(defn full-page []
  [:div.container
   [:title "SPACEWAR! [Cljs]"]
   [game-canv]
   [foot-notes]])

(defn ^:dev/after-load render-page []
  (rf/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rd/unmount-component-at-node root-el)
    (rd/render [full-page] root-el)))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)

(defn dispatch-key-handler [e kw]
 (let [player-keys (rfu/<sub [::subs/controls])
        {:keys [p1 p2]}  player-keys
       ;; TODO: depend on mode?
        key-list (apply into (map (juxt :thrust :fire :left :right) [p1 p2]))
        menu-keys #{"Enter" "ArrowUp" "ArrowDown" " " "Escape"}
        key-set (into menu-keys key-list)]
  (when (key-set e.key) (.preventDefault e))
  (rf/dispatch [kw {:code e.keyCode :key e.key :shift e.shiftKey :alt e.altKey}])) )

(defonce tick (js/setInterval #(rf/dispatch [::events/tick])
                              (/ 1000 (:fps pr/game-view))))

(defn run-spacewar []
  (rf/dispatch-sync [::events/initialize])
  (render-page)
  (br/add-listener js/document "keydown" #(dispatch-key-handler % ::events/key-down))
  (br/add-listener js/document "keyup" #(dispatch-key-handler % ::events/key-up)))

(run-spacewar)
