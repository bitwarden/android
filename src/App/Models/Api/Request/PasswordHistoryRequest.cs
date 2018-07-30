namespace Bit.App.Models.Api
{
    public class PasswordHistoryRequest
    {
        public PasswordHistoryRequest(PasswordHistory ph)
        {
            Password = ph.Password?.EncryptedString;
            LastUsedDate = ph.LastUsedDate;
        }

        public string Password { get; set; }
        public System.DateTime LastUsedDate { get; set; }
    }
}
