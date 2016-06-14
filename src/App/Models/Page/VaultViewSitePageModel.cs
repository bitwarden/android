using System;
using System.ComponentModel;
using Bit.App.Resources;

namespace Bit.App.Models.Page
{
    public class VaultViewSitePageModel : INotifyPropertyChanged
    {
        private string _name;
        private string _username;
        private string _password;
        private string _uri;
        private string _notes;
        private bool _revealPassword;

        public VaultViewSitePageModel() { }

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
            }
        }
        public bool ShowUsername => !string.IsNullOrWhiteSpace(Username);

        public string Password
        {
            get { return _password; }
            set
            {
                _password = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(Password)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(MaskedPassword)));
            }
        }
        public bool ShowPassword => !string.IsNullOrWhiteSpace(Password);

        public string Uri
        {
            get { return _uri; }
            set
            {
                _uri = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(Uri)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(UriHost)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(ShowUri)));
            }
        }
        public bool ShowUri => !string.IsNullOrWhiteSpace(Uri);

        public string UriHost
        {
            get
            {
                if(ShowUri)
                {
                    try
                    {
                        return new Uri(Uri).Host;
                    }
                    catch
                    {
                        return Uri;
                    }
                }

                return null;
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
            }
        }
        public string MaskedPassword => RevealPassword ? Password : Password == null ? null : new string('●', Password.Length);
        public string ShowHideText => RevealPassword ? AppResources.Hide : AppResources.Show;

        public void Update(Site site)
        {
            Name = site.Name?.Decrypt();
            Username = site.Username?.Decrypt();
            Password = site.Password?.Decrypt();
            Uri = site.Uri?.Decrypt();
            Notes = site.Notes?.Decrypt();
        }
    }
}
