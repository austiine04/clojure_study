(ns
  ^{:author mate.magyari}
  marsrovers.pure.rover-controller
  (:require [marsrovers.api.rover-api :as r]
            [marsrovers.api.rover-controller-api :as c]
            [marsrovers.util :as u]))

(defn- rover-position [controller]
  {:post [(some? %)]}
  (get-in controller [:rover-config :position]))

(defn- rover-channel [controller]
  (get-in controller [:rover :rover-channel]))

(defn- rover-msg [controller body] (u/msg
                                     (rover-channel controller)
                                     body))
(defn- deploy-rover-msg [controller]
  (rover-msg
    controller
    (r/deploy-rover-msg (rover-position controller) (:in-channel controller))))

(defn- rover-action-msg [controller]
  (rover-msg
    controller
    (c/rover-action-msg (-> controller :actions peek))))

(defn- pop-action [controller]
  (update-in controller [:actions] pop))

(defn- has-actions? [controller]
  (-> (:actions controller) empty? not))

(defn- poison-pill-msg [controller]
  (rover-msg
    controller
    (c/posion-pill-msg)))

(defn- result [state & msgs]
  {:pre [(some? state) (some? msgs)]}
  {:state state :msgs msgs})

(defn- controller-log! [controller & text]
  (u/log! "Controller " (get-in controller [:rover :rover-id]) ": " text))

(defn receive [controller in-msg]
  (controller-log! controller " Message arrived: " (:rover-position in-msg))
  (condp = (:type in-msg)
    :start-rover (result
                   controller
                   (deploy-rover-msg controller))
    :rover-deployed (result
                      (pop-action controller)
                      (rover-action-msg controller))
    :position (if (has-actions? controller)
                (result
                  (pop-action controller)
                  (rover-action-msg controller))
                (result
                  controller
                  (poison-pill-msg controller)))))

(defn controller [rover-id rover-channel rover-config in-channel hq-channel]
  {:pre [(some? rover-id) (some? rover-channel) (some? rover-config) (some? in-channel) (some? hq-channel)]}
  {:rover {:rover-id rover-id :rover-channel rover-channel}
   :actions (:actions rover-config)
   :rover-config rover-config
   :in-channel in-channel
   :hq-channel hq-channel})
