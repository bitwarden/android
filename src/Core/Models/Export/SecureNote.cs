using Bit.Core.Enums;
using Bit.Core.Models.View;

namespace Bit.Core.Models.Export
{
    public class SecureNote
    {
        public SecureNote()
        {
            Type = SecureNoteType.Generic;
        }

        public SecureNoteView ToView(SecureNote req, SecureNoteView view = null)
        {
            if(view == null)
            {
                view = new SecureNoteView();
            }

            view.Type = req.Type;
            return view;
        }

        public SecureNoteType Type { get; set; }

        public SecureNote(SecureNoteView obj)
        {
            if(obj == null)
            {
                return;
            }

            Type = obj.Type;
        }
    }
}
