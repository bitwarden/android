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

            List<PageDetails.Field> usernames = new List<PageDetails.Field>();
            List<PageDetails.Field> passwords = new List<PageDetails.Field>();

            var passwordFields = pageDetails.Fields.Where(f => f.Type == "password" && f.Viewable).ToArray();
            if(!passwordFields.Any())
            {
                // not able to find any viewable password fields. maybe there are some "hidden" ones?
                passwordFields = pageDetails.Fields.Where(f => f.Type == "password").ToArray();
            }

            foreach(var form in pageDetails.Forms)
            {
                var passwordFieldsForForm = passwordFields.Where(f => f.Form == form.Key).ToArray();
                passwords.AddRange(passwordFieldsForForm);

                if(string.IsNullOrWhiteSpace(fillUsername))
                {
                    continue;
                }

                foreach(var pf in passwordFieldsForForm)
                {
                    var username = FindUsernameField(pageDetails, pf, false, true);
                    if(username == null)
                    {
                        // not able to find any viewable username fields. maybe there are some "hidden" ones?
                        username = FindUsernameField(pageDetails, pf, true, true);
                    }

                    if(username != null)
                    {
                        usernames.Add(username);
                    }
                }
            }

            if(passwordFields.Any() && !passwords.Any())
            {
                // The page does not have any forms with password fields. Use the first password field on the page and the
                // input field just before it as the username.

                var pf = passwordFields.First();
                passwords.Add(pf);

                if(!string.IsNullOrWhiteSpace(fillUsername) && pf.ElementNumber > 0)
                {
                    var username = FindUsernameField(pageDetails, pf, false, false);
                    if(username == null)
                    {
                        // not able to find any viewable username fields. maybe there are some "hidden" ones?
                        username = FindUsernameField(pageDetails, pf, true, false);
                    }

                    if(username != null)
                    {
                        usernames.Add(username);
                    }
                }
            }

            foreach(var username in usernames)
            {
                Script.Add(new List<string> { "click_on_opid", username.OpId });
                Script.Add(new List<string> { "fill_by_opid", username.OpId, fillUsername });
            }

            foreach(var password in passwords)
            {
                Script.Add(new List<string> { "click_on_opid", password.OpId });
                Script.Add(new List<string> { "fill_by_opid", password.OpId, fillPassword });
            }

            if(passwords.Any())
            {
                AutoSubmit = new Submit { FocusOpId = passwords.First().OpId };
            }
        }

        private PageDetails.Field FindUsernameField(PageDetails pageDetails, PageDetails.Field passwordField, bool canBeHidden,
            bool checkForm)
        {
            return pageDetails.Fields.LastOrDefault(f =>
                (!checkForm || f.Form == passwordField.Form)
                && (canBeHidden || f.Viewable)
                && f.ElementNumber < passwordField.ElementNumber
                && (f.Type == "text" || f.Type == "email" || f.Type == "tel"));
        }

        [JsonProperty(PropertyName = "script")]
        public List<List<string>> Script { get; set; } = new List<List<string>>();
        [JsonProperty(PropertyName = "autosubmit")]
        public Submit AutoSubmit { get; set; }
        [JsonProperty(PropertyName = "documentUUID")]
        public object DocumentUUID { get; set; }
        [JsonProperty(PropertyName = "properties")]
        public object Properties { get; set; } = new object();
        [JsonProperty(PropertyName = "options")]
        public object Options { get; set; } = new { animate = false };
        [JsonProperty(PropertyName = "metadata")]
        public object MetaData { get; set; } = new object();

        public class Submit
        {
            [JsonProperty(PropertyName = "focusOpid")]
            public string FocusOpId { get; set; }
        }
    }

}
