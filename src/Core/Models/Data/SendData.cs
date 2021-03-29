using System;
using Bit.Core.Enums;
using Bit.Core.Models.Response;

namespace Bit.Core.Models.Data
{
    public class SendData : Data
    {
        public SendData() { }

        public SendData(SendResponse response, string userId)
        {
            Id = response.Id;
            AccessId = response.AccessId;
            UserId = userId;
            Type = response.Type;
            Name = response.Name;
            Notes = response.Notes;
            Key = response.Key;
            MaxAccessCount = response.MaxAccessCount;
            AccessCount = response.AccessCount;
            RevisionDate = response.RevisionDate;
            ExpirationDate = response.ExpirationDate;
            DeletionDate = response.DeletionDate;
            Password = response.Password;
            Disabled = response.Disabled;
            HideEmail = response.HideEmail.GetValueOrDefault();

            switch (Type)
            {
                case SendType.File:
                    File = new SendFileData(response.File);
                    break;
                case SendType.Text:
                    Text = new SendTextData(response.Text);
                    break;
                default:
                    break;
            }
        }
        
        public string Id { get; set; }
        public string AccessId { get; set; }
        public string UserId { get; set; }
        public SendType Type { get; set; }
        public string Name { get; set; }
        public string Notes { get; set; }
        public SendFileData File { get; set; }
        public SendTextData Text { get; set; }
        public string Key { get; set; }
        public int? MaxAccessCount { get; set; }
        public int AccessCount { get; set; }
        public DateTime RevisionDate { get; set; }
        public DateTime? ExpirationDate { get; set; }
        public DateTime DeletionDate { get; set; }
        public string Password { get; set; }
        public bool Disabled { get; set; }
        public bool HideEmail { get; set; }
    }
}
