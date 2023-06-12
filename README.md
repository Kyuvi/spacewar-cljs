# spacewar

A clone (of a clone) of the classic spacewar! game in ClojureScript using the reagent and re-frame libraries.

## Overview

Mainly to experiment with using reagent/re-frame for making games on the html canvas. 

As I could not find any clojurescipt versions, it seemed like an interesting project to take on.

Version 1.0 can be played  [[https://kyuvi.codeberg.page/spacewar-cljs/@cmp/resources/public/](Here)]. This is meant to be a two player game so do not expect too much from the AI. Controls can be changed in the "options" menu

## Setup

To get an interactive development environment run:


    lein figwheel

<!-- and open your browser at [localhost:3449](http://localhost:3449/). -->
and open your browser at [localhost:4840](http://localhost:4840/).
This will auto compile and send all changes to the browser without the
need to reload. After the compilation process is complete, you will
get a Browser Connected REPL. An easy way to try it is:

    (js/alert "Am I connected?")

and you should see an alert in the browser window.

To clean all compiled files:

    lein clean

To create a production build run:

    lein do clean, cljsbuild once min

And open your browser in `resources/public/index.html`. You will not
get live reloading, nor a REPL. 

## License

Copyright © 2023 Kyuvi

This program and the accompanying materials are made available under the terms of the GNU General Public License 3.0 or later which is available at https://www.gnu.org/licenses/gpl-3.0.html with the GNU Classpath Exception which is available at https://www.gnu.org/software/classpath/license.html.

