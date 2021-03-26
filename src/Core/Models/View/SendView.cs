using System;
using Bit.Core.Enums;
using Bit.Core.Models.Domain;
using Bit.Core.Utilities;

namespace Bit.Core.Models.View
{
    public class SendView : View
    {
        public SendView() { }

        public SendView(Send send) : base()
        {
            Id = send.Id;
            AccessId = send.AccessId;
            Type = send.Type;
            MaxAccessCount = send.MaxAccessCount;
            AccessCount = send.AccessCount;
            RevisionDate = send.RevisionDate;
            DeletionDate = send.DeletionDate;
            ExpirationDate = send.ExpirationDate;
            Disabled = send.Disabled;
            Password = send.Password;
            HideEmail = send.HideEmail;
        }

        public string Id { get; set; }
        public string AccessId { get; set; }
        public string Name { get; set; }
        public string Notes { get; set; }
        public byte[] Key { get; set; }
        public SymmetricCryptoKey CryptoKey { get; set; }
        public SendType Type { get; set; }
        public SendTextView Text { get; set; } = new SendTextView();
        public SendFileView File { get; set; } = new SendFileView();
        public int? MaxAccessCount { get; set; }
        public int AccessCount { get; set; }
        public DateTime RevisionDate { get; set; }
        public DateTime DeletionDate { get; set; }
        public DateTime? ExpirationDate { get; set; }
        public string Password { get; set; }
        public bool Disabled { get; set; }
        public string UrlB64Key => Key == null ? null : CoreHelpers.Base64UrlEncode(Key);
        public bool HasPassword => Password?.Length > 0;
        public bool MaxAccessCountReached => MaxAccessCount.HasValue && AccessCount >= MaxAccessCount.Value;
        public bool Expired => ExpirationDate.HasValue && ExpirationDate.Value <= DateTime.UtcNow;
        public bool PendingDelete => DeletionDate <= DateTime.UtcNow;
        public string DisplayDate => DeletionDate.ToLocalTime().ToString("MMM d, yyyy, h:mm tt");
        public bool HideEmail { get; set; }
    }
}
