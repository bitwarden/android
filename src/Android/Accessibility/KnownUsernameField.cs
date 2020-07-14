namespace Bit.Droid.Accessibility
{
    public class KnownUsernameField
    {
        public KnownUsernameField(string uriAuthority, string[,] accessOptions)
        {
            UriAuthority = uriAuthority;
            AccessOptions = accessOptions;
        }

        public string UriAuthority { get; set; }
        public string[,] AccessOptions { get; set; }
    }
}
