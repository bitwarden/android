using Bit.App.Enums;
using System.Collections.Generic;
using System.Linq;

namespace Bit.App.Models.Api
{
    public class CipherRequest
    {
        public CipherRequest(Cipher cipher)
        {
            Type = cipher.Type;
            OrganizationId = cipher.OrganizationId;
            FolderId = cipher.FolderId;
            Name = cipher.Name?.EncryptedString;
            Notes = cipher.Notes?.EncryptedString;
            Favorite = cipher.Favorite;

            if(cipher.Fields != null)
            {
                Fields = cipher.Fields.Select(f => new FieldType(f));
            }

            switch(Type)
            {
                case CipherType.Login:
                    Login = new LoginType(cipher);
                    break;
                case CipherType.Card:
                    Card = new CardType(cipher);
                    break;
                case CipherType.Identity:
                    Identity = new IdentityType(cipher);
                    break;
                case CipherType.SecureNote:
                    SecureNote = new SecureNoteType(cipher);
                    break;
                default:
                    break;
            }
        }

        public CipherType Type { get; set; }
        public string OrganizationId { get; set; }
        public string FolderId { get; set; }
        public bool Favorite { get; set; }
        public string Name { get; set; }
        public string Notes { get; set; }
        public IEnumerable<FieldType> Fields { get; set; }
        public IEnumerable<PasswordHistoryResponse> PasswordHistory { get; set; }

        public LoginType Login { get; set; }
        public CardType Card { get; set; }
        public IdentityType Identity { get; set; }
        public SecureNoteType SecureNote { get; set; }
    }
}
