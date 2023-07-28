using System;
using Bit.Core.Models.Domain;

namespace Bit.Core.Models.View
{
    public class FolderView : View, ITreeNodeObject
    {
        public FolderView() { }

        public FolderView(Folder f)
        {
            Id = f.Id;
            RevisionDate = f.RevisionDate;
        }

        public string Id { get; set; }
        public string Name { get; set; }
        public DateTime RevisionDate { get; set; }
    }
}
