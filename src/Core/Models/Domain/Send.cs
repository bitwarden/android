using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models.Data;
using Bit.Core.Models.View;
using Bit.Core.Utilities;

namespace Bit.Core.Models.Domain
{
    public class Send : Domain
    {
        public string Id { get; set; }
        public string AccessId { get; set; }
        public string UserId { get; set; }
        public SendType Type { get; set; }
        public EncString Name { get; set; }
        public EncString Notes { get; set; }
        public SendFile File { get; set; }
        public SendText Text { get; set; }
        public EncString Key { get; set; }
        public int? MaxAccessCount { get; set; }
        public int AccessCount { get; set; }
        public DateTime RevisionDate { get; set; }
        public DateTime? ExpirationDate { get; set; }
        public DateTime DeletionDate { get; set; }
        public string Password { get; set; }
        public bool Disabled { get; set; }
        public bool HideEmail { get; set; }

        public Send() : base() { }

        public Send(SendData data, bool alreadyEncrypted = false) : base()
        {
            BuildDomainModel(this, data, new HashSet<string>{
                "Id",
                "AccessId",
                "UserId",
                "Name",
                "Notes",
                "Key",
            }, alreadyEncrypted, new HashSet<string> { "Id", "AccessId", "UserId" });

            Type = data.Type;
            MaxAccessCount = data.MaxAccessCount;
            AccessCount = data.AccessCount;
            Password = data.Password;
            Disabled = data.Disabled;
            RevisionDate = data.RevisionDate;
            DeletionDate = data.DeletionDate;
            ExpirationDate = data.ExpirationDate;
            HideEmail = data.HideEmail;

            switch (Type)
            {
                case SendType.Text:
                    Text = new SendText(data.Text, alreadyEncrypted);
                    break;
                case SendType.File:
                    File = new SendFile(data.File, alreadyEncrypted);
                    break;
                default:
                    break;
            }
        }

        public async Task<SendView> DecryptAsync()
        {
            var view = new SendView(this);

            var cryptoService = ServiceContainer.Resolve<ICryptoService>("cryptoService");

            view.Key = await cryptoService.DecryptToBytesAsync(Key, null);
            view.CryptoKey = await cryptoService.MakeSendKeyAsync(view.Key);

            await DecryptObjAsync(view, this, new HashSet<string> { "Name", "Notes" }, null, view.CryptoKey);

            switch (Type)
            {
                case SendType.File:
                    view.File = await this.File.DecryptAsync(view.CryptoKey);
                    break;
                case SendType.Text:
                    view.Text = await this.Text.DecryptAsync(view.CryptoKey);
                    break;
                default:
                    break;
            }
            return view;
        }
    }
}
