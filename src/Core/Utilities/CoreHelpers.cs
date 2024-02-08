using System;
using System.Collections.Generic;
using System.Drawing;
using System.Linq;
using System.Text.RegularExpressions;
using System.Web;
using Bit.Core.Models.Domain;
using Bit.Core.Services;
using Newtonsoft.Json;
using Color = Microsoft.Maui.Graphics.Color;

namespace Bit.Core.Utilities
{
    public static class CoreHelpers
    {
        public static readonly string IpRegex =
            "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
            "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
            "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
            "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";

        public static readonly string TldEndingRegex =
            ".*\\.(com|net|org|edu|uk|gov|ca|de|jp|fr|au|ru|ch|io|es|us|co|xyz|info|ly|mil)$";

        public static readonly DateTime Epoc = new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc);

        public static long EpocUtcNow()
        {
            return (long)(DateTime.UtcNow - Epoc).TotalMilliseconds;
        }

        public static bool InDebugMode()
        {
#if DEBUG
            return true;
#else
            return false;
#endif
        }

        /// <summary>
        /// Returns the host (and not port) of the given uri.
        /// Does not support plain hostnames without a protocol.
        /// 
        /// Input => Output examples:
        /// <para>https://bitwarden.com => bitwarden.com</para>
        /// <para>https://login.bitwarden.com:1337 => login.bitwarden.com</para>
        /// <para>https://sub.login.bitwarden.com:1337 => sub.login.bitwarden.com</para>
        /// <para>https://localhost:8080 => localhost</para>
        /// <para>localhost => null</para>
        /// <para>bitwarden => null</para>
        /// <para>127.0.0.1 => 127.0.0.1</para>
        /// </summary>
        public static string GetHostname(string uriString)
        {
            var uri = GetUri(uriString);
            return string.IsNullOrEmpty(uri?.Host) ? null : uri.Host;
        }

        /// <summary>
        /// Returns the host and port of the given uri.
        /// Does not support plain hostnames without 
        /// 
        /// Input => Output examples:
        /// <para>https://bitwarden.com => bitwarden.com</para>
        /// <para>https://login.bitwarden.com:1337 => login.bitwarden.com:1337</para>
        /// <para>https://sub.login.bitwarden.com:1337 => sub.login.bitwarden.com:1337</para>
        /// <para>https://localhost:8080 => localhost:8080</para>
        /// <para>localhost => null</para>
        /// <para>bitwarden => null</para>
        /// <para>127.0.0.1 => 127.0.0.1</para>
        /// </summary>
        public static string GetHost(string uriString)
        {
            var uri = GetUri(uriString);
            if (!string.IsNullOrEmpty(uri?.Host))
            {
                if (uri.IsDefaultPort)
                {
                    return uri.Host;
                }
                else
                {
                    return string.Format("{0}:{1}", uri.Host, uri.Port);
                }
            }
            return null;
        }

        /// <summary>
        /// Returns the second and top level domain of the given uri.
        /// Does not support plain hostnames without 
        /// 
        /// Input => Output examples:
        /// <para>https://bitwarden.com => bitwarden.com</para>
        /// <para>https://login.bitwarden.com:1337 => bitwarden.com</para>
        /// <para>https://sub.login.bitwarden.com:1337 => bitwarden.com</para>
        /// <para>https://localhost:8080 => localhost</para>
        /// <para>localhost => null</para>
        /// <para>bitwarden => null</para>
        /// <para>127.0.0.1 => 127.0.0.1</para>
        /// </summary>
        public static string GetDomain(string uriString)
        {
            var uri = GetUri(uriString);
            if (uri == null)
            {
                return null;
            }

            if (uri.Host == "localhost" || Regex.IsMatch(uri.Host, IpRegex))
            {
                return uri.Host;
            }
            try
            {
                if (DomainName.TryParseBaseDomain(uri.Host, out var baseDomain))
                {
                    return baseDomain ?? uri.Host;
                }
            }
            catch { }
            return null;
        }

        public static Uri GetUri(string uriString)
        {
            if (string.IsNullOrWhiteSpace(uriString))
            {
                return null;
            }
            var hasHttpProtocol = uriString.StartsWith("http://") || uriString.StartsWith("https://");
            if (!hasHttpProtocol && !uriString.Contains("://") && uriString.Contains("."))
            {
                if (Uri.TryCreate("http://" + uriString, UriKind.Absolute, out var uri))
                {
                    return uri;
                }
            }
            if (Uri.TryCreate(uriString, UriKind.Absolute, out var uri2))
            {
                return uri2;
            }
            return null;
        }

        public static void NestedTraverse<T>(List<TreeNode<T>> nodeTree, int partIndex, string[] parts,
            T obj, T parent, char delimiter) where T : ITreeNodeObject
        {
            if (parts.Length <= partIndex)
            {
                return;
            }

            var end = partIndex == parts.Length - 1;
            var partName = parts[partIndex];
            foreach (var n in nodeTree)
            {
                if (n.Node.Name != parts[partIndex])
                {
                    continue;
                }
                if (end && n.Node.Id != obj.Id)
                {
                    // Another node with the same name.
                    nodeTree.Add(new TreeNode<T>(obj, partName, parent));
                    return;
                }
                NestedTraverse(n.Children, partIndex + 1, parts, obj, n.Node, delimiter);
                return;
            }
            if (!nodeTree.Any(n => n.Node.Name == partName))
            {
                if (end)
                {
                    nodeTree.Add(new TreeNode<T>(obj, partName, parent));
                    return;
                }
                var newPartName = string.Concat(parts[partIndex], delimiter, parts[partIndex + 1]);
                var newParts = new List<string> { newPartName };
                var newPartsStartFrom = partIndex + 2;
                newParts.AddRange(new ArraySegment<string>(parts, newPartsStartFrom, parts.Length - newPartsStartFrom));
                NestedTraverse(nodeTree, 0, newParts.ToArray(), obj, parent, delimiter);
            }
        }

        public static TreeNode<T> GetTreeNodeObject<T>(List<TreeNode<T>> nodeTree, string id) where T : ITreeNodeObject
        {
            foreach (var n in nodeTree)
            {
                if (n.Node.Id == id)
                {
                    return n;
                }
                else if (n.Children != null)
                {
                    var node = GetTreeNodeObject(n.Children, id);
                    if (node != null)
                    {
                        return node;
                    }
                }
            }
            return null;
        }

        public static Dictionary<string, string> GetQueryParams(string urlString)
        {
            try
            {
                if (Uri.TryCreate(urlString, UriKind.Absolute, out var uri))
                {
                    return GetQueryParams(uri);
                }
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
            }
            return new Dictionary<string, string>();
        }

        public static Dictionary<string, string> GetQueryParams(Uri uri)
        {
            try
            {
                if (string.IsNullOrWhiteSpace(uri.Query))
                {
                    return new Dictionary<string, string>();
                }

                var queryStringNameValueCollection = HttpUtility.ParseQueryString(uri.Query);
                return queryStringNameValueCollection.AllKeys.Where(k => k != null).ToDictionary(k => k, k => queryStringNameValueCollection[k]);
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
            }
            return new Dictionary<string, string>();
        }

        public static string SerializeJson(object obj, bool ignoreNulls = false)
        {
            var jsonSerializationSettings = new JsonSerializerSettings();
            if (ignoreNulls)
            {
                jsonSerializationSettings.NullValueHandling = NullValueHandling.Ignore;
            }
            return JsonConvert.SerializeObject(obj, jsonSerializationSettings);
        }

        public static string SerializeJson(object obj, JsonSerializerSettings jsonSerializationSettings)
        {
            return JsonConvert.SerializeObject(obj, jsonSerializationSettings);
        }

        public static T DeserializeJson<T>(string json, bool ignoreNulls = false)
        {
            var jsonSerializationSettings = new JsonSerializerSettings();
            if (ignoreNulls)
            {
                jsonSerializationSettings.NullValueHandling = NullValueHandling.Ignore;
            }
            return JsonConvert.DeserializeObject<T>(json, jsonSerializationSettings);
        }

        public static string Base64UrlEncode(byte[] input)
        {
            var output = Convert.ToBase64String(input)
                .Replace('+', '-')
                .Replace('/', '_')
                .Replace("=", string.Empty);
            return output;
        }

        public static byte[] Base64UrlDecode(string input)
        {
            var output = input;
            // 62nd char of encoding
            output = output.Replace('-', '+');
            // 63rd char of encoding
            output = output.Replace('_', '/');
            // Pad with trailing '='s
            switch (output.Length % 4)
            {
                case 0:
                    // No pad chars in this case
                    break;
                case 2:
                    // Two pad chars
                    output += "=="; break;
                case 3:
                    // One pad char
                    output += "="; break;
                default:
                    throw new InvalidOperationException("Illegal base64url string!");
            }
            // Standard base64 decoder
            return Convert.FromBase64String(output);
        }

        public static T Clone<T>(T obj)
        {
            return JsonConvert.DeserializeObject<T>(JsonConvert.SerializeObject(obj));
        }

        public static string TextColorFromBgColor(string hexColor, int threshold = 166)
        {
            var isValidColor = Color.TryParse(hexColor, out var bgColor);
            if (isValidColor)
            {
                var luminance = (bgColor.Red * 255 * 0.299) + (bgColor.Green * 255 * 0.587) + (bgColor.Blue * 255 * 0.114);
                return luminance > threshold ? "#ff000000" : "#ffffffff";
            }
            else
            {
                return "#ff000000";
            }
        }

        public static string StringToColor(string str, string fallback)
        {
            if (str == null)
            {
                return fallback;
            }
            var hash = 0;
            for (var i = 0; i < str.Length; i++)
            {
                hash = str[i] + ((hash << 5) - hash);
            }
            var color = "#FF";
            for (var i = 0; i < 3; i++)
            {
                var value = (hash >> (i * 8)) & 0xff;
                color += Convert.ToString(value, 16).PadLeft(2, '0');
            }
            return color;
        }
    }
}
