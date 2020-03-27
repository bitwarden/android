using Bit.Core.Models.Domain;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text.RegularExpressions;

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

        public static string GetHostname(string uriString)
        {
            return GetUri(uriString)?.Host;
        }

        public static string GetHost(string uriString)
        {
            var uri = GetUri(uriString);
            if (uri != null)
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

        private static Uri GetUri(string uriString)
        {
            if (string.IsNullOrWhiteSpace(uriString))
            {
                return null;
            }
            var httpUrl = uriString.StartsWith("https://") || uriString.StartsWith("http://");
            if (!httpUrl && !uriString.Contains("://") && Regex.IsMatch(uriString, TldEndingRegex))
            {
                uriString = "http://" + uriString;
            }
            if (Uri.TryCreate(uriString, UriKind.Absolute, out var uri))
            {
                return uri;
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
            var dict = new Dictionary<string, string>();
            if (!Uri.TryCreate(urlString, UriKind.Absolute, out var uri) || string.IsNullOrWhiteSpace(uri.Query))
            {
                return dict;
            }
            var pairs = uri.Query.Substring(1).Split('&');
            foreach (var pair in pairs)
            {
                var parts = pair.Split('=');
                if (parts.Length < 1)
                {
                    continue;
                }
                var key = System.Net.WebUtility.UrlDecode(parts[0]).ToLower();
                if (!dict.ContainsKey(key))
                {
                    dict.Add(key, parts[1] == null ? string.Empty : System.Net.WebUtility.UrlDecode(parts[1]));
                }
            }
            return dict;
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
    }
}
