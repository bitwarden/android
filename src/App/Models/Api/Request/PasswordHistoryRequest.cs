namespace Bit.App.Models.Api
{
    public class PasswordHistoryRequest
    {
        public string Password { get; set; }
        public System.DateTime LastUsedDate { get; set; }
    }
}
