using System;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Exceptions;
using Newtonsoft.Json.Linq;

namespace Bit.Core.Services.EmailForwarders
{
    public class FastmailForwarder : BaseForwarder<ForwarderOptions>
    {
        protected override string RequestUri => "https://api.fastmail.com/jmap/api/";

        protected override void ConfigureHeaders(HttpRequestHeaders headers, ForwarderOptions options)
        {
            headers.Add("Authorization", $"Bearer {options.ApiKey}");
        }

        protected override async Task<HttpContent> GetContentAsync(IApiService apiService, ForwarderOptions options)
        {
            string accountId = null;
            try
            {
                accountId = await apiService.GetFastmailAccountIdAsync(options.ApiKey);
            }
            catch (ApiException ex)
            {
                if (IsRequestSecretInvalid(ex))
                {
                    throw new ForwardedEmailInvalidSecretException(ex);
                }

                throw;
            }

            var requestJObj = new JObject
            {
                new JProperty("using",
                    new JArray { "https://www.fastmail.com/dev/maskedemail", "urn:ietf:params:jmap:core" }),
                new JProperty("methodCalls",
                    new JArray
                    {
                        new JArray
                        {
                            "MaskedEmail/set",
                            new JObject
                            {
                                ["accountId"] = accountId,
                                ["create"] = new JObject
                                {
                                    ["new-masked-email"] = new JObject
                                    {
                                        ["state"] = "enabled",
                                        ["description"] = "",
                                        ["url"] = "",
                                        ["emailPrefix"] = ""
                                    }
                                }
                            },
                            "0"
                        }
                    })
            };

            return new StringContent(requestJObj.ToString(), Encoding.UTF8, "application/json");
        }

        protected override string HandleResponse(JObject result)
        {
            if (result["methodResponses"] == null || !result["methodResponses"].HasValues ||
                !result["methodResponses"][0].HasValues)
            {
                throw new Exception("Fastmail error: could not parse response.");
            }
            if (result["methodResponses"][0][0].ToString() == "MaskedEmail/set")
            {
                if (result["methodResponses"][0][1]?["created"]?["new-masked-email"] != null)
                {
                    return result["methodResponses"][0][1]?["created"]?["new-masked-email"]?["email"].ToString();
                }
                if (result["methodResponses"][0][1]?["notCreated"]?["new-masked-email"] != null)
                {
                    throw new Exception("Fastmail error: " +
                                        result["methodResponses"][0][1]?["created"]?["new-masked-email"]?["description"].ToString());
                }
            }
            else if (result["methodResponses"][0][0].ToString() == "error")
            {
                throw new Exception("Fastmail error: " + result["methodResponses"][0][1]?["description"].ToString());
            }
            throw new Exception("Fastmail error: could not parse response.");
        }

        protected override bool IsRequestSecretInvalid(ApiException ex) => base.IsRequestSecretInvalid(ex) || ex.Error?.StatusCode == System.Net.HttpStatusCode.Forbidden;
    }
}
