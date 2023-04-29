(ns spacewar.rfm.events
  "Namespace containing re-frames event registrations"
  {:author "Kyuvi"
   :license {:name "GPL-3.0 WITH Classpath-exception-2.0"
             :url "https://www.gnu.org/licenses/gpl-3.0.html"}}
 (:require [reagent.core :as rg :refer [atom]]
            [reagent.dom :as rd]
            [re-frame.core :as rf]
            [sutils.rf :as rfu]
            ;; [pong.prep :as pr]
            ;; [pong.blkchars :as lt]
            ;; [pong.obj :as obj]
            [spacewar.kone :as kn]
  ))

(rf/reg-event-db
 ::initialize
 (fn [db event] (kn/initialize-state :menu))
 ;; (fn [db event] (kn/initialize-state :controls))
 )


(rf/reg-event-fx
 ::key-up
 (fn [cofx [kw data]]
    (kn/handle-key-up cofx kw data)))

(rf/reg-event-fx
 ::key-down
 (fn [cofx [kw data]]
    ;; (println kw data)
    ;; (println "key event dispatch")
    (kn/handle-key-down cofx kw data)))

(rf/reg-event-fx
 ::tick
 ;; (fn [cofx [_ dt]]
 (fn [cofx event]
       (kn/game-tick cofx)
    )
 )
