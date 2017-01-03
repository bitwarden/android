using System;
using System.ComponentModel;
using Bit.App.Resources;
using Xamarin.Forms;

namespace Bit.App.Models.Page
{
    public class VaultViewLoginPageModel : INotifyPropertyChanged
    {
        private string _name;
        private string _username;
        private string _password;
        private string _uri;
        private string _notes;
        private bool _revealPassword;
        private string _uriHost;

        public VaultViewLoginPageModel() { }

        public event PropertyChangedEventHandler PropertyChanged;

        public string Name
        {
            get { return _name; }
            set
            {
                _name = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(Name)));
            }
        }
        public string Username
        {
            get { return _username; }
            set
            {
                _username = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(Username)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(ShowUsername)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(UsernameFontSize)));
            }
        }
        public bool ShowUsername => !string.IsNullOrWhiteSpace(Username);
        public double UsernameFontSize
        {
            get
            {
                if(Device.OS == TargetPlatform.Android)
                {
                    var length = Username?.Length ?? 0;

                    if(length > 35)
                    {
                        return Device.GetNamedSize(NamedSize.Micro, typeof(Label));
                    }
                    else if(length > 25)
                    {
                        return Device.GetNamedSize(NamedSize.Small, typeof(Label));
                    }
                }

                return Device.GetNamedSize(NamedSize.Medium, typeof(Label));
            }
        }

        public string Password
        {
            get { return _password; }
            set
            {
                _password = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(Password)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(MaskedPassword)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(ShowPassword)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(PasswordFontSize)));
            }
        }
        public bool ShowPassword => !string.IsNullOrWhiteSpace(Password);
        public double PasswordFontSize
        {
            get
            {
                if(Device.OS == TargetPlatform.Android)
                {
                    var length = Password?.Length ?? 0;

                    if(length > 25)
                    {
                        return Device.GetNamedSize(NamedSize.Micro, typeof(Label));
                    }
                    else if(length > 20)
                    {
                        return Device.GetNamedSize(NamedSize.Small, typeof(Label));
                    }
                }

                return Device.GetNamedSize(NamedSize.Medium, typeof(Label));
            }
        }

        public string Uri
        {
            get { return _uri; }
            set
            {
                _uri = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(Uri)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(UriHost)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(ShowUri)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(ShowLaunch)));
            }
        }
        public bool ShowUri => !string.IsNullOrWhiteSpace(Uri);
        public bool ShowLaunch => Uri.StartsWith("http://") || Uri.StartsWith("https://");

        public string UriHost
        {
            get
            {
                if(!ShowUri)
                {
                    return null;
                }

                if(_uriHost != null)
                {
                    return _uriHost;
                }

                try
                {
                    var host = new Uri(Uri).Host;

                    DomainName domain;
                    if(DomainName.TryParse(host, out domain))
                    {
                        _uriHost = domain.BaseDomain;
                    }
                    else
                    {
                        _uriHost = host;
                    }

                    return _uriHost;
                }
                catch
                {
                    return Uri;
                }
            }
        }

        public string Notes
        {
            get { return _notes; }
            set
            {
                _notes = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(Notes)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(ShowNotes)));
            }
        }
        public bool ShowNotes => !string.IsNullOrWhiteSpace(Notes);
        public bool RevealPassword
        {
            get { return _revealPassword; }
            set
            {
                _revealPassword = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(RevealPassword)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(MaskedPassword)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(ShowHideText)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(ShowHideImage)));
            }
        }
        public string MaskedPassword => RevealPassword ? Password : Password == null ? null : new string('●', Password.Length);
        public string ShowHideText => RevealPassword ? AppResources.Hide : AppResources.Show;
        public ImageSource ShowHideImage => RevealPassword ? ImageSource.FromFile("eye_slash") : ImageSource.FromFile("eye");

        public void Update(Login login)
        {
            Name = login.Name?.Decrypt();
            Username = login.Username?.Decrypt();
            Password = login.Password?.Decrypt();
            Uri = login.Uri?.Decrypt();
            Notes = login.Notes?.Decrypt();
        }
    }
}
