(ns consul-api.core
  (:require [compojure.api.sweet :refer :all]
            [consul-api.domain :refer :all]
            [schema.core :as s]
            [ring.swagger.json-schema :refer [field convert-class]]))

(defmethod convert-class (type (byte-array 0))
  [_ _]
  {:type "string" :format "binary"})

(defn consul-api
  ([] (consul-api (constantly nil)))
  ([handler]
   (api
     :swagger {:spec "/swagger.json"
               :ui "/"
               :data {:info {:version "1"
                             :title "Consul API"}}
               :basePath "/v1"
               :host "localhost"
               :port 8500
               :scheme "http"
               :tags [{:name "ACL"
                       :description "The /acl endpoints are used to manage ACL tokens and policies in Consul, bootstrap the ACL system, check ACL replication status, and translate rules. There are additional pages for managing tokens and policies with the /acl endpoints.\n\nFor more information on how to setup ACLs, please see the ACL Guide."}
                      {:name "Agent"
                       :description "The /agent endpoints are used to interact with the local Consul agent. Usually, services and checks are registered with an agent which then takes on the burden of keeping that data synchronized with the cluster. For example, the agent registers services and checks with the Catalog and performs anti-entropy to recover from outages.\n\nIn addition to these endpoints, additional endpoints are grouped in the navigation for Checks and Services.\n\n"}
                      {:name "Catalog"
                       :description "The /catalog endpoints register and deregister nodes, services, and checks in Consul. The catalog should not be confused with the agent, since some of the API methods look similar."}
                      {:name "Config"
                       :description "The /config endpoints create, update, delete and query central configuration entries registered with Consul. See the agent configuration for more information on how to enable this functionality for centrally configuring services and configuration entries docs for a description of the configuration entries content."}
                      {:name "Health"
                       :description "The /health endpoints query health-related information. They are provided separately from the /catalog endpoints since users may prefer not to use the optional health checking mechanisms. Additionally, some of the query results from the health endpoints are filtered while the catalog endpoints provide the raw entries."}
                      {:name "KV Store"
                       :description "The /kv endpoints access Consul's simple key/value store, useful for storing service configuration or other metadata.\n\nIt is important to note that each datacenter has its own KV store, and there is no built-in replication between datacenters. If you are interested in replication between datacenters, please view the Consul Replicate project.\n\nValues in the KV store cannot be larger than 512kb.\nFor multi-key updates, please consider using transaction.\n\n"}
                      {:name "Session"
                       :description "The /session endpoints create, destroy, and query sessions in Consul."}]}

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
                          {wait  :- (field (s/maybe s/Str) {:description "How long to wait for a blocking query"}) nil}
                          {consistent :- (field s/Int {:description "Set consistent consistency mode."}) 0}
                          {stale :- (field s/Int {:description "Set stale consistency mode."}) 0}]
           :return [CatalogNode]
           (handler request))

         (GET "/services" request
           :operationId "catalogListServices"
           :summary "List Services"
           :description "This endpoint returns the services registered in a given datacenter."
           :query-params [{dc :- (field s/Str {:description "Specifies the datacenter to query. This will default to the datacenter of the agent being queried. "}) ""}
                          {node-meta :- (field s/Str {:description "Specifies a desired node metadata key/value pair of the form key:value. This parameter can be specified multiple times, and will filter the results to nodes with the specified key/value pairs."}) ""}
                          {index :- (field (s/maybe s/Int) {:description "Index to use for consul's blocking queries."}) nil}
                          {wait  :- (field (s/maybe s/Str) {:description "How long to wait for a blocking query"}) nil}
                          {consistent :- (field s/Int {:description "Set consistent consistency mode."}) 0}
                          {stale :- (field s/Int {:description "Set stale consistency mode."}) 0}]
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
                          {wait  :- (field (s/maybe s/Str) {:description "How long to wait for a blocking query"}) nil}
                          {consistent :- (field s/Int {:description "Set consistent consistency mode."}) 0}
                          {stale :- (field s/Int {:description "Set stale consistency mode."}) 0}]
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
                          {wait  :- (field (s/maybe s/Str) {:description "How long to wait for a blocking query"}) nil}
                          {consistent :- (field s/Int {:description "Set consistent consistency mode."}) 0}
                          {stale :- (field s/Int {:description "Set stale consistency mode."}) 0}]
           :return [{:Node CatalogNode
                     :Services {s/Str CatalogService}}]
           (handler request)))

       (PUT "/config" request
         :tags ["Config"]
         :operationId "applyConfiguration"
         :summary "Apply Configuration"
         :description "This endpoint creates or updates the given config entry."
         :query-params [{dc :- (field s/Str {:description "Specifies the datacenter to query. This will default to the datacenter of the agent being queried. "}) ""}
                        {cas :- (field s/Int {:description "Specifies to use a Check-And-Set operation. If the index is 0, Consul will only store the entry if it does not already exist. If the index is non-zero, the entry is only set if the current index matches the ModifyIndex of that entry."}) 0}]
         :body [r {s/Str s/Str}]
         (handler request))

       (context "/config" []
         :tags ["Config"]

         (GET "/config/:kind/:name" request
           :operationId "getConfiguration"
           :summary "Get Configuration"
           :description "This endpoint returns a specific config entry."
           :path-params [kind :- (field s/Str {:description "Specifies the kind of the entry to read."})
                         name :- (field s/Str {:description "Specifies the name of the entry to read."})]
           :query-params [{dc :- (field s/Str {:description "Specifies the datacenter to query. This will default to the datacenter of the agent being queried. "}) ""}
                          {index :- (field (s/maybe s/Int) {:description "Index to use for consul's blocking queries."}) nil}
                          {wait  :- (field (s/maybe s/Str) {:description "How long to wait for a blocking query"}) nil}
                          {consistent :- (field s/Int {:description "Set consistent consistency mode."}) 0}
                          {stale :- (field s/Int {:description "Set stale consistency mode."}) 0}]
           :return {s/Str s/Str}
           (handler request))

         (GET "/:kind" request
           :operationId "listConfigurations"
           :summary "List Configurations"
           :path-params [kind :- (field s/Str {:description "Specifies the kind of the entry to list."})]
           :query-params [{dc :- (field s/Str {:description "Specifies the datacenter to query. This will default to the datacenter of the agent being queried. "}) ""}
                          {index :- (field (s/maybe s/Int) {:description "Index to use for consul's blocking queries."}) nil}
                          {wait  :- (field (s/maybe s/Str) {:description "How long to wait for a blocking query"}) nil}
                          {consistent :- (field s/Int {:description "Set consistent consistency mode."}) 0}
                          {stale :- (field s/Int {:description "Set stale consistency mode."}) 0}]
           :return [{s/Str s/Str}]
           (handler request))

         (DELETE "/:kind/:name" request
           :operationId "deleteConfiguration"
           :summary "Delete Configuration"
           :description "This endpoint creates or updates the given config entry."
           :path-params [kind :- (field s/Str {:description "Specifies the kind of the entry to delete."})
                         name :- (field s/Str {:description "Specifies the name of the entry to delete."})]
           :query-params [{dc :- (field s/Str {:description "Specifies the datacenter to query. This will default to the datacenter of the agent being queried. "}) ""}]
           (handler request)))

       ; TODO Connect
       ; TODO Coordinates
       ; TODO Discovery Chain

       (context "/health" []
         :tags ["Health"]
         (GET "/node/:node" request
           :operationId "listNodeHealthChecks"
           :summary "List Checks for Node"
           :description "This endpoint returns the checks specific to the node provided on the path."
           :path-params [node :- (field s/Str {:description "Specifies the name or ID of the node to query"})]
           :query-params [{dc :- (field s/Str {:description "Specifies the datacenter to query. This will default to the datacenter of the agent being queried. "}) ""}
                          {filter :- (field s/Str {:description "Specifies the expression used to filter the queries results prior to returning the data."}) ""}
                          {index :- (field (s/maybe s/Int) {:description "Index to use for consul's blocking queries."}) nil}
                          {wait  :- (field (s/maybe s/Str) {:description "How long to wait for a blocking query"}) nil}
                          {consistent :- (field s/Int {:description "Set consistent consistency mode."}) 0}
                          {stale :- (field s/Int {:description "Set stale consistency mode."}) 0}]
           :return [NodeHealthCheck]
           (handler request))

         (GET "/checks/:service" request
           :operationId "listServiceHealthChecks"
           :summary "List Checks for Service"
           :description "This endpoint returns the checks associated with the service provided on the path."
           :path-params [service :- (field s/Str {:description "Specifies the service to list checks for."})]
           :query-params [{dc :- (field s/Str {:description "Specifies the datacenter to query. This will default to the datacenter of the agent being queried. "}) ""}
                          {filter :- (field s/Str {:description "Specifies the expression used to filter the queries results prior to returning the data."}) ""}
                          {near :- (field s/Str {:description "Specifies a node name to sort the node list in ascending order based on the estimated round trip time from that node. Passing ?near=_agent will use the agent's node for the sort."}) ""}
                          {node-meta :- (field s/Str {:description "Specifies a desired node metadata key/value pair of the form key:value. This parameter can be specified multiple times, and will filter the results to nodes with the specified key/value pairs."}) ""}
                          {index :- (field (s/maybe s/Int) {:description "Index to use for consul's blocking queries."}) nil}
                          {wait  :- (field (s/maybe s/Str) {:description "How long to wait for a blocking query"}) nil}
                          {consistent :- (field s/Int {:description "Set consistent consistency mode."}) 0}
                          {stale :- (field s/Int {:description "Set stale consistency mode."}) 0}]
           :return [NodeHealthCheck]
           (handler request))

         (GET "/service/:service" request
           :operationId "listNodesForService"
           :summary "List Nodes for Service"
           :description "This endpoint returns the nodes providing the service indicated on the path. Users can also build in support for dynamic load balancing and other features by incorporating the use of health checks."
           :path-params [service :- (field s/Str {:description "Specifies the service to list services for."})]
           :query-params [{dc :- (field s/Str {:description "Specifies the datacenter to query. This will default to the datacenter of the agent being queried. "}) ""}
                          {filter :- (field s/Str {:description "Specifies the expression used to filter the queries results prior to returning the data."}) ""}
                          {near :- (field s/Str {:description "Specifies a node name to sort the node list in ascending order based on the estimated round trip time from that node. Passing ?near=_agent will use the agent's node for the sort."}) ""}
                          {node-meta :- (field s/Str {:description "Specifies a desired node metadata key/value pair of the form key:value. This parameter can be specified multiple times, and will filter the results to nodes with the specified key/value pairs."}) ""}
                          {passing :- (field s/Bool {:description "Specifies that the server should return only nodes with all checks in the passing state. This can be used to avoid additional filtering on the client side."}) false}
                          {tag :- (field [s/Str] {:description "Specifies the tag to filter the list. Can be used multiple times for additional filtering, returning only the results that include all of the tag values provided."}) []}
                          {index :- (field (s/maybe s/Int) {:description "Index to use for consul's blocking queries."}) nil}
                          {wait  :- (field (s/maybe s/Str) {:description "How long to wait for a blocking query"}) nil}
                          {consistent :- (field s/Int {:description "Set consistent consistency mode."}) 0}
                          {stale :- (field s/Int {:description "Set stale consistency mode."}) 0}]
           :return [HealthService]
           (handler request))

         (GET "/connect/:service" request
           :operationId "listNodesForConnectService"
           :summary "List Nodes for Connect-capable Service"
           :description "This endpoint returns the nodes providing a Connect-capable service in a given datacenter. This will include both proxies and native integrations. A service may register both Connect-capable and incapable services at the same time, so this endpoint may be used to filter only the Connect-capable endpoints."
           :path-params [service :- (field s/Str {:description "Specifies the service to list services for."})]
           :query-params [{dc :- (field s/Str {:description "Specifies the datacenter to query. This will default to the datacenter of the agent being queried. "}) ""}
                          {filter :- (field s/Str {:description "Specifies the expression used to filter the queries results prior to returning the data."}) ""}
                          {near :- (field s/Str {:description "Specifies a node name to sort the node list in ascending order based on the estimated round trip time from that node. Passing ?near=_agent will use the agent's node for the sort."}) ""}
                          {node-meta :- (field s/Str {:description "Specifies a desired node metadata key/value pair of the form key:value. This parameter can be specified multiple times, and will filter the results to nodes with the specified key/value pairs."}) ""}
                          {passing :- (field s/Bool {:description "Specifies that the server should return only nodes with all checks in the passing state. This can be used to avoid additional filtering on the client side."}) false}
                          {tag :- (field [s/Str] {:description "Specifies the tag to filter the list. Can be used multiple times for additional filtering, returning only the results that include all of the tag values provided."}) []}
                          {index :- (field (s/maybe s/Int) {:description "Index to use for consul's blocking queries."}) nil}
                          {wait  :- (field (s/maybe s/Str) {:description "How long to wait for a blocking query"}) nil}
                          {consistent :- (field s/Int {:description "Set consistent consistency mode."}) 0}
                          {stale :- (field s/Int {:description "Set stale consistency mode."}) 0}]
           :return [HealthService]
           (handler request))

         (GET "/state/:state" request
           :operationId "listChecksInState"
           :summary "List Checks in State"
           :description "This endpoint returns the checks in the state provided on the path."
           :path-params [state :- (field (s/enum "any" "passing" "warning" "critical") {:description "Specifies the state to query."})]
           :query-params [{dc :- (field s/Str {:description "Specifies the datacenter to query. This will default to the datacenter of the agent being queried. "}) ""}
                          {filter :- (field s/Str {:description "Specifies the expression used to filter the queries results prior to returning the data."}) ""}
                          {near :- (field s/Str {:description "Specifies a node name to sort the node list in ascending order based on the estimated round trip time from that node. Passing ?near=_agent will use the agent's node for the sort."}) ""}
                          {node-meta :- (field s/Str {:description "Specifies a desired node metadata key/value pair of the form key:value. This parameter can be specified multiple times, and will filter the results to nodes with the specified key/value pairs."}) ""}
                          {index :- (field (s/maybe s/Int) {:description "Index to use for consul's blocking queries."}) nil}
                          {wait  :- (field (s/maybe s/Str) {:description "How long to wait for a blocking query"}) nil}
                          {consistent :- (field s/Int {:description "Set consistent consistency mode."}) 0}
                          {stale :- (field s/Int {:description "Set stale consistency mode."}) 0}]
           :return [NodeHealthCheck]))

       (context "/kv" []
         :tags ["KV Store"]
         (GET "/:key" request
           :operationId "readKey"
           :summary "Read Key"
           :description "This endpoint returns the specified key. If no key exists at the given path, a 404 is returned instead of a 200 response.\n\nFor multi-key reads, please consider using transaction."
           :path-params [key :- (field s/Str {:description "Specifies the path of the key to read."})]
           :query-params [{dc :- (field s/Str {:description "Specifies the datacenter to query. This will default to the datacenter of the agent being queried. "}) ""}
                          {recurse :- (field s/Bool {:description "Specifies if the lookup should be recursive and key treated as a prefix instead of a literal match."}) false}
                          {raw :- (field s/Bool {:description "Specifies the response is just the raw value of the key, without any encoding or metadata."}) false}
                          {keys :- (field s/Bool {:description "Specifies to return only keys (no values or metadata). Specifying this implies recurse."}) false}
                          {separator :- (field s/Str {:description "Specifies the string to use as a separator for recursive key lookups. This option is only used when paired with the keys parameter to limit the prefix of keys returned, only up to the given separator. "}) ""}
                          {index :- (field (s/maybe s/Int) {:description "Index to use for consul's blocking queries."}) nil}
                          {wait  :- (field (s/maybe s/Str) {:description "How long to wait for a blocking query"}) nil}
                          {consistent :- (field s/Int {:description "Set consistent consistency mode."}) 0}
                          {stale :- (field s/Int {:description "Set stale consistency mode."}) 0}]
           :return ReadKeyResponse
           (handler request))

         (PUT "/:key" request
           :operationId "writeKey"
           :summary "Create/Update Key"
           :description "This endpoint" ; upstream doc error
           :path-params [key :- (field s/Str {:description "Specifies the path of the key to read."})] ; upstream doc error
           :query-params [{dc :- (field s/Str {:description "Specifies the datacenter to query. This will default to the datacenter of the agent being queried."}) ""}
                          {flags :- (field s/Int {:description "Specifies an unsigned value between 0 and (2^64)-1. Clients can choose to use this however makes sense for their application."}) 0}
                          {cas :- (field s/Int {:description "Specifies to use a Check-And-Set operation. This is very useful as a building block for more complex synchronization primitives. If the index is 0, Consul will only put the key if it does not already exist. If the index is non-zero, the key is only set if the index matches the ModifyIndex of that key."}) 0}
                          {acquire :- (field s/Str {:description "Supply a session ID to use in a lock acquisition operation. This is useful as it allows leader election to be built on top of Consul. If the lock is not held and the session is valid, this increments the LockIndex and sets the Session value of the key in addition to updating the key contents. A key does not need to exist to be acquired. If the lock is already held by the given session, then the LockIndex is not incremented but the key contents are updated. This lets the current lock holder update the key contents without having to give up the lock and reacquire it. Note that an update that does not include the acquire parameter will proceed normally even if another session has locked the key.\n\nFor an example of how to use the lock feature, see the Leader Election Guide."}) ""}
                          {release :- (field s/Str {:description "Supply a session ID to use in a release operation. This is useful when paired with ?acquire= as it allows clients to yield a lock. This will leave the LockIndex unmodified but will clear the associated Session of the key. The key must be held by this session to be unlocked."}) ""}]
           :consumes ["application/octet-stream"]
           :body [r (type (byte-array 0))]
           :return s/Bool
           (handler request))

         (DELETE "/:key" request
           :operationId "deleteKey"
           :summary "Delete Key"
           :description "This endpoint deletes a single key or all keys sharing a prefix."
           :path-params [keys :- (field s/Str {:description ""})] ; upstream doc missing
           :query-params [{recurse :- (field s/Str {:description "Specifies to delete all keys which have the specified prefix. Without this, only a key with an exact match will be deleted."}) false}
                          {cas :- (field s/Int {:description "Specifies to use a Check-And-Set operation. This is very useful as a building block for more complex synchronization primitives. Unlike PUT, the index must be greater than 0 for Consul to take any action: a 0 index will not delete the key. If the index is non-zero, the key is only deleted if the index matches the ModifyIndex of that key."}) 0}]
           (handler request)))

       ; TODO operator API
       ; TODO prepared query API

       (context "/session" []
         :tags ["Session"]
         (PUT "/create" request
           :operationId "createSession"
           :summary "Create Session"
           :query-params [{dc :- (field s/Str {:description "Specifies the datacenter to query. This will default to the datacenter of the agent being queried. Using this across datacenters is not recommended."}) ""}]
           :body [r Session]
           :return {:ID (field s/Uuid {:description "the ID of the created session"})}
           (handler request))

         (DELETE "/destroy/:uuid" request
           :operationId "deleteSession"
           :summary "Delete Session"
           :description "This endpoint destroys the session with the given name. If the session UUID is malformed, an error is returned. If the session UUID does not exist or already expired, a 200 is still returned (the operation is idempotent)."
           :path-params [uuid :- (field s/Uuid {:description "Specifies the UUID of the session to destroy."})]
           :query-params [{dc :- (field s/Str {:description "Specifies the datacenter to query. This will default to the datacenter of the agent being queried. Using this across datacenters is not recommended."}) ""}]
           :return s/Bool
           (handler request))

         (GET "/info/:uuid" request
           :operationId "readSession"
           :summary "Read Session"
           :description "This endpoint returns the requested session information."
           :path-params [uuid :- (field s/Uuid {:description "Specifies the UUID of the session to read."})]
           :query-params [{dc :- (field s/Str {:description "Specifies the datacenter to query. This will default to the datacenter of the agent being queried. Using this across datacenters is not recommended."}) ""}]
           :return (s/maybe [Session])
           (handler request))

         (GET "/node/:node" request
           :operationId "listSessionsForNode"
           :summary "List Sessions for Node"
           :description "This endpoint returns the active sessions for a given node."
           :path-params [node :- (field s/Str {:description "Specifies the name or ID of the node to query."})]
           :query-params [{dc :- (field s/Str {:description "Specifies the datacenter to query. This will default to the datacenter of the agent being queried. Using this across datacenters is not recommended."}) ""}
                          {index :- (field (s/maybe s/Int) {:description "Index to use for consul's blocking queries."}) nil}
                          {wait  :- (field (s/maybe s/Str) {:description "How long to wait for a blocking query"}) nil}
                          {consistent :- (field s/Int {:description "Set consistent consistency mode."}) 0}
                          {stale :- (field s/Int {:description "Set stale consistency mode."}) 0}]
           :return [Session]
           (handler request))

         (GET "/list" request
           :operationId "listSessions"
           :summary "List Sessions"
           :description "This endpoint returns the list of active sessions."
           :query-params [{dc :- (field s/Str {:description "Specifies the datacenter to query. This will default to the datacenter of the agent being queried. Using this across datacenters is not recommended."}) ""}
                          {index :- (field (s/maybe s/Int) {:description "Index to use for consul's blocking queries."}) nil}
                          {wait  :- (field (s/maybe s/Str) {:description "How long to wait for a blocking query"}) nil}
                          {consistent :- (field s/Int {:description "Set consistent consistency mode."}) 0}
                          {stale :- (field s/Int {:description "Set stale consistency mode."}) 0}]
           :return [Session]
           (handler request))

         (PUT "/renew/:uuid" request
           :operationId "renewSession"
           :summary "Renew Session"
           :description "This endpoint renews the given session. This is used with sessions that have a TTL, and it extends the expiration by the TTL."
           :path-params [uuid :- (field s/Uuid {:description "Specifies the UUID of the session to renew."})]
           :query-params [{dc :- (field s/Str {:description "Specifies the datacenter to query. This will default to the datacenter of the agent being queried. Using this across datacenters is not recommended."}) ""}]
           :return [Session]
           (handler request)))

       ; TODO snapshot API

       (context "/status" []
         :tags ["Status"]
         (GET "/leader" request
           :operationId "getRaftLeader"
           :summary "Get Raft Leader"
           :description "This endpoint returns the Raft leader for the datacenter in which the agent is running."
           :query-params [{dc :- (field s/Str {:description "Specifies the datacenter to query. This will default to the datacenter of the agent being queried."}) ""}]
           :return s/Str
           (handler request))

         (GET "/peers" request
           :operationId "listRaftPeers"
           :summary "List Raft Peers"
           :description "This endpoint retrieves the Raft peers for the datacenter in which the the agent is running. This list of peers is strongly consistent and can be useful in determining when a given server has successfully joined the cluster."
           :query-params [{dc :- (field s/Str {:description "Specifies the datacenter to query. This will default to the datacenter of the agent being queried."}) ""}]
           :return [s/Str]
           (handler request)))

       (PUT "/txn" request
         :tags ["Transactions"]
         :operationId "transact"
         :summary "Create Transaction"
         :description "This endpoint permits submitting a list of operations to apply to Consul inside of a transaction. If any operation fails, the transaction is rolled back and none of the changes are applied.\n\nIf the transaction does not contain any write operations then it will be fast-pathed internally to an endpoint that works like other reads, except that blocking queries are not currently supported. In this mode, you may supply the ?stale or ?consistent query parameters with the request to control consistency. To support bounding the acceptable staleness of data, read-only transaction responses provide the X-Consul-LastContact header containing the time in milliseconds that a server was last contacted by the leader node. The X-Consul-KnownLeader header also indicates if there is a known leader. These won't be present if the transaction contains any write operations, and any consistency query parameters will be ignored, since writes are always managed by the leader via the Raft consensus protocol."
         :query-params [{dc :- (field s/Str {:description "Specifies the datacenter to query. This will default to the datacenter of the agent being queried."}) ""}]
         :body [r TransactionRequest]
         :return TransactionResponse
         (handler request))))))