(ns consul-api.core
  (:require [compojure.api.sweet :refer :all]
            [consul-api.domain :refer :all]
            [schema.core :as s]
            [ring.swagger.json-schema :refer [field]]))

(defn consul-api
  ([] (consul-api (constantly nil)))
  ([handler]
   (api
     :swagger {:spec "/swagger.json"
               :ui "/"
               :data {:info {:version "1"
                             :title "Consul API"}}
               :tags [{:name "ACL"
                       :description "Access Control Lists"}
                      {:name "Agent"
                       :description "Agents"}
                      {:name "Catalog"
                       :description "Catalog"}]}

     (context "/v1" []
       (context "/acl" []
         :tags ["ACL"]
         :produces ["application/json"]
         :consumes ["application/json"]
         (PUT "/bootstrap" request
           :operationId "bootstrapACL"
           :summary "Bootstrap ACLs"
           :description "This endpoint does a special one-time bootstrap of the ACL system, making the first management token if the acl.tokens.master configuration entry is not specified in the Consul server configuration and if the cluster has not been bootstrapped previously. This is available in Consul 0.9.1 and later, and requires all Consul servers to be upgraded in order to operate.

          This provides a mechanism to bootstrap ACLs without having any secrets present in Consul's configuration files."
           :return ACLBootstrapResponse
           :responses {200 {:description "The ACL system was successfully bootstrapped."}
                       403 {:description "The ACL system has already been bootstrapped; the cluster may be in a compromised state."}}
           (handler request))

         (GET "/replication" request
           :operationId "checkACLReplication"
           :summary "Check ACL Replication"
           :description "This endpoint returns the status of the ACL replication processes in the datacenter. This is intended to be used by operators or by automation checking to discover the health of ACL replication."
           :query-params [{dc :- s/Str ""}]
           :return ACLReplicationStatus
           :responses {200 {:description "The replication status was returned"}}
           (handler request))

         (POST "/rules/translate" request
           :operationId "translateRules"
           :summary "Translate Rules"
           :produces ["text/plain"]
           :description "Deprecated - This endpoint was introduced in Consul 1.4.0 for migration from the previous ACL system. It will be removed in a future major Consul version when support for legacy ACLs is removed.

          This endpoint translates the legacy rule syntax into the latest syntax. It is intended to be used by operators managing Consul's ACLs and performing legacy token to new policy migrations."
           :body [r s/Str]
           :return s/Str
           :responses {200 {:description "The call was successful"}}
           (handler request))

         (GET "/rules/translate/:accessor_id" request
           :operationId "translateTokenRules"
           :summary "Translate a Legacy Token's Rules"
           :produces ["text/plain"]
           :description "Deprecated - This endpoint was introduced in Consul 1.4.0 for migration from the previous ACL system.. It will be removed in a future major Consul version when support for legacy ACLs is removed.

          This endpoint translates the legacy rules embedded within a legacy ACL into the latest syntax. It is intended to be used by operators managing Consul's ACLs and performing legacy token to new policy migrations. Note that this API requires the auto-generated Accessor ID of the legacy token. This ID can be retrieved using the /v1/acl/token/self endpoint."
           :path-params [accessor_id :- s/Uuid]
           :return s/Str
           :responses {200 {:description "The call was successful"}}
           (handler request))

         (POST "/login" request
           :operationId "login"
           :summary "Login to Auth Method"
           :description "This endpoint was added in Consul 1.5.0 and is used to exchange an auth method bearer token for a newly-created Consul ACL token."
           :body [req ACLLoginRequest]
           :return ACLLoginResponse
           :responses {200 {:description "The login was successful"}}
           (handler request))

         (POST "/logout" request
           :operationId "logout"
           :summary "Logout from Auth Method"
           :description "This endpoint was added in Consul 1.5.0 and is used to destroy a token created via the login endpoint. The token deleted is specified with the X-Consul-Token header or the token query parameter."
           :responses {200 {:description "The logout was successful."}}
           (handler request))

         (PUT "/token" request
           :operationId "createToken"
           :summary "Create a Token"
           :description "This endpoint creates a new ACL token."
           :body [r ACLTokenRequest]
           (handler request)))

       (context "/agent" []
         :tags ["Agent"]
         (GET "/members" request
           :operationId "listMembers"
           :summary "List Members"
           :description "This endpoint returns the members the agent sees in the cluster gossip pool. Due to the nature of gossip, this is eventually consistent: the results may differ by agent. The strongly consistent view of nodes is instead provided by /v1/catalog/nodes."
           :query-params [{wan :- s/Bool false}
                          {segment :- s/Str ""}]
           :return [AgentMember]
           (handler request))

         (GET "/self" request
           :operationId "readConfiguration"
           :summary "Read Configuration"
           :description "This endpoint returns the configuration and member information of the local agent. The Config element contains a subset of the configuration and its format will not change in a backwards incompatible way between releases. DebugConfig contains the full runtime configuration but its format is subject to change without notice or deprecation."
           :return AgentConfiguration
           (handler request))

         (PUT "/reload" request
           :operationId "reloadAgent"
           :summary "Reload Agent"
           :description "This endpoint instructs the agent to reload its configuration. Any errors encountered during this process are returned.

          Not all configuration options are reloadable. See the Reloadable Configuration section on the agent options page for details on which options are supported."
           (handler request))

         (PUT "/maintenance" request
           :operationId "enableMaintenanceMode"
           :summary "Enable Maintenance Mode"
           :description "This endpoint places the agent into \"maintenance mode\". During maintenance mode, the node will be marked as unavailable and will not be present in DNS or API queries. This API call is idempotent.

          Maintenance mode is persistent and will be automatically restored on agent restart."
           (handler request))

         (GET "/metrics" request
           :operationId "getMetrics"
           :summary "View Metrics"
           :description "This endpoint will dump the metrics for the most recent finished interval. For more information about metrics, see the telemetry page.

          In order to enable Prometheus support, you need to use the configuration directive prometheus_retention_time.

          Note: If your metric includes labels that use the same key name multiple times (i.e. tag=tag2 and tag=tag1), only the sorted last value (tag=tag2) will be visible on this endpoint due to a display issue. The complete label set is correctly applied and passed to external metrics providers even though it is not visible through this endpoint."
           :return AgentMetrics
           (handler request))

         (GET "/monitor" request
           :operationId "streamLogs"
           :summary "Stream Logs"
           :description "This endpoint streams logs from the local agent until the connection is closed."
           :query-params [{loglevel :- s/Str "info"}]
           :return s/Str
           (handler request))

         (PUT "/join/:address" request
           :operationId "agentJoin"
           :summary "Join Agent"
           :description "This endpoint instructs the agent to attempt to connect to a given address."
           :path-params [address :- (field s/Str {:description "Specifies the address of the other agent to join."})]
           :query-params [{wan :- (field s/Bool {:description "Specifies to try and join over the WAN pool. This is only optional for agents running in server mode."}) false}]
           (handler request))

         (PUT "/leave" request
           :operationId "agentLeave"
           :summary "Graceful Leave and Shutdown"
           :description "This endpoint triggers a graceful leave and shutdown of the agent. It is used to ensure other nodes see the agent as \"left\" instead of \"failed\". Nodes that leave will not attempt to re-join the cluster on restarting with a snapshot.

          For nodes in server mode, the node is removed from the Raft peer set in a graceful manner. This is critical, as in certain situations a non-graceful leave can affect cluster availability."
           (handler request))

         (PUT "/force-leave/:node" request
           :operationId "forceAgentLeave"
           :summary "Force Leave and Shutdown"
           :description "This endpoint instructs the agent to force a node into the left state. If a node fails unexpectedly, then it will be in a failed state. Once in the failed state, Consul will attempt to reconnect, and the services and checks belonging to that node will not be cleaned up. Forcing a node into the left state allows its old entries to be removed."
           :path-params [node :- (field s/Str {:description "Specifies the name of the node to be forced into left state."})]
           (handler request))

         (PUT "/agent/token/:token_name" request
           :operationId "updateACLTokens"
           :summary "Update ACL Tokens"
           :description "This endpoint updates the ACL tokens currently in use by the agent. It can be used to introduce ACL tokens to the agent for the first time, or to update tokens that were initially loaded from the agent's configuration. Tokens will be persisted only if the acl.enable_token_persistence configuration is true. When not being persisted, they will need to be reset if the agent is restarted."
           :path-params [token_name :- (s/enum :default :agent :agent_master :replication :acl_token :acl_agent_token :acl_agent_master_token :acl_replication_token)]
           :body [req {:Token (field s/Uuid {:description "Specifies the ACL token to set"})}]
           (handler request))

         (GET "/checks" request
           :operationId "agentListChecks"
           :summary "List Checks"
           :description "This endpoint returns all checks that are registered with the local agent. These checks were either provided through configuration files or added dynamically using the HTTP API.

          It is important to note that the checks known by the agent may be different from those reported by the catalog. This is usually due to changes being made while there is no leader elected. The agent performs active anti-entropy, so in most situations everything will be in sync within a few seconds."
           :query-params [{filter :- (field s/Str {:description "Specifies the expression used to filter the queries results prior to returning the data"}) ""}]
           :return AgentChecks
           (handler request))

         (PUT "/check/register" request
           :operationId "agentRegisterCheck"
           :summary "Register Check"
           :description "This endpoint adds a new check to the local agent. Checks may be of script, HTTP, TCP, or TTL type. The agent is responsible for managing the status of the check and keeping the Catalog in sync."
           :body [r RegisterCheckRequest]
           (handler request))

         (PUT "/check/deregister/:check_id" request
           :operationId "agentDeregisterCheck"
           :summary "Deregister Check"
           :description "This endpoint remove a check from the local agent. The agent will take care of deregistering the check from the catalog. If the check with the provided ID does not exist, no action is taken."
           :path-params [check_id :- (field s/Str {:description "Specifies the unique ID of the check to deregister."})]
           (handler request))

         (PUT "/check/pass/:check_id" request
           :operationId "agentPassTTLCheck"
           :summary "TTL Check Pass"
           :description "This endpoint is used with a TTL type check to set the status of the check to passing and to reset the TTL clock."
           :path-params [check_id :- (field s/Str {:description "Specifies the unique ID of the check to use."})]
           :query-params [{note :- (field s/Str {:description "Specifies a human-readable message."}) ""}]
           (handler request))

         (PUT "/check/warn/:check_id" request
           :operationId "agentWarnTTLCheck"
           :summary "TTL Check Warn"
           :description "This endpoint is used with a TTL type check to set the status of the check to warning and to reset the TTL clock."
           :path-params [check_id :- (field s/Str {:description "Specifies the unique ID of the check to use."})]
           :query-params [{note :- (field s/Str {:description "Specifies a human-readable message."}) ""}]
           (handler request))

         (PUT "/check/fail/:check_id" request
           :operationId "agentFailTTLCheck"
           :summary "TTL Check Fail"
           :description "This endpoint is used with a TTL type check to set the status of the check to critical and to reset the TTL clock."
           :path-params [check_id :- (field s/Str {:description "Specifies the unique ID of the check to use."})]
           :query-params [{note :- (field s/Str {:description "Specifies a human-readable message."}) ""}]
           (handler request))

         (PUT "/check/update/:check_id" request
           :operationId "agentUpdateTTLCheck"
           :summary "TTL Check Update"
           :description "This endpoint is used with a TTL type check to set the status of the check and to reset the TTL clock."
           :path-params [check_id :- (field s/Str {:description "Specifies the unique ID of the check to use."})]
           :body [r {(s/optional-key :Status) (field (s/enum :passing :warning :critical) {:description "Specifies the status of the check."})
                     (s/optional-key :Output) (field s/Str {:description "Specifies a human-readable message. This will be passed through to the check's Output field."})}]
           (handler request))

         (GET "/services" request
           :operationId "agentListServices"
           :summary "List Services"
           :description "This endpoint returns all the services that are registered with the local agent. These services were either provided through configuration files or added dynamically using the HTTP API.

           It is important to note that the services known by the agent may be different from those reported by the catalog. This is usually due to changes being made while there is no leader elected. The agent performs active anti-entropy, so in most situations everything will be in sync within a few seconds."
           :query-params [{filter :- (field s/Str {:description "Specifies the expression used to filter the queries results prior to returning the data."}) ""}]
           :return AgentServices
           (handler request))

         (GET "/service/:service_id" request
           :operationId "agentGetServiceConfiguration"
           :summary "Get Service Configuration"
           :description "This endpoint was added in Consul 1.3.0 and returns the full service definition for a single service instance registered on the local agent. It is used by Connect proxies to discover the embedded proxy configuration that was registered with the instance.

           It is important to note that the services known by the agent may be different from those reported by the catalog. This is usually due to changes being made while there is no leader elected. The agent performs active anti-entropy, so in most situations everything will be in sync within a few seconds."
           :path-params [service_id :- (field s/Str {:description "Specifies the ID of the service to fetch."})]
           :query-params [{index :- (field (s/maybe s/Int) {:description "Index to use for consul's blocking queries."}) nil}
                          {wait  :- (field (s/maybe s/Str) {:description "How long to wait for a blocking query"}) nil}]
           :return AgentServiceConfiguration
           (handler request))

         (GET "/health/service/name/:service_name" request
           :operationId "agentGetLocalServiceHealth"
           :summary "Get local service health"
           :description "Retrieve an aggregated state of service(s) on the local agent by name.

           This endpoints support JSON format and text/plain formats, JSON being the default. In order to get the text format, you can append ?format=text to the URL or use Mime Content negotiation by specifying a HTTP Header Accept starting with text/plain."
           :path-params [service_name :- (field s/Str {:description "The service name."})]
           :query-params [{format :- (s/maybe (s/enum :text)) nil}]
           :produces ["application/json" "text/plain"]
           :responses {200 {:description "All healthchecks of every matching service instance are passing"}
                       400 {:description "Bad parameter (missing service name of id)"}
                       404 {:description "No such service id or name"}
                       429 {:description "Some healthchecks are passing, at least one is warning"}
                       503 {:description "At least one of the healthchecks is critical"}}
           :return AgentServicesHealth
           (handler request))

         (PUT "/service/register" request
           :operationId "agentRegisterService"
           :summary "Register Service"
           :description "This endpoint adds a new service, with an optional health check, to the local agent.

           The agent is responsible for managing the status of its local services, and for sending updates about its local services to the servers to keep the global catalog in sync.

           For \"connect-proxy\" kind services, the service:write ACL for the Proxy.DestinationServiceName value is also required to register the service."
           :body [r AgentRegisterRequest]
           (handler request))

         (PUT "/service/deregister/:service_id" request
           :operationId "agentDeregisterService"
           :summary "Deregister Service"
           :description "This endpoint removes a service from the local agent. If the service does not exist, no action is taken.

           The agent will take care of deregistering the service with the catalog. If there is an associated check, that is also deregistered."
           :path-params [service_id :- (field s/Str {:description "Specifies the ID of the service to deregister"})]
           (handler request))

         (PUT "/service/maintenance/:service_id" request
           :operationId "agentEnableServiceMaintenanceMode"
           :summary "Enable Maintenance Mode"
           :description "This endpoint places a given service into \"maintenance mode\". During maintenance mode, the service will be marked as unavailable and will not be present in DNS or API queries. This API call is idempotent. Maintenance mode is persistent and will be automatically restored on agent restart."
           :path-params [service_id :- (field s/Str {:description "Specifies the ID of the service to put in maintenance mode."})]
           :query-params [enable :- s/Bool
                          {reason :- s/Str ""}]
           (handler request))

         (POST "/connect/authorize" request
           :operationId "agentAuthorizeConnect"
           :summary "Authorize"
           :description "This endpoint tests whether a connection attempt is authorized between two services. This is the primary API that must be implemented by proxies or native integrations that wish to integrate with Connect. Prior to calling this API, it is expected that the client TLS certificate has been properly verified against the current CA roots.


           The implementation of this API uses locally cached data and doesn't require any request forwarding to a server. Therefore, the response typically occurs in microseconds to impose minimal overhead on the connection attempt."
           :body [r ConnectAuthorizeRequest]
           :return ConnectAuthorizeResponse
           (handler request))

         (GET "/connect/ca/roots" request
           :operationId "getCertificateAuthorityRoots"
           :summary "Certificate Authority Roots"
           :description "This endpoint returns the trusted certificate authority (CA) root certificates. This is used by proxies or native integrations to verify served client or server certificates are valid.

           This is equivalent to the non-Agent Connect endpoint, but the response of this request is cached locally at the agent. This allows for very fast response times and for fail open behavior if the server is unavailable. This endpoint should be used by proxies and native integrations."
           :return CertificateAuthorityRoots
           :query-params [{index :- (field (s/maybe s/Int) {:description "Index to use for consul's blocking queries."}) nil}
                          {wait  :- (field (s/maybe s/Str) {:description "How long to wait for a blocking query"}) nil}]
           (handler request))

         (GET "/connect/ca/leaf/:service" request
           :operationId "getLeafCertificate"
           :summary "Service Leaf Certificate"
           :description "This endpoint returns the leaf certificate representing a single service. This certificate is used as a server certificate for accepting inbound connections and is also used as the client certificate for establishing outbound connections to other services.\n\nThe agent generates a CSR locally and calls the CA sign API to sign it. The resulting certificate is cached and returned by this API until it is near expiry or the root certificates change.\n\nThis API supports blocking queries. The blocking query will block until a new certificate is necessary because the existing certificate will expire or the root certificate is being rotated. This blocking behavior allows clients to efficiently wait for certificate rotations."
           :path-params [service :- (field s/Str {:description "The name of the service for the leaf certificate. The service does not need to exist in the catalog, but the proper ACL permissions must be available."})]
           :query-params [{index :- (field (s/maybe s/Int) {:description "Index to use for consul's blocking queries."}) nil}
                          {wait  :- (field (s/maybe s/Str) {:description "How long to wait for a blocking query"}) nil}]
           :return ServiceLeafCertificate
           (handler request)))

       (context "/catalog" []
         :tags ["Catalog"]
         (PUT "/register" request
           :operationId "catalogRegisterEntity"
           :summary "Register Entity"
           :description "This endpoint is a low-level mechanism for registering or updating entries in the catalog. It is usually preferable to instead use the agent endpoints for registration as they are simpler and perform anti-entropy."
           :body [r CatalogRegisterRequest]
           (handler request))

         (PUT "/deregister" request
           :operationId "catalogDeregisterEntity"
           :summary "Deregister Entity"
           :description "This endpoint is a low-level mechanism for directly removing entries from the Catalog. It is usually preferable to instead use the agent endpoints for deregistration as they are simpler and perform anti-entropy."
           :body [r CatalogDeregisterRequest]
           (handler request))

         (GET "/datacenters" request
           :operationId "listDatacenters"
           :summary "List Datacenters"
           :description "This endpoint returns the list of all known datacenters. The datacenters will be sorted in ascending order based on the estimated median round trip time from the server to the servers in that datacenter.

           This endpoint does not require a cluster leader and will succeed even during an availability outage. Therefore, it can be used as a simple check to see if any Consul servers are routable."
           :return [s/Str]
           (handler request))

         (GET "/nodes" request
           :operationId "catalogListNodes"
           :summary "List Nodes"
           :description "This endpoint and returns the nodes registered in a given datacenter."
           :query-params [{dc :- (field s/Str {:description "Specifies the datacenter to query. This will default to the datacenter of the agent being queried. "}) ""}
                          {near :- (field s/Str {:description "Specifies a node name to sort the node list in ascending order based on the estimated round trip time from that node. Passing ?near=_agent will use the agent's node for the sort."}) ""}
                          {node-meta :- (field s/Str {:description "Specifies a desired node metadata key/value pair of the form key:value. This parameter can be specified multiple times, and will filter the results to nodes with the specified key/value pairs."}) ""}
                          {filter :- (field s/Str {:description "Specifies the expression used to filter the queries results prior to returning the data."}) ""}
                          {index :- (field (s/maybe s/Int) {:description "Index to use for consul's blocking queries."}) nil}
                          {wait  :- (field (s/maybe s/Str) {:description "How long to wait for a blocking query"}) nil}]
           :return [CatalogNode]
           (handler request))

         (GET "/services" request
           :operationId "catalogListServices"
           :summary "List Services"
           :description "This endpoint returns the services registered in a given datacenter."
           :query-params [{dc :- (field s/Str {:description "Specifies the datacenter to query. This will default to the datacenter of the agent being queried. "}) ""}
                          {node-meta :- (field s/Str {:description "Specifies a desired node metadata key/value pair of the form key:value. This parameter can be specified multiple times, and will filter the results to nodes with the specified key/value pairs."}) ""}
                          {index :- (field (s/maybe s/Int) {:description "Index to use for consul's blocking queries."}) nil}
                          {wait  :- (field (s/maybe s/Str) {:description "How long to wait for a blocking query"}) nil}]
           :return {s/Str [s/Str]}
           (handler request))

         (GET "/service/:service" request
           :operationId "catalogListServiceNodes"
           :summary "List Nodes for Service"
           :description "This endpoint returns the nodes providing a service in a given datacenter."
           :path-params [service :- (field s/Str {:description "Specifies the name of the service for which to list nodes"})]
           :query-params [{dc :- (field s/Str {:description "Specifies the datacenter to query. This will default to the datacenter of the agent being queried. "}) ""}
                          {tag :- (field [s/Str] {:description "Specifies the tag to filter on. Can be used multiple times for additional filtering, returning only the results that include all of the tag values provided."}) []}
                          {near :- (field s/Str {:description "Specifies a node name to sort the node list in ascending order based on the estimated round trip time from that node. Passing ?near=_agent will use the agent's node for the sort."}) ""}
                          {node-meta :- (field s/Str {:description "Specifies a desired node metadata key/value pair of the form key:value. This parameter can be specified multiple times, and will filter the results to nodes with the specified key/value pairs."}) ""}
                          {filter :- (field s/Str {:description "Specifies the expression used to filter the queries results prior to returning the data."}) ""}
                          {index :- (field (s/maybe s/Int) {:description "Index to use for consul's blocking queries."}) nil}
                          {wait  :- (field (s/maybe s/Str) {:description "How long to wait for a blocking query"}) nil}]
           :return [CatalogServiceNode]
           (handler request))

         (GET "/connect/:service" request
           :operationId "catalogListConnectServiceNodes"
           :summary "List Nodes for Connect-capable Service"
           :description "This endpoint returns the nodes providing a Connect-capable service in a given datacenter. This will include both proxies and native integrations. A service may register both Connect-capable and incapable services at the same time, so this endpoint may be used to filter only the Connect-capable endpoints."
           :path-params [service :- (field s/Str {:description "Specifies the name of the service for which to list nodes"})]
           :query-params [{dc :- (field s/Str {:description "Specifies the datacenter to query. This will default to the datacenter of the agent being queried. "}) ""}
                          {tag :- (field [s/Str] {:description "Specifies the tag to filter on. Can be used multiple times for additional filtering, returning only the results that include all of the tag values provided."}) []}
                          {near :- (field s/Str {:description "Specifies a node name to sort the node list in ascending order based on the estimated round trip time from that node. Passing ?near=_agent will use the agent's node for the sort."}) ""}
                          {node-meta :- (field s/Str {:description "Specifies a desired node metadata key/value pair of the form key:value. This parameter can be specified multiple times, and will filter the results to nodes with the specified key/value pairs."}) ""}
                          {filter :- (field s/Str {:description "Specifies the expression used to filter the queries results prior to returning the data."}) ""}
                          {index :- (field (s/maybe s/Int) {:description "Index to use for consul's blocking queries."}) nil}
                          {wait  :- (field (s/maybe s/Str) {:description "How long to wait for a blocking query"}) nil}]
           :return [CatalogServiceNode]
           (handler request))

         (GET "/node/:node" request
           :operationId "catalogListNodeServices"
           :summary "List Services for Node"
           :description "This endpoint returns the node's registered services."
           :path-params [node :- (field s/Str {:description "Specifies the name of the node for which to list services."})]
           :query-params [{dc :- (field s/Str {:description "Specifies the datacenter to query. This will default to the datacenter of the agent being queried. "}) ""}
                          {filter :- (field s/Str {:description "Specifies the expression used to filter the queries results prior to returning the data."}) ""}
                          {index :- (field (s/maybe s/Int) {:description "Index to use for consul's blocking queries."}) nil}
                          {wait  :- (field (s/maybe s/Str) {:description "How long to wait for a blocking query"}) nil}]
           :return [{:Node CatalogNode
                     :Services {s/Str CatalogService}}]
           (handler request)))))))