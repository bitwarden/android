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
                if(Device.RuntimePlatform == Device.Android)
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
                if(Device.RuntimePlatform == Device.Android)
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

        public bool ShowLaunch
        {
            get
            {
                if(!ShowUri || !Uri.StartsWith("http"))
                {
                    return false;
                }

                Uri uri;
                if(!System.Uri.TryCreate(Uri, UriKind.Absolute, out uri))
                {
                    return false;
                }

                return true;
            }
        }

        public string UriHost
        {
            get
            {
                if(!ShowUri)
                {
                    return null;
                }

                Uri uri;
                if(!System.Uri.TryCreate(Uri, UriKind.Absolute, out uri))
                {
                    return Uri;
                }

                DomainName domain;
                if(DomainName.TryParse(uri.Host, out domain))
                {
                    return domain.BaseDomain;
                }

                return uri.Host;
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
            Name = login.Name?.Decrypt(login.OrganizationId);
            Username = login.Username?.Decrypt(login.OrganizationId);
            Password = login.Password?.Decrypt(login.OrganizationId);
            Uri = login.Uri?.Decrypt(login.OrganizationId);
            Notes = login.Notes?.Decrypt(login.OrganizationId);
        }
    }
}
