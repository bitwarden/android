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
        private bool _showPassword;

        public VaultViewSitePageModel() { }

        public event PropertyChangedEventHandler PropertyChanged;

        public string Name
        {
            get { return _name; }
            set
            {
                _name = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(Name)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(PageTitle)));
            }
        }
        public string Username
        {
            get { return _username; }
            set
            {
                _username = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(Username)));
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
            }
        }
        public string Uri
        {
            get { return _uri; }
            set
            {
                _uri = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(Uri)));
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
        public string PageTitle => Name ?? AppResources.SiteNoName;
        public bool ShowPassword
        {
            get { return _showPassword; }
            set
            {
                _showPassword = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(ShowPassword)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(MaskedPassword)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(ShowHideText)));
            }
        }
        public string MaskedPassword => ShowPassword ? Password : Password == null ? null : new string('●', Password.Length);
        public string ShowHideText => ShowPassword ? AppResources.Hide : AppResources.Show;
        public bool ShowNotes => !string.IsNullOrWhiteSpace(Notes);

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
