using System;
using System.Collections.Generic;
using System.Text.RegularExpressions;

namespace Bit.App.Utilities
{
    public static class PasswordStrengthHelper
    {
        public static List<string> GetPasswordStrengthUserInput(string email)
        {
            var atPosition = email?.IndexOf('@');
            if (atPosition is null || atPosition < 0)
            {
                return null;
            }
            var rx = new Regex("/[^A-Za-z0-9]/", RegexOptions.Compiled);
            var data = rx.Split(email.Substring(0, atPosition.Value).Trim().ToLower());

            return new List<string>(data); ;
        }
    }
}

