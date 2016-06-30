using Bit.App.Enums;
using System;

namespace Bit.App.Models.Api
{
    public class CipherResponse
    {
        public string Id { get; set; }
        public string FolderId { get; set; }
        public CipherType Type { get; set; }
        public bool Favorite { get; set; }
        public dynamic Data { get; set; }
        public DateTime RevisionDate { get; set; }
    }
}
