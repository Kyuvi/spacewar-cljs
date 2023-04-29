(ns spacewar.rfm.subs
  "Namespace containing re-frames Subscription registrations for spacewar"
  {:author "Kyuvi"
   :license {:name "GPL-3.0 WITH Classpath-exception-2.0"
             :url "https://www.gnu.org/licenses/gpl-3.0.html"}}
  (:require [re-frame.core :as rf])
  )


(rf/reg-sub
 ::state
 (fn [db _] (:state db)))

(rf/reg-sub
 ::mode
 (fn [db _] (get-in db [:state :mode])))

(rf/reg-sub
 ::stars
 (fn [db _] (get-in db [:state :star-list])))

(rf/reg-sub
 ::cursor
 (fn [db _] (get-in db [:state :cursor])))

(rf/reg-sub
 ::current-cur
 (fn [db _] (get-in db [:state :cursor :current])))

(rf/reg-sub
 ::previous
 (fn [db _] (:previous db)))

(rf/reg-sub
 ::previous-mode
 (fn [db _] (get-in db [:previous :mode])))

(rf/reg-sub
 ::previous-score
 (fn [db _] (get-in db [:previous :score])))

(rf/reg-sub
 ::previous-ships
 (fn [db _] (get-in db [:previous :ships])))

        ;;;; game ;;;;

(rf/reg-sub
 ::ship-one
 (fn [db _] (get-in db [:state :scene :ship1])))

(rf/reg-sub
 ::ship-two
 (fn [db _] (get-in db [:state :scene :ship2])))


(rf/reg-sub
 ::gwell
 (fn [db _] (get-in db [:state :scene :gravity-well])))

(rf/reg-sub
 ::score
 (fn [db _] (get-in db [:state :scene :score])))

(rf/reg-sub
 ::paused
 (fn [db _] (get-in db [:state :scene :paused])))

        ;;;; settings ;;;;

(rf/reg-sub
 ::rounds
 (fn [db _] (get-in db [:state :settings :rounds])))

(rf/reg-sub
 ::controls
 (fn [db _] (get-in db [:state :settings :controls])))

(rf/reg-sub
 ::p-one-keys
 (fn [db _] (get-in db [:state :settings :controls :p1])))

(rf/reg-sub
 ::p-two-keys
 (fn [db _] (get-in db [:state :settings :controls :p2])))



        ;;;; key inputs ;;;;
;
(rf/reg-sub
 ::key-input
 (fn [db _] (:key-input db)))

(rf/reg-sub
 ::pressed
 (fn [db _] (get-in db [:key-input :pressed])))
