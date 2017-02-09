using Bit.App.Models;

namespace Bit.iOS.Extension.Models
{
    public class LoginViewModel
    {
        public LoginViewModel(Login login)
        {
            Id = login.Id;
            Name = login.Name?.Decrypt();
            Username = login.Username?.Decrypt();
            Password = login.Password?.Decrypt();
            Uri = login.Uri?.Decrypt();
        }

        public string Id { get; set; }
        public string Name { get; set; }
        public string Username { get; set; }
        public string Password { get; set; }
        public string Uri { get; set; }
    }
}
