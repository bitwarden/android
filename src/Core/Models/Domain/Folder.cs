using Bit.Core.Models.Data;
using Bit.Core.Models.View;
using System;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace Bit.Core.Models.Domain
{
    public class Folder : Domain
    {
        public Folder() { }

        public Folder(FolderData obj, bool alreadyEncrypted = false)
        {
            BuildDomainModel(this, obj, new HashSet<string>
            {
                "Id",
                "Name"
            }, alreadyEncrypted, new HashSet<string> { "Id" });
            RevisionDate = obj.RevisionDate;
        }

        public string Id { get; set; }
        public EncString Name { get; set; }
        public DateTime RevisionDate { get; set; }

        public Task<FolderView> DecryptAsync()
        {
            return DecryptObjAsync(new FolderView(this), this, new HashSet<string> { "Name" }, null);
        }
    }
}
