namespace Bit.Droid.Accessibility
{
    public class KnownUsernameField
    {
        public KnownUsernameField(string uriAuthority, (string UriPathWanted, string UsernameViewId)[] accessOptions)
        {
            UriAuthority = uriAuthority;
            AccessOptions = accessOptions;
        }

        public string UriAuthority { get; set; }
        public (string UriPathWanted, string UsernameViewId)[] AccessOptions { get; set; }
    }
}
