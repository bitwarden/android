using System;
using System.Collections.Generic;

namespace Bit.Core.Models.Response
{
    public class ConfigResponse
    {
        public string Version { get; set; }
        public string GitHash { get; set; }
        public ServerConfigResponse Server { get; set; }
        public EnvironmentConfigResponse Environment { get; set; }
        public IDictionary<string, object> FeatureStates { get; set; }
        public DateTime ExpiresOn { get; set; }
    }

    public class ServerConfigResponse
    {
        public string Name { get; set; }
        public string Url { get; set; }
    }

    public class EnvironmentConfigResponse
    {
        public string Vault { get; set; }
        public string Api { get; set; }
        public string Identity { get; set; }
        public string Notifications { get; set; }
        public string Sso { get; set; }
    }
}

