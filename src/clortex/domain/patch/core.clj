(ns clortex.domain.patch.core
	(:require [clortex.utils.math :refer :all]))

(defn make-columns
    [& {:keys [^int columns ^int cells-per-column dims] :or {columns 2048 cells-per-column 32 dims [2048]}}]
[])
    

(defn single-layer-patch
    [& {:keys [^int columns ^int cells-per-column dims] :as patch-spec :or {columns 2048 cells-per-column 32 dims [2048]}}]
    (let [randomer 
            (random-fn-with-seed 123456)
          patch-columns (make-columns patch-spec)
          data 
            (atom {:n-columns columns :cells-per-column cells-per-column :dims dims 
                   :randomer randomer :patch patch-columns})]
        {:patch data}))