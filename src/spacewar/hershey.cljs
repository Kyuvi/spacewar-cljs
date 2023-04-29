(ns spacewar.hershey
  {:author "Kyuvi"
   :license {:name "GPL-3.0 WITH Classpath-exception-2.0"
             :url "https://www.gnu.org/licenses/gpl-3.0.html"}}
  (:require [clojure.string :as str ]
            [sutils.canvas :as cv]
            [spacewar.prep :as pr]))

(declare hershey-characters)

(defn parse-letter [chc]
  (let [cdesc (get hershey-characters (str/capitalize chc))
        cpairs (re-find #"[^\d]+" cdesc)
        numerify-fn (fn [ch] (- (.charCodeAt ch) 82))
        limit-list (map numerify-fn (take 2 cpairs))
        char-length (- (second limit-list ) (first limit-list))
        cord-str (str/replace cpairs #"^.." "")
        cord (if (empty? cord-str)
               []
               (->>
                (str/split cord-str #" R")
                (mapv (fn [xs] (mapv numerify-fn xs)))
                (mapv #(partition 2 %))
                (mapv (fn [xs]
                        (mapv (fn [[a b]] [(- a (first limit-list))
                                           (+ b 5.5)])
                              xs)))))]
    (list cord char-length)))

(defn phrase-length [phrase size]
  (* size (apply + (map second (map parse-letter phrase))))
  )


(defn write-text
  ([ctx x y text]
   (write-text ctx x y text 1))
  ([ctx x y text size & {:keys [width color]
                         :or {width 1 color pr/vector-color}}]
   (loop [last-position 0
          desc-list (map parse-letter text)]
     (if (empty? desc-list)
       last-position
         (let [[coordinates end-position] (first desc-list)]
           (doseq [line-points coordinates]
             (cv/draw-seq ctx
                          (map (fn [[a b]]
                                 [(+ x  (* size a) (* size last-position ))
                                   (+ y (* size b))])
                               line-points)
                        :width width :color color)
           )
           (recur (+ end-position last-position)
                  (rest desc-list))
     )
   )
  )))

(defn write-centered
  ([ctx y text]
   (write-centered ctx y text 1))
  ([ctx y text size & {:keys [width color]
                       :or {width 1 color pr/vector-color}}]
   (let [text-length (phrase-length text size)]
     (write-text ctx (- (/ (:width pr/game-view) 2) (/ text-length 2))
                 y text size :width width :color color)
   )))


(def hershey-characters {
  "A" "9MWRMNV RRMVV RPSTS",
  "B" "16MWOMOV ROMSMUNUPSQ ROQSQURUUSVOV",
  "C" "11MXVNTMRMPNOPOSPURVTVVU",
  "D" "12MWOMOV ROMRMTNUPUSTURVOV",
  "E" "12MWOMOV ROMUM ROQSQ ROVUV",
  "F" "9MVOMOV ROMUM ROQSQ",
  "G" "15MXVNTMRMPNOPOSPURVTVVUVR RSRVR",
  "H" "9MWOMOV RUMUV ROQUQ",
  "I" "3PTRMRV",
  "J" "7NUSMSTRVPVOTOS",
  "K" "9MWOMOV RUMOS RQQUV",
  "L" "6MVOMOV ROVUV",
  "M" "12LXNMNV RNMRV RVMRV RVMVV",
  "N" "9MWOMOV ROMUV RUMUV",
  "O" "14MXRMPNOPOSPURVSVUUVSVPUNSMRM",
  "P" "10MWOMOV ROMSMUNUQSROR",
  "Q" "17MXRMPNOPOSPURVSVUUVSVPUNSMRM RSTVW",
  "R" "13MWOMOV ROMSMUNUQSROR RRRUV",
  "S" "13MWUNSMQMONOOPPTRUSUUSVQVOU",
  "T" "6MWRMRV RNMVM",
  "U" "9MXOMOSPURVSVUUVSVM",
  "V" "6MWNMRV RVMRV",
  "W" "12LXNMPV RRMPV RRMTV RVMTV",
  "X" "6MWOMUV RUMOV",
  "Y" "7MWNMRQRV RVMRQ",
  "Z" "9MWUMOV ROMUM ROVUV",
  " " "1NV",
  "0" "12MWRMPNOPOSPURVTUUSUPTNRM",
  "1" "4MWPORMRV",
  "2" "9MWONQMSMUNUPTROVUV",
  "3" "15MWONQMSMUNUPSQ RRQSQURUUSVQVOU",
  "4" "7MWSMSV RSMNSVS",
  "5" "14MWPMOQQPRPTQUSTURVQVOU RPMTM",
  "6" "14MWTMRMPNOPOSPURVTUUSTQRPPQOS",
  "7" "6MWUMQV ROMUM",
  "8" "19MWQMONOPQQSQUPUNSMQM RQQOROUQVSVUUURSQ",
  "9" "14MWUPTRRSPROPPNRMTNUPUSTURVPV",
  "." "6PURURVSVSURU",
  "," "7PUSVRVRUSUSWRY",
  ":" "12PURPRQSQSPRP RRURVSVSURU",
  ";" "13PURPRQSQSPRP RSVRVRUSUSWRY",
  "!" "12PURMRR RSMSR RRURVSVSURU",
  "?" "17NWPNRMSMUNUPRQRRSRSQUP RRURVSVSURU",
  "'" "3PTRMRQ",
  "\"" "6NVPMPQ RTMTQ",
  "°" "10NVQMPNPPQQSQTPTNSMQM",
  "$" "16MWUNSMQMONOPQQTRUSUUSVQVOU RRLRW",
  "/" "3MWVLNW",
  "(" "7OVTLRNQPQSRUTW",
  ")" "7NUPLRNSPSSRUPW",
  "|" "3PTRLRW",
  "-" "3LXNRVR",
  "+" "6LXRNRV RNRVR",
  "=" "6LXNPVP RNTVT",
  "*" "9MWRORU ROPUT RUPOT",
  "&" "21LXVRURTSSURVOVNUNSORRQSPSNRMPMONOPQSSUUVVV",
  "^" "3NVOPRLUP"
  "_" "3MWMXWX"
  "<" "3NUTVORTN",
  ">" "3NUOVTRON"
  "↑" "9NVRLRW RORRL RURRL"
  "↓" "9NVRLRW RORRW RURRW"
   })
