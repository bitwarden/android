using System;
using Bit.Core.Enums;
using Bit.Core.Models.Api;
using Bit.Core.Models.Domain;

namespace Bit.Core.Models.Request
{
    public class SendRequest
    {
        public SendType Type { get; set; }
        public long? FileLength { get; set; }
        public string Name { get; set; }
        public string Notes { get; set; }
        public string Key { get; set; }
        public int? MaxAccessCount { get; set; }
        public DateTime? ExpirationDate { get; set; }
        public DateTime DeletionDate { get; set; }
        public SendTextApi Text { get; set; }
        public SendFileApi File { get; set; }
        public string Password { get; set; }
        public bool Disabled { get; set; }
        public bool HideEmail { get; set; }

        public SendRequest(Send send, long? fileLength)
        {
            Type = send.Type ;
            FileLength = fileLength;
            Name = send.Name?.EncryptedString;
            Notes = send.Notes?.EncryptedString;
            MaxAccessCount = send.MaxAccessCount;
            ExpirationDate = send.ExpirationDate;
            DeletionDate = send.DeletionDate;
            Key = send.Key?.EncryptedString;
            Password = send.Password;
            Disabled = send.Disabled;
            HideEmail = send.HideEmail;

            switch (Type)
            {
                case SendType.Text:
                    Text = new SendTextApi
                    {
                        Text = send.Text?.Text?.EncryptedString,
                        Hidden = send.Text.Hidden
                    };
                    break;
                case SendType.File:
                    File = new SendFileApi
                    {
                        FileName = send.File?.FileName?.EncryptedString
                    };
                    break;
                default:
                    break;
            }
        }
    }
}
