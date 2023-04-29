(ns sutils.hershey
  {:author "Kyuvi"
   :license {:name "GPL-3.0 WITH Classpath-exception-2.0"
             :url "https://www.gnu.org/licenses/gpl-3.0.html"}}
  (:require [clojure.string :as str ]
            [sutils.canvas :as cv]))

(declare hershey-characters)

(defn parse-letter [chc]
  (let [cdesc (get hershey-characters (str/capitalize chc)) ;; change if expanding
        ;; pair-num (re-find #"\d+" cdesc)
        ;; cpairs (str/replace cdesc pair-num)
        cpairs (re-find #"[^\d]+" cdesc)
        numerify-fn (fn [ch] (- (.charCodeAt ch) 82))

        ;; [limit-list cord-list] (split-at 2 cpairs)
        ;; [limits cord-list] (map #(map (- (int %) 82)) (split-at 2 cpairs))
        ;; [limit-list cord-list] (split-at 2 (map #(- (int %) 82)
                                                ;; " R" not neccesary
                                                ;; (str/replace cpairs " R" "")))
        ;; limit-list (take 2 (first c-vec))
        limit-list (map numerify-fn (take 2 cpairs))
        char-length (- (second limit-list ) (first limit-list))
        cord-str (str/replace cpairs #"^.." "")
        ;; cord-vec (str/split cord-str #" R")
        cord (if (empty? cord-str)
               []
                 ;; (str/split (->> ))
               (->>
                (str/split cord-str #" R")
                (mapv (fn [xs] (mapv numerify-fn xs)))
                (mapv #(partition 2 %))
                (mapv (fn [xs]
                        (mapv (fn [[a b]] [(- a (first limit-list))
                                           (+ b 5.5)])
                              xs)))
               ))
                      ;; (str/split (apply str cord-list) #" R")))
                      ;; (str/split cord-str #" R")))
        ;; cord-str (apply str cord-list)
        ;; cord-vec (str/split cord-str #" R")
        ;; [limit-list cord-list]
        ;; limits (map numerify-fn limit-list)

        ;; cord-temp (partition 2 cord-list)
        ;; cord (if cord-temp cord-temp '((0 0)))

        ]
    (list cord char-length)
    ;; limit-list
    ;; cord
    )

  )

(defn phrase-length [phrase size]
  (* size (apply + (map second (map parse-letter phrase))))
  )


(defn canvas-write-text
  ([ctx x y text]
   (canvas-write-text ctx x y text 1))
  ([ctx x y text size & {:keys [width color]}]
   ;; (let )
   (loop [last-position 0
          desc-list (map parse-letter text)]
     (if (empty? desc-list)
       last-position
       ;; (do
         (let [[coordinates end-position] (first desc-list)]
           (doseq [line-points coordinates]
             (cv/draw-seq ctx
                          (map (fn [[a b]]
                                 [(+ x  (* size a) (* size last-position ))
                                   (+ y (* size b))])
                               line-points)
                        :width width :color color)
           )
         ;; (cv/draw-seq ctx (first (first text-list)) :width width :color color)
           (recur (+ end-position last-position)
                  (rest desc-list))
           ;; )
     )
   )
  )))

(defn canvas-write-centered
  ([ctx cwidth y text]
   (canvas-write-centered ctx y text 1))
([ctx cwidth y text size & {:keys [width color]}]
 (let [text-length (phrase-length text size)]
   (canvas-write-text ctx (- (/ cwidth 2) (/ text-length 2)) y text size :width width :color color)
   )
  ))

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
  "<" "3NUTVORTN",
  ">" "3NUOVTRON"
   })
