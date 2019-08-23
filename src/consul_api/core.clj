(ns consul-api.core
  (:require [compojure.api.sweet :refer :all]
            [consul-api.domain :refer :all]
            [schema.core :as s]
            [ring.swagger.json-schema :refer [field]]))

(def consul-api
  (api
    :swagger {:spec "/swagger.json"
              :data {:info {:version "1"
                            :title "Consul API"}}
              :tags [{:name "ACL"
                      :descriptino "Access Control Lists"}]}

    (context "/v1" []
      (context "/acl" []
        :tags ["ACL"]
        (PUT "/bootstrap" []
          :summary "Bootstrap ACLs"
          :description "This endpoint does a special one-time bootstrap of the ACL system, making the first management token if the acl.tokens.master configuration entry is not specified in the Consul server configuration and if the cluster has not been bootstrapped previously. This is available in Consul 0.9.1 and later, and requires all Consul servers to be upgraded in order to operate.

          This provides a mechanism to bootstrap ACLs without having any secrets present in Consul's configuration files."
          :return ACLBootstrapResponse
          :responses {200 {:description "The ACL system was successfully bootstrapped."}
                      403 {:description "The ACL system has already been bootstrapped; the cluster may be in a compromised state."}})

        (GET "/replication" []
          :summary "Check ACL Replication"
          :description "This endpoint returns the status of the ACL replication processes in the datacenter. This is intended to be used by operators or by automation checking to discover the health of ACL replication."
          :query-params [{dc :- s/Str ""}]
          :return ACLReplicationStatus
          :responses {200 {:description "The replication status was returned"}})

        (POST "/rules/translate" []
          :summary "Translate Rules"
          :description "Deprecated - This endpoint was introduced in Consul 1.4.0 for migration from the previous ACL system. It will be removed in a future major Consul version when support for legacy ACLs is removed.

          This endpoint translates the legacy rule syntax into the latest syntax. It is intended to be used by operators managing Consul's ACLs and performing legacy token to new policy migrations."
          :body [r s/Str]
          :return s/Str
          :responses {200 {:description "The call was successful"}})

        (GET "/rules/translate/:accessor_id" []
          :summary "Translate a Legacy Token's Rules"
          :description "Deprecated - This endpoint was introduced in Consul 1.4.0 for migration from the previous ACL system.. It will be removed in a future major Consul version when support for legacy ACLs is removed.

          This endpoint translates the legacy rules embedded within a legacy ACL into the latest syntax. It is intended to be used by operators managing Consul's ACLs and performing legacy token to new policy migrations. Note that this API requires the auto-generated Accessor ID of the legacy token. This ID can be retrieved using the /v1/acl/token/self endpoint."
          :path-params [accessor_id :- s/Uuid]
          :return s/Str
          :responses {200 {:description "The call was successful"}})

        (POST "/login" []
          :summary "Login to Auth Method"
          :description "This endpoint was added in Consul 1.5.0 and is used to exchange an auth method bearer token for a newly-created Consul ACL token."
          :body [req ACLLoginRequest]
          :return ACLLoginResponse
          :responses {200 {:description "The login was successful"}})

        (POST "/logout" []
          :summary "Logout from Auth Method"
          :description "This endpoint was added in Consul 1.5.0 and is used to destroy a token created via the login endpoint. The token deleted is specified with the X-Consul-Token header or the token query parameter."
          :responses {200 {:description "The logout was successful."}})

        (PUT "/token" []
          :summary "Create a Token"
          :description "This endpoint creates a new ACL token."
          :body [r ACLTokenRequest]))

      (context "/agent" []
        :tags ["Agent"]
        (GET "/members" []
          :summary "List Members"
          :description "This endpoint returns the members the agent sees in the cluster gossip pool. Due to the nature of gossip, this is eventually consistent: the results may differ by agent. The strongly consistent view of nodes is instead provided by /v1/catalog/nodes."
          :query-params [{wan :- s/Bool false}
                         {segment :- s/Str ""}]
          :return [AgentMember])

        (GET "/self" []
          :summary "Read Configuration"
          :description "This endpoint returns the configuration and member information of the local agent. The Config element contains a subset of the configuration and its format will not change in a backwards incompatible way between releases. DebugConfig contains the full runtime configuration but its format is subject to change without notice or deprecation."
          :return AgentConfiguration)

        (PUT "/reload" []
          :summary "Reload Agent"
          :description "This endpoint instructs the agent to reload its configuration. Any errors encountered during this process are returned.

          Not all configuration options are reloadable. See the Reloadable Configuration section on the agent options page for details on which options are supported.")

        (PUT "/maintenance" []
          :summary "Enable Maintenance Mode"
          :description "This endpoint places the agent into \"maintenance mode\". During maintenance mode, the node will be marked as unavailable and will not be present in DNS or API queries. This API call is idempotent.

          Maintenance mode is persistent and will be automatically restored on agent restart.")

        (GET "/metrics" []
          :summary "View Metrics"
          :description "This endpoint will dump the metrics for the most recent finished interval. For more information about metrics, see the telemetry page.

          In order to enable Prometheus support, you need to use the configuration directive prometheus_retention_time.

          Note: If your metric includes labels that use the same key name multiple times (i.e. tag=tag2 and tag=tag1), only the sorted last value (tag=tag2) will be visible on this endpoint due to a display issue. The complete label set is correctly applied and passed to external metrics providers even though it is not visible through this endpoint."
          :return AgentMetrics)

        (GET "/monitor" []
          :summary "Stream Logs"
          :description "This endpoint streams logs from the local agent until the connection is closed."
          :query-params [{loglevel :- s/Str "info"}]
          :return s/Str)

        (PUT "/join/:address" []
          :summary "Join Agent"
          :description "This endpoint instructs the agent to attempt to connect to a given address."
          :path-params [address :- (field s/Str {:description "Specifies the address of the other agent to join."})]
          :query-params [{wan :- (field s/Bool {:description "Specifies to try and join over the WAN pool. This is only optional for agents running in server mode."}) false}])

        (PUT "/leave" []
          :summary "Graceful Leave and Shutdown"
          :description "This endpoint triggers a graceful leave and shutdown of the agent. It is used to ensure other nodes see the agent as \"left\" instead of \"failed\". Nodes that leave will not attempt to re-join the cluster on restarting with a snapshot.

          For nodes in server mode, the node is removed from the Raft peer set in a graceful manner. This is critical, as in certain situations a non-graceful leave can affect cluster availability.")

        (PUT "/force-leave/:node" []
          :summary "Force Leave and Shutdown"
          :description "This endpoint instructs the agent to force a node into the left state. If a node fails unexpectedly, then it will be in a failed state. Once in the failed state, Consul will attempt to reconnect, and the services and checks belonging to that node will not be cleaned up. Forcing a node into the left state allows its old entries to be removed."
          :path-params [node :- (field s/Str {:description "Specifies the name of the node to be forced into left state."})])

        (PUT "/agent/token/:token_name" []
          :summary "Update ACL Tokens"
          :description "This endpoint updates the ACL tokens currently in use by the agent. It can be used to introduce ACL tokens to the agent for the first time, or to update tokens that were initially loaded from the agent's configuration. Tokens will be persisted only if the acl.enable_token_persistence configuration is true. When not being persisted, they will need to be reset if the agent is restarted."
          :path-params [token_name (s/enum :default :agent :agent_master :replication :acl_token :acl_agent_token :acl_agent_master_token :acl_replication_token)]
          :body [req {:Token (field s/Uuid {:description "Specifies the ACL token to set"})}])

        (GET "/checks" []
          :summary "List Checks"
          :description "This endpoint returns all checks that are registered with the local agent. These checks were either provided through configuration files or added dynamically using the HTTP API.

          It is important to note that the checks known by the agent may be different from those reported by the catalog. This is usually due to changes being made while there is no leader elected. The agent performs active anti-entropy, so in most situations everything will be in sync within a few seconds."
          :query-params [{filter :- (field s/Str {:description "Specifies the expression used to filter the queries results prior to returning the data"}) ""}]
          :return AgentChecks)

        (PUT "/check/register" []
          :summary "Register Check"
          :description "This endpoint adds a new check to the local agent. Checks may be of script, HTTP, TCP, or TTL type. The agent is responsible for managing the status of the check and keeping the Catalog in sync."
          :body [r RegisterCheckRequest])

        (PUT "/check/deregister/:check_id" []
          :summary "Deregister Check"
          :description "This endpoint remove a check from the local agent. The agent will take care of deregistering the check from the catalog. If the check with the provided ID does not exist, no action is taken."
          :path-params [check_id :- (field s/Str {:description "Specifies the unique ID of the check to deregister."})])

        (PUT "/check/pass/:check_id" []
          :summary "TTL Check Pass"
          :description "This endpoint is used with a TTL type check to set the status of the check to passing and to reset the TTL clock."
          :path-params [check_id :- (field s/Str {:description "Specifies the unique ID of the check to use."})]
          :query-params [{note :- (field s/Str {:description "Specifies a human-readable message."}) ""}])))))
