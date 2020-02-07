using Bit.Core.Models.View;

namespace Bit.Core.Models.Export
{
    public class Folder
    {
        public Folder()
        {
            Name = "Folder name";
        }

        public FolderView ToView(Folder req, FolderView view = null)
        {
            if(view == null)
            {
                view = new FolderView();
            }

            view.Name = req.Name;
            return view;
        }

        public string Name { get; set; }

        public Folder(FolderView obj)
        {
            Name = obj.Name;
        }
    }
}
