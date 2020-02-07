using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models.Export;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using Newtonsoft.Json;
using Newtonsoft.Json.Serialization;

namespace Bit.Core.Services
{
    public class ExportService : IExportService
    {
        private List<FolderView> _decryptedFolders;
        private List<CipherView> _decryptedCiphers;
        private readonly IFolderService _folderService;
        private readonly ICipherService _cipherService;

        public ExportService(
            IFolderService folderService,
            ICipherService cipherService)
        {
            _folderService = folderService;
            _cipherService = cipherService;
        }

        public async Task<string> GetExport(string format = "csv")
        {
            _decryptedFolders = await _folderService.GetAllDecryptedAsync();
            _decryptedCiphers = await _cipherService.GetAllDecryptedAsync();

            if(format == "csv")
            {
                var foldersMap = new Dictionary<string, FolderView>();
                foreach(var f in _decryptedFolders)
                {
                    foldersMap.Add(f.Id, f);
                }

                var exportCiphers = new List<ExportCipher>();
                foreach(var c in _decryptedCiphers)
                {
                    // only export logins and secure notes
                    if(c.Type != CipherType.Login && c.Type != CipherType.SecureNote)
                    {
                        continue;
                    }

                    if(c.OrganizationId != null)
                    {
                        continue;
                    }

                    var cipher = new ExportCipher();
                    cipher.Folder = c.FolderId != null && foldersMap.ContainsKey(c.FolderId)
                        ? foldersMap[c.FolderId].Name
                        : null;
                    cipher.Favorite = c.Favorite ? "1" : null;
                    BuildCommonCipher(cipher, c);
                    exportCiphers.Add(cipher);
                }

                // TODO return csvLib.unparse(exportCiphers);
                throw new NotImplementedException();
            }
            else
            {
                var jsonDoc = new {Folders = new List<FolderWithId>(), Items = new List<CipherWithId>()};

                foreach(var f in _decryptedFolders)
                {
                    if(f.Id == null)
                    {
                        continue;
                    }

                    var folder = new FolderWithId(f);
                    jsonDoc.Folders.Add(folder);
                }

                foreach(var c in _decryptedCiphers)
                {
                    if(c.OrganizationId != null)
                    {
                        continue;
                    }

                    var cipher = new CipherWithId(c);
                    cipher.CollectionIds = null;
                    jsonDoc.Items.Add(cipher);
                }

                return CoreHelpers.SerializeJson(jsonDoc,
                    new JsonSerializerSettings
                    {
                        Formatting = Formatting.Indented,
                        ContractResolver = new CamelCasePropertyNamesContractResolver()
                    });
            }
        }

        public Task<string> GetOrganizationExport(string organizationId, string format = "csv")
        {
            throw new NotImplementedException();
        }

        public string GetFileName(string prefix = null, string extension = "csv")
        {
            var dateString = DateTime.Now.ToString("yyyyMMddHHmmss");

            return "bitwarden" + (!string.IsNullOrEmpty(prefix) ? ("_" + prefix) : "") + "_export_" + dateString + "."
                   + extension;
        }

        private void BuildCommonCipher(ExportCipher cipher, CipherView c)
        {
            cipher.Type = null;
            cipher.Name = c.Name;
            cipher.Notes = c.Notes;
            cipher.Fields = null;
            // Login props
            cipher.LoginUris = null;
            cipher.LoginUsername = null;
            cipher.LoginPassword = null;
            cipher.LoginTotp = null;

            if(c.Fields != null)
            {
                foreach(var f in c.Fields)
                {
                    if(cipher.Fields == null)
                    {
                        cipher.Fields = "";
                    }
                    else
                    {
                        cipher.Fields += "\n";
                    }

                    cipher.Fields += (f.Name ?? "") + ": " + f.Value;
                }
            }

            switch(c.Type)
            {
                case CipherType.Login:
                    cipher.Type = "login";
                    cipher.LoginUsername = c.Login.Username;
                    cipher.LoginPassword = c.Login.Password;
                    cipher.LoginTotp = c.Login.Totp;

                    if(c.Login.Uris != null)
                    {
                        cipher.LoginUris = new List<String>();
                        foreach(var u in c.Login.Uris)
                        {
                            cipher.LoginUris.Add(u.Uri);
                        }
                    }

                    break;
                case CipherType.SecureNote:
                    cipher.Type = "note";
                    break;
                default:
                    return;
            }
        }

        private class ExportCipher
        {
            public string Folder { get; set; }
            public string Favorite { get; set; }
            public string Type { get; set; }
            public string Name { get; set; }
            public string Notes { get; set; }
            public string Fields { get; set; }
            public List<string> LoginUris { get; set; }
            public string LoginUsername { get; set; }
            public string LoginPassword { get; set; }
            public string LoginTotp { get; set; }
        }
    }
}
