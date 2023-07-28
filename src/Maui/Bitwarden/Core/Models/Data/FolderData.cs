using System;
using Bit.Core.Models.Response;

namespace Bit.Core.Models.Data
{
    public class FolderData : Data
    {
        public FolderData() { }

        public FolderData(FolderResponse response, string userId)
        {
            UserId = userId;
            Id = response.Id;
            Name = response.Name;
            RevisionDate = response.RevisionDate;
        }

        public string Id { get; set; }
        public string UserId { get; set; }
        public string Name { get; set; }
        public DateTime RevisionDate { get; set; }
    }
}
