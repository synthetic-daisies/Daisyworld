globals [
  max-age               ;; maximum age that all daisies live to
  global-temperature    ;; the average temperature of the patches in the world
  num-blacks            ;; the number of black daisies
  num-whites            ;; the number of white daisies
  scenario-phase        ;; interval counter used to keep track of what portion of scenario is currently occurring
  ]

breed [daisies daisy]

patches-own [temperature]  ;; local temperature at this location

daisies-own [
  age       ;; age of the daisy
  albedo    ;; fraction (0-1) of energy absorbed as heat from sunlight
]


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Setup Procedures ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

to setup
  clear-all
  set-default-shape daisies "flower"
  ask patches [ set pcolor gray ]

  set max-age 25
  set global-temperature 0

  if (scenario = "ramp-up-ramp-down"    ) [ set solar-luminosity 0.8 ]
  if (scenario = "low solar luminosity" ) [ set solar-luminosity 0.6 ]
  if (scenario = "our solar luminosity" ) [ set solar-luminosity 1.0 ]
  if (scenario = "high solar luminosity") [ set solar-luminosity 1.4 ]

  seed-blacks-randomly
  seed-whites-randomly
  ask daisies [set age random max-age]
  ask patches [calc-temperature]
  set global-temperature (mean [temperature] of patches)
  update-display
  reset-ticks
end

to seed-blacks-randomly
   ask n-of round ((start-%-blacks * count patches) / 100) patches with [not any? daisies-here]
     [ sprout-daisies 1 [set-as-black] ]
end

to seed-whites-randomly
   ask n-of floor ((start-%-whites * count patches) / 100) patches with [not any? daisies-here]
     [ sprout-daisies 1 [set-as-white] ]
end


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Runtime Procedures ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


to go
   ask patches [calc-temperature]
   diffuse temperature .5
   ask daisies [check-survivability]
   set global-temperature (mean [temperature] of patches)
   update-display
   tick
   if scenario = "ramp-up-ramp-down" [
     if ticks > 200 and ticks <= 400 [
       set solar-luminosity precision (solar-luminosity + 0.005) 4
     ]
     if ticks > 600 and ticks <= 850 [
       set solar-luminosity precision (solar-luminosity - 0.0025) 4
     ]
   ]
   if scenario = "low solar luminosity"  [ set solar-luminosity 0.6 ]
   if scenario = "our solar luminosity"  [ set solar-luminosity 1.0 ]
   if scenario = "high solar luminosity" [ set solar-luminosity 1.4 ]
end

to set-as-black ;; turtle procedure
  set color black
  set albedo albedo-of-blacks
  set age 0
  set size 0.6
end

to set-as-white  ;; turtle procedure
  set color white
  set albedo albedo-of-whites
  set age 0
  set size 0.6
end

to check-survivability ;; turtle procedure
  let seed-threshold 0
  let not-empty-spaces nobody
  let seeding-place nobody

  set age (age + 1)
  ifelse age < max-age
  [
     set seed-threshold ((0.1457 * temperature) - (0.0032 * (temperature ^ 2)) - 0.6443)
     ;; This equation may look complex, but it is just a parabola.
     ;; This parabola has a peak value of 1 -- the maximum growth factor possible at an optimum
     ;; temperature of 22.5 degrees C
     ;; -- and drops to zero at local temperatures of 5 degrees C and 40 degrees C. [the x-intercepts]
     ;; Thus, growth of new daisies can only occur within this temperature range,
     ;; with decreasing probability of growth new daisies closer to the x-intercepts of the parabolas
     ;; remember, however, that this probability calculation is based on the local temperature.

     if (random-float 1.0 < seed-threshold) [
       set seeding-place one-of neighbors with [not any? daisies-here]

       if (seeding-place != nobody)
       [
         if (color = white)
         [
           ask seeding-place [sprout-daisies 1 [set-as-white]  ]
         ]
         if (color = black)
         [
           ask seeding-place [sprout-daisies 1 [set-as-black]  ]
         ]
       ]
     ]
  ]
  [die]
end

to calc-temperature  ;; patch procedure
  let absorbed-luminosity 0
  let local-heating 0
  ifelse not any? daisies-here
  [   ;; the percentage of absorbed energy is calculated (1 - albedo-of-surface) and then multiplied by the solar-luminosity
      ;; to give a scaled absorbed-luminosity.
    set absorbed-luminosity ((1 - albedo-of-surface) * solar-luminosity)
  ]
  [
      ;; the percentage of absorbed energy is calculated (1 - albedo) and then multiplied by the solar-luminosity
      ;; to give a scaled absorbed-luminosity.
    ask one-of daisies-here
      [set absorbed-luminosity ((1 - albedo) * solar-luminosity)]
  ]
  ;; local-heating is calculated as logarithmic function of solar-luminosity
  ;; where a absorbed-luminosity of 1 yields a local-heating of 80 degrees C
  ;; and an absorbed-luminosity of .5 yields a local-heating of approximately 30 C
  ;; and a absorbed-luminosity of 0.01 yields a local-heating of approximately -273 C
  ifelse absorbed-luminosity > 0
      [set local-heating 72 * ln absorbed-luminosity + 80]
      [set local-heating 80]
  set temperature ((temperature + local-heating) / 2)
     ;; set the temperature at this patch to be the average of the current temperature and the local-heating effect
end

to paint-daisies   ;; daisy painting procedure which uses the mouse location draw daisies when the mouse button is down
  if mouse-down?
  [
    ask patch mouse-xcor mouse-ycor [
      ifelse not any? daisies-here
      [
        if paint-daisies-as = "add black"
          [sprout-daisies 1 [set-as-black]]
        if paint-daisies-as = "add white"
          [sprout-daisies 1 [set-as-white]]
      ]
      [
        if paint-daisies-as = "remove"
          [ask daisies-here [die]]
      ]
      display  ;; update view
    ]
  ]
end

to update-display
  ifelse (show-temp-map? = true)
    [ ask patches [set pcolor scale-color red temperature -50 110] ]  ;; scale color of patches to the local temperature
    [ ask patches [set pcolor grey] ]

  ifelse (show-daisies? = true)
    [ ask daisies [set hidden? false] ]
    [ ask daisies [set hidden? true] ]
end


; Copyright 2006 Uri Wilensky.
; See Info tab for full copyright and license.
