(ns marceline.storm.state.serializers
  (:import [storm.trident.state
            JSONOpaqueSerializer
            JSONNonTransactionalSerializer
            JSONTransactionalSerializer]))


(defn json-opaque-serializer
  []
  (JSONOpaqueSerializer.))

(defn json-nontransactional-serializer
  []
  (JSONNonTransactionalSerializer.))

(defn json-transactional-serializer
  []
  (JSONTransactionalSerializer.))
