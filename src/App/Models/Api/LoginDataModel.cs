namespace Bit.App.Models.Api
{
    public class LoginDataModel
    {
        public string Name { get; set; }
        public string Uri { get; set; }
        public string Username { get; set; }
        public string Password { get; set; }
        public string Notes { get; set; }
        public string Totp { get; set; }
    }
}
