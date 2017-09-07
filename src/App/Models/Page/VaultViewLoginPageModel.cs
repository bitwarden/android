using System;
using System.ComponentModel;
using Bit.App.Resources;
using Xamarin.Forms;
using System.Collections.Generic;

namespace Bit.App.Models.Page
{
    public class VaultViewLoginPageModel : INotifyPropertyChanged
    {
        private string _name;
        private string _username;
        private string _password;
        private string _uri;
        private string _notes;
        private string _totpCode;
        private int _totpSec = 30;
        private bool _revealPassword;
        private List<Attachment> _attachments;

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
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(ShowPassword)));
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
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(ShowLaunch)));
            }
        }
        public bool ShowUri => !string.IsNullOrWhiteSpace(Uri);

        public bool ShowLaunch
        {
            get
            {
                if(!ShowUri)
                {
                    return false;
                }

                if(Device.RuntimePlatform == Device.Android && !Uri.StartsWith("http") &&
                    !Uri.StartsWith("androidapp://"))
                {
                    return false;
                }

                if(Device.RuntimePlatform != Device.Android && !Uri.StartsWith("http"))
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

                string domain;
                if(DomainName.TryParseBaseDomain(uri.Host, out domain))
                {
                    return domain;
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

        public string TotpCode
        {
            get { return _totpCode; }
            set
            {
                _totpCode = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(TotpCode)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(TotpCodeFormatted)));
            }
        }
        public int TotpSecond
        {
            get { return _totpSec; }
            set
            {
                _totpSec = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(TotpSecond)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(TotpColor)));
            }
        }
        public bool TotpLow => TotpSecond <= 7;
        public Color TotpColor => !string.IsNullOrWhiteSpace(TotpCode) && TotpLow ? Color.Red : Color.Black;
        public string TotpCodeFormatted => !string.IsNullOrWhiteSpace(TotpCode) ?
            string.Format("{0} {1}", TotpCode.Substring(0, 3), TotpCode.Substring(3)) : null;

        public List<Attachment> Attachments
        {
            get { return _attachments; }
            set
            {
                _attachments = value;
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(Attachments)));
                PropertyChanged(this, new PropertyChangedEventArgs(nameof(ShowAttachments)));
            }
        }
        public bool ShowAttachments => (Attachments?.Count ?? 0) > 0;

        public void Update(Login login)
        {
            Name = login.Name?.Decrypt(login.OrganizationId);
            Username = login.Username?.Decrypt(login.OrganizationId);
            Password = login.Password?.Decrypt(login.OrganizationId);
            Uri = login.Uri?.Decrypt(login.OrganizationId);
            Notes = login.Notes?.Decrypt(login.OrganizationId);

            if(login.Attachments != null)
            {
                var attachments = new List<Attachment>();
                foreach(var attachment in login.Attachments)
                {
                    attachments.Add(new Attachment
                    {
                        Id = attachment.Id,
                        Name = attachment.FileName?.Decrypt(login.OrganizationId),
                        SizeName = attachment.SizeName,
                        Size = attachment.Size,
                        Url = attachment.Url
                    });
                }
                Attachments = attachments;
            }
            else
            {
                login.Attachments = null;
            }
        }

        public class Attachment
        {
            public string Id { get; set; }
            public string Name { get; set; }
            public string SizeName { get; set; }
            public long Size { get; set; }
            public string Url { get; set; }
        }
    }
}
