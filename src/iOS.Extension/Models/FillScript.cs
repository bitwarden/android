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

            var passwordFields = pageDetails.Fields.Where(f => f.Type == "password").ToArray();
            var passwordForms = pageDetails.Forms.Where(form => passwordFields.Any(f => f.Form == form.Key)).ToArray();

            PageDetails.Form loginForm = null;
            PageDetails.Field username = null, password = null;

            if(passwordForms.Any())
            {
                if(passwordForms.Count() > 1)
                {
                    // More than one form with a password field is on the page.
                    // This usually occurs when a website has a login and signup form on the same page.
                    // Let's try to guess which one is the login form.

                    // First let's try to guess the correct login form by examining the form attribute strings
                    // for common login form attribute.
                    foreach(var form in passwordForms)
                    {
                        var formDescriptor = string.Format("{0}~{1}~{2}",
                            form.Value?.HtmlName, form.Value?.HtmlId, form.Value?.HtmlAction)
                            ?.ToLowerInvariant()?.Replace('_', '-');

                        if(formDescriptor.Contains("login") || formDescriptor.Contains("log-in")
                            || formDescriptor.Contains("signin") || formDescriptor.Contains("sign-in")
                            || formDescriptor.Contains("logon") || formDescriptor.Contains("log-on"))
                        {
                            loginForm = form.Value;
                            break;
                        }
                    }

                    if(loginForm == null)
                    {
                        // Next we can try to find the login form that only has one password field. Typically
                        // a registration form may have two password fields for password confirmation.
                        var fieldGroups = passwordFields.GroupBy(f => f.Form);
                        var singleFields = fieldGroups.FirstOrDefault(f => f.Count() == 1);
                        if(singleFields.Any())
                        {
                            var singlePasswordForms = passwordForms.Where(f => f.Key == singleFields.Key);
                            if(singlePasswordForms.Any())
                            {
                                loginForm = singlePasswordForms.First().Value;
                            }
                        }
                    }
                }

                if(loginForm == null)
                {
                    loginForm = passwordForms.FirstOrDefault().Value;
                }

                password = pageDetails.Fields.FirstOrDefault(f =>
                    f.Form == loginForm.OpId
                    && f.Type == "password");

                username = pageDetails.Fields.LastOrDefault(f =>
                    f.Form == loginForm.OpId
                    && (f.Type == "text" || f.Type == "email")
                    && f.ElementNumber < password.ElementNumber);

                if(loginForm.HtmlAction != null)
                {
                    AutoSubmit = new Submit { FocusOpId = password.OpId };
                }
            }
            else if(passwordFields.Count() == 1)
            {
                password = passwordFields.First();
                if(password.ElementNumber > 0)
                {
                    username = pageDetails.Fields[password.ElementNumber - 1];
                }
            }

            Script = new List<List<string>>();

            if(username != null)
            {
                Script.Add(new List<string> { "click_on_opid", username.OpId });
                Script.Add(new List<string> { "fill_by_opid", username.OpId, fillUsername });
            }

            if(password != null)
            {
                Script.Add(new List<string> { "click_on_opid", password.OpId });
                Script.Add(new List<string> { "fill_by_opid", password.OpId, fillPassword });
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
