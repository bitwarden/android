namespace Bit.Droid.Accessibility
{
    public class KnownUsernameField
    {
        public KnownUsernameField(string uriAuthority, string uriPathEnd, string usernameViewId)
        {
            UriAuthority = uriAuthority;
            UriPathEnd = uriPathEnd;
            UsernameViewId = usernameViewId;
        }

        public string UriAuthority { get; set; }
        public string UriPathEnd { get; set; }
        public string UsernameViewId { get; set; }
    }
}
