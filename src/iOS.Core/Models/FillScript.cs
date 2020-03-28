using System;
using System.Collections.Generic;
using System.Linq;
using Newtonsoft.Json;
using System.Text.RegularExpressions;

namespace Bit.iOS.Core.Models
{
    public class FillScript
    {
        private static string[] _usernameFieldNames = new[]{ "username", "user name", "email",
            "email address", "e-mail", "e-mail address", "userid", "user id" };

        public FillScript(PageDetails pageDetails, string fillUsername, string fillPassword,
            List<Tuple<string, string>> fillFields)
        {
            if (pageDetails == null)
            {
                return;
            }

            DocumentUUID = pageDetails.DocumentUUID;

            var filledFields = new Dictionary<string, PageDetails.Field>();

            if (fillFields?.Any() ?? false)
            {
                var fieldNames = fillFields.Select(f => f.Item1?.ToLower()).ToArray();
                foreach (var field in pageDetails.Fields.Where(f => f.Viewable))
                {
                    if (filledFields.ContainsKey(field.OpId))
                    {
                        continue;
                    }

                    var matchingIndex = FindMatchingFieldIndex(field, fieldNames);
                    if (matchingIndex > -1)
                    {
                        filledFields.Add(field.OpId, field);
                        Script.Add(new List<string> { "click_on_opid", field.OpId });
                        Script.Add(new List<string> { "fill_by_opid", field.OpId, fillFields[matchingIndex].Item2 });
                    }
                }
            }

            if (string.IsNullOrWhiteSpace(fillPassword))
            {
                // No password for this login. Maybe they just wanted to auto-fill some custom fields?
                SetFillScriptForFocus(filledFields);
                return;
            }

            List<PageDetails.Field> usernames = new List<PageDetails.Field>();
            List<PageDetails.Field> passwords = new List<PageDetails.Field>();

            var passwordFields = pageDetails.Fields.Where(f => f.Type == "password" && f.Viewable).ToArray();
            if (!passwordFields.Any())
            {
                // not able to find any viewable password fields. maybe there are some "hidden" ones?
                passwordFields = pageDetails.Fields.Where(f => f.Type == "password").ToArray();
            }

            foreach (var form in pageDetails.Forms)
            {
                var passwordFieldsForForm = passwordFields.Where(f => f.Form == form.Key).ToArray();
                passwords.AddRange(passwordFieldsForForm);

                if (string.IsNullOrWhiteSpace(fillUsername))
                {
                    continue;
                }

                foreach (var pf in passwordFieldsForForm)
                {
                    var username = FindUsernameField(pageDetails, pf, false, true);
                    if (username == null)
                    {
                        // not able to find any viewable username fields. maybe there are some "hidden" ones?
                        username = FindUsernameField(pageDetails, pf, true, true);
                    }

                    if (username != null)
                    {
                        usernames.Add(username);
                    }
                }
            }

            if (passwordFields.Any() && !passwords.Any())
            {
                // The page does not have any forms with password fields. Use the first password field on the page and the
                // input field just before it as the username.

                var pf = passwordFields.First();
                passwords.Add(pf);

                if (!string.IsNullOrWhiteSpace(fillUsername) && pf.ElementNumber > 0)
                {
                    var username = FindUsernameField(pageDetails, pf, false, false);
                    if (username == null)
                    {
                        // not able to find any viewable username fields. maybe there are some "hidden" ones?
                        username = FindUsernameField(pageDetails, pf, true, false);
                    }

                    if (username != null)
                    {
                        usernames.Add(username);
                    }
                }
            }

            if (!passwordFields.Any())
            {
                // No password fields on this page. Let's try to just fuzzy fill the username.
                var usernameFieldNamesList = _usernameFieldNames.ToList();
                foreach (var f in pageDetails.Fields)
                {
                    if (f.Viewable && (f.Type == "text" || f.Type == "email" || f.Type == "tel") &&
                        FieldIsFuzzyMatch(f, usernameFieldNamesList))
                    {
                        usernames.Add(f);
                    }
                }
            }

            foreach (var username in usernames.Where(u => !filledFields.ContainsKey(u.OpId)))
            {
                filledFields.Add(username.OpId, username);
                Script.Add(new List<string> { "click_on_opid", username.OpId });
                Script.Add(new List<string> { "fill_by_opid", username.OpId, fillUsername });
            }

            foreach (var password in passwords.Where(p => !filledFields.ContainsKey(p.OpId)))
            {
                filledFields.Add(password.OpId, password);
                Script.Add(new List<string> { "click_on_opid", password.OpId });
                Script.Add(new List<string> { "fill_by_opid", password.OpId, fillPassword });
            }

            SetFillScriptForFocus(filledFields);
        }

        [JsonProperty(PropertyName = "script")]
        public List<List<string>> Script { get; set; } = new List<List<string>>();
        [JsonProperty(PropertyName = "documentUUID")]
        public object DocumentUUID { get; set; }
        [JsonProperty(PropertyName = "properties")]
        public object Properties { get; set; } = new object();
        [JsonProperty(PropertyName = "options")]
        public object Options { get; set; } = new { animate = false };
        [JsonProperty(PropertyName = "metadata")]
        public object MetaData { get; set; } = new object();

        private PageDetails.Field FindUsernameField(PageDetails pageDetails, PageDetails.Field passwordField, bool canBeHidden,
            bool checkForm)
        {
            PageDetails.Field usernameField = null;

            foreach (var f in pageDetails.Fields)
            {
                if (f.ElementNumber >= passwordField.ElementNumber)
                {
                    break;
                }

                if ((!checkForm || f.Form == passwordField.Form)
                    && (canBeHidden || f.Viewable)
                    && f.ElementNumber < passwordField.ElementNumber
                    && (f.Type == "text" || f.Type == "email" || f.Type == "tel"))
                {
                    usernameField = f;

                    if (FindMatchingFieldIndex(f, _usernameFieldNames) > -1)
                    {
                        // We found an exact match. No need to keep looking.
                        break;
                    }
                }
            }

            return usernameField;
        }

        private int FindMatchingFieldIndex(PageDetails.Field field, string[] names)
        {
            var matchingIndex = -1;
            if (!string.IsNullOrWhiteSpace(field.HtmlId))
            {
                matchingIndex = Array.IndexOf(names, field.HtmlId.ToLower());
            }
            if (matchingIndex < 0 && !string.IsNullOrWhiteSpace(field.HtmlName))
            {
                matchingIndex = Array.IndexOf(names, field.HtmlName.ToLower());
            }
            if (matchingIndex < 0 && !string.IsNullOrWhiteSpace(field.LabelTag))
            {
                matchingIndex = Array.IndexOf(names, CleanLabel(field.LabelTag));
            }
            if (matchingIndex < 0 && !string.IsNullOrWhiteSpace(field.Placeholder))
            {
                matchingIndex = Array.IndexOf(names, field.Placeholder.ToLower());
            }

            return matchingIndex;
        }

        private bool FieldIsFuzzyMatch(PageDetails.Field field, List<string> names)
        {
            if (!string.IsNullOrWhiteSpace(field.HtmlId) && FuzzyMatch(names, field.HtmlId.ToLower()))
            {
                return true;
            }
            if (!string.IsNullOrWhiteSpace(field.HtmlName) && FuzzyMatch(names, field.HtmlName.ToLower()))
            {
                return true;
            }
            if (!string.IsNullOrWhiteSpace(field.LabelTag) && FuzzyMatch(names, CleanLabel(field.LabelTag)))
            {
                return true;
            }
            if (!string.IsNullOrWhiteSpace(field.Placeholder) && FuzzyMatch(names, field.Placeholder.ToLower()))
            {
                return true;
            }

            return false;
        }

        private bool FuzzyMatch(List<string> options, string value)
        {
            if ((!options?.Any() ?? true) || string.IsNullOrWhiteSpace(value))
            {
                return false;
            }

            return options.Any(o => value.Contains(o));
        }

        private void SetFillScriptForFocus(IDictionary<string, PageDetails.Field> filledFields)
        {
            if (!filledFields.Any())
            {
                return;
            }

            PageDetails.Field lastField = null, lastPasswordField = null;
            foreach (var field in filledFields)
            {
                if (field.Value.Viewable)
                {
                    lastField = field.Value;
                    if (field.Value.Type == "password")
                    {
                        lastPasswordField = field.Value;
                    }
                }
            }

            // Prioritize password field over others.
            if (lastPasswordField != null)
            {
                Script.Add(new List<string> { "focus_by_opid", lastPasswordField.OpId });
            }
            else if (lastField != null)
            {
                Script.Add(new List<string> { "focus_by_opid", lastField.OpId });
            }
        }

        private string CleanLabel(string label)
        {
            return Regex.Replace(label, @"(?:\r\n|\r|\n)", string.Empty).Trim().ToLower();
        }
    }
}
