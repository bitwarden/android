using System;
using System.Collections.Generic;
using System.Linq;
using Newtonsoft.Json;

namespace Bit.iOS.Extension.Models
{
    public class FillScript
    {
        public FillScript(PageDetails pageDetails, string fillUsername, string fillPassword)
        {
            if(pageDetails == null)
            {
                return;
            }

            DocumentUUID = pageDetails.DocumentUUID;

            var loginForm = pageDetails.Forms.FirstOrDefault(form => pageDetails.Fields.Any(f => f.Form == form.Key && f.Type == "password")).Value;
            if(loginForm == null)
            {
                return;
            }

            Script = new List<List<string>>();

            var password = pageDetails.Fields.FirstOrDefault(f =>
                f.Form == loginForm.OpId
                && f.Type == "password");

            var username = pageDetails.Fields.LastOrDefault(f =>
                f.Form == loginForm.OpId
                && (f.Type == "text" || f.Type == "email")
                && f.ElementNumber < password.ElementNumber);

            if(username != null)
            {
                Script.Add(new List<string> { "click_on_opid", username.OpId });
                Script.Add(new List<string> { "fill_by_opid", username.OpId, fillUsername });
            }

            Script.Add(new List<string> { "click_on_opid", password.OpId });
            Script.Add(new List<string> { "fill_by_opid", password.OpId, fillPassword });

            if(loginForm.HtmlAction != null)
            {
                AutoSubmit = new Submit { FocusOpId = password.OpId };
            }
        }

        [JsonProperty(PropertyName = "script")]
        public List<List<string>> Script { get; set; }
        [JsonProperty(PropertyName = "autosubmit")]
        public Submit AutoSubmit { get; set; }
        [JsonProperty(PropertyName = "documentUUID")]
        public object DocumentUUID { get; set; }
        [JsonProperty(PropertyName = "properties")]
        public object Properties { get; set; } = new object();
        [JsonProperty(PropertyName = "options")]
        public object Options { get; set; } = new object();
        [JsonProperty(PropertyName = "metadata")]
        public object MetaData { get; set; } = new object();

        public class Submit
        {
            [JsonProperty(PropertyName = "focusOpid")]
            public string FocusOpId { get; set; }
        }
    }

}
