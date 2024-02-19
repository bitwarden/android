using System;

namespace Bit.Droid.Accessibility
{
    public class Browser
    {
        public Browser(string packageName, string uriViewId)
        {
            PackageName = packageName;
            UriViewId = uriViewId;
        }

        public Browser(string packageName, string uriViewId, Func<string, string> getUriFunction)
            : this(packageName, uriViewId)
        {
            GetUriFunction = getUriFunction;
        }

        public string PackageName { get; set; }
        public string UriViewId { get; set; }
        public Func<string, string> GetUriFunction { get; set; } = (s) => s;
    }
}
