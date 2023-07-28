using Bit.Core.Enums;
using Bit.Core.Models.View;

namespace Bit.Core.Models.Export
{
    public class SecureNote
    {
        public SecureNote() { }

        public SecureNote(SecureNoteView obj)
        {
            Type = obj.Type;
        }

        public SecureNote(Domain.SecureNote obj)
        {
            Type = obj.Type;
        }

        public SecureNoteType Type { get; set; }

        public SecureNoteView ToView(SecureNote req, SecureNoteView view = null)
        {
            if (view == null)
            {
                view = new SecureNoteView();
            }

            view.Type = req.Type;
            return view;
        }
    }
}
