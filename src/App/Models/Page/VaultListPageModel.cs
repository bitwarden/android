using System;
using System.Collections.Generic;
using Bit.App.Resources;
using System.Linq;
using Bit.App.Enums;
using Bit.App.Abstractions;

namespace Bit.App.Models.Page
{
    public class VaultListPageModel
    {
        public class Cipher
        {
            public Cipher(Models.Cipher cipher, IAppSettingsService appSettings)
            {
                CipherModel = cipher;
                Id = cipher.Id;
                Shared = !string.IsNullOrWhiteSpace(cipher.OrganizationId);
                HasAttachments = cipher.Attachments?.Any() ?? false;
                FolderId = cipher.FolderId;
                Name = cipher.Name?.Decrypt(cipher.OrganizationId);
                Type = cipher.Type;

                if(string.IsNullOrWhiteSpace(Name) || Name.Length == 0)
                {
                    NameGroup = "-";
                }
                else if(Char.IsLetter(Name[0]))
                {
                    NameGroup = Name[0].ToString();
                }
                else if(Char.IsDigit(Name[0]))
                {
                    NameGroup = "#";
                }
                else
                {
                    NameGroup = "-";
                }

                switch(cipher.Type)
                {
                    case CipherType.Login:
                        LoginUsername = cipher.Login?.Username?.Decrypt(cipher.OrganizationId) ?? " ";
                        LoginUri = cipher.Login.Uris?.FirstOrDefault()?.Uri?.Decrypt(cipher.OrganizationId) ?? " ";
                        LoginPassword = new Lazy<string>(() => cipher.Login?.Password?.Decrypt(cipher.OrganizationId));
                        LoginTotp = new Lazy<string>(() => cipher.Login?.Totp?.Decrypt(cipher.OrganizationId));

                        Icon = "login.png";
                        var hostnameUri = LoginUri;
                        var isWebsite = false;
                        var imageEnabled = !appSettings.DisableWebsiteIcons;
                        if(hostnameUri.StartsWith("androidapp://"))
                        {
                            Icon = "android.png";
                        }
                        else if(hostnameUri.StartsWith("iosapp://"))
                        {
                            Icon = "apple.png";
                        }
                        else if(imageEnabled && !hostnameUri.Contains("://") && hostnameUri.Contains("."))
                        {
                            hostnameUri = $"http://{hostnameUri}";
                            isWebsite = true;
                        }
                        else if(imageEnabled)
                        {
                            isWebsite = hostnameUri.StartsWith("http") && hostnameUri.Contains(".");
                        }

                        if(imageEnabled && isWebsite && Uri.TryCreate(hostnameUri, UriKind.Absolute, out Uri u))
                        {
                            var iconsUrl = appSettings.IconsUrl;
                            if(string.IsNullOrWhiteSpace(iconsUrl))
                            {
                                if(!string.IsNullOrWhiteSpace(appSettings.BaseUrl))
                                {
                                    iconsUrl = $"{appSettings.BaseUrl}/icons";
                                }
                                else
                                {
                                    iconsUrl = "https://icons.bitwarden.net";
                                }
                            }

                            Icon = $"{iconsUrl}/{u.Host}/icon.png";
                        }

                        Subtitle = LoginUsername;
                        break;
                    case CipherType.SecureNote:
                        Icon = "note.png";
                        Subtitle = " ";
                        break;
                    case CipherType.Card:
                        CardNumber = cipher.Card?.Number?.Decrypt(cipher.OrganizationId) ?? " ";
                        var cardBrand = cipher.Card?.Brand?.Decrypt(cipher.OrganizationId) ?? " ";
                        CardCode = new Lazy<string>(() => cipher.Card?.Code?.Decrypt(cipher.OrganizationId));

                        Icon = "card.png";
                        Subtitle = cardBrand;
                        if(!string.IsNullOrWhiteSpace(CardNumber) && CardNumber.Length >= 4)
                        {
                            if(!string.IsNullOrWhiteSpace(CardNumber))
                            {
                                Subtitle += ", ";
                            }
                            Subtitle += ("*" + CardNumber.Substring(CardNumber.Length - 4));
                        }
                        break;
                    case CipherType.Identity:
                        var firstName = cipher.Identity?.FirstName?.Decrypt(cipher.OrganizationId) ?? " ";
                        var lastName = cipher.Identity?.LastName?.Decrypt(cipher.OrganizationId) ?? " ";

                        Icon = "id.png";
                        Subtitle = " ";
                        if(!string.IsNullOrWhiteSpace(firstName))
                        {
                            Subtitle = firstName;
                        }
                        if(!string.IsNullOrWhiteSpace(lastName))
                        {
                            if(!string.IsNullOrWhiteSpace(Subtitle))
                            {
                                Subtitle += " ";
                            }
                            Subtitle += lastName;
                        }
                        break;
                    default:
                        break;
                }
            }

            public Models.Cipher CipherModel { get; set; }
            public string Id { get; set; }
            public bool Shared { get; set; }
            public bool HasAttachments { get; set; }
            public string FolderId { get; set; }
            public string NameGroup { get; set; }
            public string Name { get; set; }
            public string Subtitle { get; set; }
            public CipherType Type { get; set; }
            public string Icon { get; set; }
            public string Image { get; set; }

            // Login metadata
            public string LoginUsername { get; set; }
            public Lazy<string> LoginPassword { get; set; }
            public string LoginUri { get; set; }
            public Lazy<string> LoginTotp { get; set; }

            // Login metadata
            public string CardNumber { get; set; }
            public Lazy<string> CardCode { get; set; }
        }

        public class AutofillCipher : Cipher
        {
            public AutofillCipher(Models.Cipher cipher, IAppSettingsService appSettings, bool fuzzy = false)
                : base(cipher, appSettings)
            {
                Fuzzy = fuzzy;
            }

            public bool Fuzzy { get; set; }
        }

        public class Section<T> : List<T>
        {
            public Section(List<T> groupItems, string name, bool doUpper = true)
            {
                AddRange(groupItems);

                if(string.IsNullOrWhiteSpace(name))
                {
                    Name = "-";
                }
                else if(doUpper)
                {
                    Name = name.ToUpperInvariant();
                }
                else
                {
                    Name = name;
                }
            }

            public string Name { get; set; }
            public string NameShort => string.IsNullOrWhiteSpace(Name) || Name.Length == 0 ? "-" : Name[0].ToString();
        }

        public class GroupingOrCipher
        {
            public GroupingOrCipher(TreeNode<Grouping> grouping)
            {
                Grouping = grouping;
                Cipher = null;
            }

            public GroupingOrCipher(Cipher cipher)
            {
                Cipher = cipher;
                Grouping = null;
            }

            public TreeNode<Grouping> Grouping { get; set; }
            public Cipher Cipher { get; set; }
        }

        public class Grouping : ITreeNodeObject
        {
            public Grouping(string name, int count)
            {
                Id = null;
                Name = name;
                Folder = true;
                Count = count;
            }

            public Grouping(Folder folder, int? count)
            {
                Id = folder.Id;
                Name = folder.Name?.Decrypt();
                Folder = true;
                Count = count;
            }

            public Grouping(Collection collection, int? count)
            {
                Id = collection.Id;
                Name = collection.Name?.Decrypt(collection.OrganizationId);
                Collection = true;
                Count = count;
            }

            public string Id { get; set; }
            public string Name { get; set; } = AppResources.FolderNone;
            public int? Count { get; set; }
            public bool Folder { get; set; }
            public bool Collection { get; set; }
            public bool ShowCount => Count.HasValue;
        }
    }
}
