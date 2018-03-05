namespace Bit.App.Models.Api
{
    public class SecureNoteType
    {
        public SecureNoteType() { }

        public SecureNoteType(Cipher cipher)
        {
            Type = cipher.SecureNote.Type;
        }

        public Enums.SecureNoteType Type { get; set; }
    }
}
