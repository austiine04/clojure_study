(ns
  ^{:author mate.magyari
    :doc "Simple vector algebra"}
  clojure-study.ideas.swarm.vector-algebra
  (:require [clojure.test :as test]))

(defn square "Square of"
  [num]
  (* num num))

(defn v* "Multiplication for vectors"
  [scalar a-vector]
  {:x (* scalar (:x a-vector))
   :y (* scalar (:y a-vector))})

(defn v+ "Sum vector of vectors"
  [& vectors]
  (apply merge-with + vectors))

(defn v- "Diff vector of vectors"
  [v1 v2]
  (merge-with - v1 v2))

(defn vec-length [{x :x y :y}] "Length of a vector"
  (Math/sqrt
    (+ (square x) (square y))))

(defn polar->cartesian
  "Transforming a polar vector representation to a cartesian one"
  [{angle :angle length :length}]
  {:x (* length (Math/cos angle))
   :y (* length (Math/sin angle))})

(defn cartesian->polar
  "Transforming a cartesian vector representation to a polar one"
  [v]
  {:angle (Math/atan2 (:y v) (:x v))
   :length (vec-length v)})

(defn rotate-cartesian "Rotating a vector"
  [v angle]
  (-> v
    cartesian->polar
    (update-in [:angle] + angle)
    polar->cartesian))

(defn distance "Distance between 2 points"
  [point-from point-to]
  (vec-length
    (merge-with - point-to point-from)))

(defn null-vector? "Checks whether the vector is a null vector"
  [v]
  (or (= v {:x 0 :y 0})
    (= v {:x 0.0 :y 0.0})))

(defn normalize "Normalize the vector"
  [a-vector]
  (let [len (vec-length a-vector)]
    (if (= 0.0 len)
      a-vector ; null vector simply returned
      (let [div-len #(/ % len)]
        (-> a-vector
          (update-in [:x] div-len)
          (update-in [:y] div-len))))))

(defn direction-vector
  "(Normalized) direction vector from point A to point B"
  [point-from point-to]
  (normalize (merge-with - point-to point-from)))

(defn weight-point "Calculates the weight point of the points"
  [& points]
  (let [n (count points)
        sums (apply v+ points)]
    (-> sums
      (update-in [:x] #(/ % n))
      (update-in [:y] #(/ % n)))))


;;============== TESTS ==================
(defn is= [a b]
  (test/is (= a b)))

(defn is-close-enough [vec-1 vec-2]
  (test/is (> 0.001
             (vec-length
               (merge-with - vec-1 vec-2)))))

(test/deftest some-tests
  (is= 5.0 (vec-length {:x 4 :y 3}))
  (is= 5.0 (distance {:x 1 :y 3} {:x 4 :y 7}))
  (is= {:x 1.0 :y 0.0} (normalize {:x 4 :y 0}))
  (is= {:x 0.0 :y 1.0} (normalize {:x 0 :y 6}))
  (is-close-enough {:x (Math/sqrt 0.5) :y (Math/sqrt 0.5)} (normalize {:x 8 :y 8}))
  (is= {:x 0.0 :y 1.0} (direction-vector {:x 2 :y 6} {:x 2 :y 8}))
  (is= {:x -1.0 :y 0.0} (direction-vector {:x 8 :y 6} {:x 5 :y 6}))
  (is= {:x 15 :y 20} (v+ {:x 4 :y 6} {:x 6 :y 10} {:x 5 :y 4})))


(test/deftest cartesian->polar-test
  (is= {:angle 0.0, :length 1.0} (cartesian->polar {:x 1 :y 0}))
  (is= {:angle Math/PI, :length 1.0} (cartesian->polar {:x -1 :y 0}))
  (is-close-enough {:x 3 :y 7} (-> {:x 3 :y 7} cartesian->polar polar->cartesian)))

(test/deftest rotate-test
  (is-close-enough {:x 0 :y 1} (rotate-cartesian {:x 1 :y 0} (/ Math/PI 2)))
  (is-close-enough {:x -1 :y 0} (rotate-cartesian {:x 1 :y 0} Math/PI))
  (is-close-enough {:x 0 :y -1} (rotate-cartesian {:x -1 :y 0} (/ Math/PI 2)))
  (is-close-enough {:x 1 :y 0} (rotate-cartesian {:x -1 :y 0} Math/PI))
  )

(test/deftest weight-point-test
  (is-close-enough {:x 4 :y 6} (weight-point {:x 1 :y 4} {:x 7 :y 8})))


(test/run-tests 'clojure-study.ideas.swarm.vector-algebra)

