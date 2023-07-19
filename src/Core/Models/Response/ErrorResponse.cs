using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Text;
using Newtonsoft.Json.Linq;

namespace Bit.Core.Models.Response
{
    public class ErrorResponse
    {
        public ErrorResponse() { }

        public ErrorResponse(JObject response, HttpStatusCode status, bool identityResponse = false)
        {
            JObject errorModel = null;
            if (response != null)
            {
                var responseErrorModel = response.GetValue("ErrorModel", StringComparison.OrdinalIgnoreCase);
                if (responseErrorModel != null && identityResponse)
                {
                    errorModel = responseErrorModel.Value<JObject>(); ;
                }
                else
                {
                    errorModel = response;
                }
            }
            if (errorModel != null)
            {
                var model = errorModel.ToObject<ErrorModel>();
                Message = model.Message;
                ValidationErrors = model.ValidationErrors ?? new Dictionary<string, List<string>>();
                CaptchaSiteKey = ValidationErrors.ContainsKey("HCaptcha_SiteKey") ?
                    ValidationErrors["HCaptcha_SiteKey"]?.FirstOrDefault() :
                    null;
                CaptchaRequired = !string.IsNullOrWhiteSpace(CaptchaSiteKey);
            }
            else
            {
                if ((int)status == 429)
                {
                    Message = "Rate limit exceeded. Try again later.";
                }
            }
            StatusCode = status;
        }

        public string Message { get; set; }
        public Dictionary<string, List<string>> ValidationErrors { get; set; }
        public HttpStatusCode StatusCode { get; set; }
        public string CaptchaSiteKey { get; set; }
        public bool CaptchaRequired { get; set; } = false;

        public bool TlsClientAuthRequired { get; set; }

        public string GetSingleMessage()
        {
            if (ValidationErrors == null)
            {
                return Message;
            }
            foreach (var error in ValidationErrors)
            {
                if (error.Value?.Any() ?? false)
                {
                    return error.Value[0];
                }
            }
            return Message;
        }

        public string GetFullMessage()
        {
            string GetDefaultMessage() => $"{(int)StatusCode} {StatusCode}. {Message}";

            if (ValidationErrors is null)
            {
                return GetDefaultMessage();
            }

            var valErrors = ValidationErrors.Where(e => e.Value != null && e.Value.Any()).ToList();
            if (!valErrors.Any())
            {
                return GetDefaultMessage();
            }

            string GetFullError(string key, List<string> errors)
            {
                return new StringBuilder()
                    .Append($"[{key}]: ")
                    .Append(string.Join("; ", errors))
                    .ToString();
            };

            return $"{(int)StatusCode} {StatusCode}. {string.Join(Environment.NewLine, valErrors.Select(ve => GetFullError(ve.Key, ve.Value)))}";
        }

        private class ErrorModel
        {
            public string Message { get; set; }
            public Dictionary<string, List<string>> ValidationErrors { get; set; }
        }
    }
}
