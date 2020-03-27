using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models.Export;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using CsvHelper;
using CsvHelper.Configuration;
using CsvHelper.Configuration.Attributes;
using Newtonsoft.Json;
using Newtonsoft.Json.Serialization;

namespace Bit.Core.Services
{
    public class ExportService : IExportService
    {
        private readonly IFolderService _folderService;
        private readonly ICipherService _cipherService;

        private List<FolderView> _decryptedFolders;
        private List<CipherView> _decryptedCiphers;

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

            if (format == "csv")
            {
                var foldersMap = _decryptedFolders.Where(f => f.Id != null).ToDictionary(f => f.Id);

                var exportCiphers = new List<ExportCipher>();
                foreach (var c in _decryptedCiphers)
                {
                    // only export logins and secure notes
                    if (c.Type != CipherType.Login && c.Type != CipherType.SecureNote)
                    {
                        continue;
                    }

                    if (c.OrganizationId != null)
                    {
                        continue;
                    }

                    var cipher = new ExportCipher();
                    cipher.Folder = c.FolderId != null && foldersMap.ContainsKey(c.FolderId)
                        ? foldersMap[c.FolderId].Name : null;
                    cipher.Favorite = c.Favorite ? "1" : null;
                    BuildCommonCipher(cipher, c);
                    exportCiphers.Add(cipher);
                }

                using (var writer = new StringWriter())
                using (var csv = new CsvWriter(writer, CultureInfo.InvariantCulture))
                {
                    csv.WriteRecords(exportCiphers);
                    csv.Flush();
                    return writer.ToString();
                }
            }
            else
            {
                var jsonDoc = new
                {
                    Folders = _decryptedFolders.Where(f => f.Id != null).Select(f => new FolderWithId(f)),
                    Items = _decryptedCiphers.Where(c => c.OrganizationId == null)
                        .Select(c => new CipherWithId(c) {CollectionIds = null})
                };

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

            return string.Format("bitwarden{0}_export_{1}.{2}",
                !string.IsNullOrEmpty(prefix) ? ("_" + prefix) : string.Empty, dateString, extension);
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

            if (c.Fields != null)
            {
                foreach (var f in c.Fields)
                {
                    if (cipher.Fields == null)
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

            switch (c.Type)
            {
                case CipherType.Login:
                    cipher.Type = "login";
                    cipher.LoginUsername = c.Login.Username;
                    cipher.LoginPassword = c.Login.Password;
                    cipher.LoginTotp = c.Login.Totp;

                    if (c.Login.Uris != null)
                    {
                        foreach (var u in c.Login.Uris)
                        {
                            if (cipher.LoginUris == null)
                            {
                                cipher.LoginUris = "";
                            }
                            else
                            {
                                cipher.LoginUris += ",";
                            }

                            cipher.LoginUris += u.Uri;
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
            [Name("folder")]
            public string Folder { get; set; }
            [Name("favorite")]
            public string Favorite { get; set; }
            [Name("type")]
            public string Type { get; set; }
            [Name("name")]
            public string Name { get; set; }
            [Name("notes")]
            public string Notes { get; set; }
            [Name("fields")]
            public string Fields { get; set; }
            [Name("login_uri")]
            public string LoginUris { get; set; }
            [Name("login_username")]
            public string LoginUsername { get; set; }
            [Name("login_password")]
            public string LoginPassword { get; set; }
            [Name("login_totp")]
            public string LoginTotp { get; set; }
        }
    }
}
