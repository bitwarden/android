using System.Collections.Generic;
using System.Linq;
using Bit.Core.Enums;
using Bit.Core.Models.Domain;
using Bit.Core.Models.View;
using Newtonsoft.Json;

namespace Bit.Core.Models.Export
{
    public class Cipher
    {
        public Cipher() { }

        public Cipher(CipherView obj)
        {
            OrganizationId = obj.OrganizationId;
            FolderId = obj.FolderId;
            Type = obj.Type;
            Name = obj.Name;
            Notes = obj.Notes;
            Favorite = obj.Favorite;

            Fields = obj.Fields?.Select(f => new Field(f)).ToList();

            switch (obj.Type)
            {
                case CipherType.Login:
                    Login = new Login(obj.Login);
                    break;
                case CipherType.SecureNote:
                    SecureNote = new SecureNote(obj.SecureNote);
                    break;
                case CipherType.Card:
                    Card = new Card(obj.Card);
                    break;
                case CipherType.Identity:
                    Identity = new Identity(obj.Identity);
                    break;
            }
        }

        public Cipher(Domain.Cipher obj)
        {
            OrganizationId = obj.OrganizationId;
            FolderId = obj.FolderId;
            Type = obj.Type;
            Name = obj.Name?.EncryptedString;
            Notes = obj.Notes?.EncryptedString;
            Favorite = obj.Favorite;
            Key = obj.Key?.EncryptedString;

            Fields = obj.Fields?.Select(f => new Field(f)).ToList();

            switch (obj.Type)
            {
                case CipherType.Login:
                    Login = new Login(obj.Login);
                    break;
                case CipherType.SecureNote:
                    SecureNote = new SecureNote(obj.SecureNote);
                    break;
                case CipherType.Card:
                    Card = new Card(obj.Card);
                    break;
                case CipherType.Identity:
                    Identity = new Identity(obj.Identity);
                    break;
            }
        }

        public string OrganizationId { get; set; }
        public string FolderId { get; set; }
        public CipherType Type { get; set; }
        public string Name { get; set; }
        public string Notes { get; set; }
        public bool Favorite { get; set; }
        [JsonProperty(NullValueHandling = NullValueHandling.Ignore)]
        public List<Field> Fields { get; set; }
        [JsonProperty(NullValueHandling = NullValueHandling.Ignore)]
        public Login Login { get; set; }
        [JsonProperty(NullValueHandling = NullValueHandling.Ignore)]
        public SecureNote SecureNote { get; set; }
        [JsonProperty(NullValueHandling = NullValueHandling.Ignore)]
        public Card Card { get; set; }
        [JsonProperty(NullValueHandling = NullValueHandling.Ignore)]
        public Identity Identity { get; set; }
        [JsonProperty(NullValueHandling = NullValueHandling.Ignore)]
        public string Key { get; set; }

        public CipherView ToView(Cipher req, CipherView view = null)
        {
            if (view == null)
            {
                view = new CipherView();
            }

            view.Type = req.Type;
            view.FolderId = req.FolderId;
            if (view.OrganizationId == null)
            {
                view.OrganizationId = req.OrganizationId;
            }

            view.Name = req.Name;
            view.Notes = req.Notes;
            view.Favorite = req.Favorite;

            view.Fields = req.Fields?.Select(f => Field.ToView(f)).ToList();

            switch (req.Type)
            {
                case CipherType.Login:
                    view.Login = Login.ToView(req.Login);
                    break;
                case CipherType.SecureNote:
                    view.SecureNote = SecureNote.ToView(req.SecureNote);
                    break;
                case CipherType.Card:
                    view.Card = Card.ToView(req.Card);
                    break;
                case CipherType.Identity:
                    view.Identity = Identity.ToView(req.Identity);
                    break;
            }

            return view;
        }
    }
}
