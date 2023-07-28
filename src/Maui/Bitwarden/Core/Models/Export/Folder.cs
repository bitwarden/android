using Bit.Core.Models.View;

namespace Bit.Core.Models.Export
{
    public class Folder
    {
        public Folder() { }

        public Folder(FolderView obj)
        {
            Name = obj.Name;
        }

        public Folder(Domain.Folder obj)
        {
            Name = obj.Name?.EncryptedString;
        }

        public string Name { get; set; }

        public FolderView ToView(Folder req, FolderView view = null)
        {
            if (view == null)
            {
                view = new FolderView();
            }

            view.Name = req.Name;
            return view;
        }
    }
}
