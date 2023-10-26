using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models.Data;
using Bit.Core.Models.View;
using Bit.Core.Utilities;

namespace Bit.Core.Models.Domain
{
    public class Cipher : Domain
    {
        public Cipher() { }

        public Cipher(CipherData obj, bool alreadyEncrypted = false, Dictionary<string, object> localData = null)
        {
            BuildDomainModel(this, obj, new HashSet<string>
            {
                nameof(Id),
                nameof(OrganizationId),
                nameof(FolderId),
                nameof(Name),
                nameof(Notes)
            }, alreadyEncrypted, new HashSet<string> { nameof(Id), nameof(OrganizationId), nameof(FolderId) });

            Type = obj.Type;
            Favorite = obj.Favorite;
            OrganizationUseTotp = obj.OrganizationUseTotp;
            Edit = obj.Edit;
            ViewPassword = obj.ViewPassword;
            RevisionDate = obj.RevisionDate;
            CreationDate = obj.CreationDate;
            CollectionIds = obj.CollectionIds != null ? new HashSet<string>(obj.CollectionIds) : null;
            LocalData = localData;
            Reprompt = obj.Reprompt;

            if (obj.Key != null)
            {
                Key = new EncString(obj.Key);
            }

            switch (Type)
            {
                case Enums.CipherType.Login:
                    Login = new Login(obj.Login, alreadyEncrypted);
                    break;
                case Enums.CipherType.SecureNote:
                    SecureNote = new SecureNote(obj.SecureNote, alreadyEncrypted);
                    break;
                case Enums.CipherType.Card:
                    Card = new Card(obj.Card, alreadyEncrypted);
                    break;
                case Enums.CipherType.Identity:
                    Identity = new Identity(obj.Identity, alreadyEncrypted);
                    break;
                default:
                    break;
            }

            Attachments = obj.Attachments?.Select(a => new Attachment(a, alreadyEncrypted)).ToList();
            Fields = obj.Fields?.Select(f => new Field(f, alreadyEncrypted)).ToList();
            PasswordHistory = obj.PasswordHistory?.Select(ph => new PasswordHistory(ph, alreadyEncrypted)).ToList();
            DeletedDate = obj.DeletedDate;
        }

        public string Id { get; set; }
        public string OrganizationId { get; set; }
        public string FolderId { get; set; }
        public EncString Name { get; set; }
        public EncString Notes { get; set; }
        public CipherType Type { get; set; }
        public bool Favorite { get; set; }
        public bool OrganizationUseTotp { get; set; }
        public bool Edit { get; set; }
        public bool ViewPassword { get; set; }
        public DateTime RevisionDate { get; set; }
        public DateTime CreationDate { get; set; }
        public DateTime? DeletedDate { get; set; }
        public Dictionary<string, object> LocalData { get; set; }
        public Login Login { get; set; }
        public Identity Identity { get; set; }
        public Card Card { get; set; }
        public SecureNote SecureNote { get; set; }
        public List<Attachment> Attachments { get; set; }
        public List<Field> Fields { get; set; }
        public List<PasswordHistory> PasswordHistory { get; set; }
        public HashSet<string> CollectionIds { get; set; }
        public CipherRepromptType Reprompt { get; set; }
        public EncString Key { get; set; }

        public async Task<CipherView> DecryptAsync()
        {
            var model = new CipherView(this);

            if (Key != null)
            {
                // HACK: I don't like resolving this here but I can't see a better way without
                // refactoring a lot of things.
                var cryptoService = ServiceContainer.Resolve<ICryptoService>();

                var orgKey = await cryptoService.GetOrgKeyAsync(OrganizationId);

                var key = await cryptoService.DecryptToBytesAsync(Key, orgKey);
                model.Key = new CipherKey(key);
            }

            await DecryptObjAsync(model, this, new HashSet<string>
            {
                nameof(Name),
                nameof(Notes)
            }, OrganizationId, model.Key);

            switch (Type)
            {
                case Enums.CipherType.Login:
                    model.Login = await Login.DecryptAsync(OrganizationId, model.Key);
                    break;
                case Enums.CipherType.SecureNote:
                    model.SecureNote = await SecureNote.DecryptAsync(OrganizationId, model.Key);
                    break;
                case Enums.CipherType.Card:
                    model.Card = await Card.DecryptAsync(OrganizationId, model.Key);
                    break;
                case Enums.CipherType.Identity:
                    model.Identity = await Identity.DecryptAsync(OrganizationId, model.Key);
                    break;
                default:
                    break;
            }

            if (Attachments?.Any() ?? false)
            {
                model.Attachments = new List<AttachmentView>();
                var tasks = new List<Task>();
                async Task decryptAndAddAttachmentAsync(Attachment attachment)
                {
                    var decAttachment = await attachment.DecryptAsync(OrganizationId, model.Key);
                    model.Attachments.Add(decAttachment);
                }
                foreach (var attachment in Attachments)
                {
                    tasks.Add(decryptAndAddAttachmentAsync(attachment));
                }
                await Task.WhenAll(tasks);
            }
            if (Fields?.Any() ?? false)
            {
                model.Fields = new List<FieldView>();
                var tasks = new List<Task>();
                async Task decryptAndAddFieldAsync(Field field)
                {
                    var decField = await field.DecryptAsync(OrganizationId, model.Key);
                    model.Fields.Add(decField);
                }
                foreach (var field in Fields)
                {
                    tasks.Add(decryptAndAddFieldAsync(field));
                }
                await Task.WhenAll(tasks);
            }
            if (PasswordHistory?.Any() ?? false)
            {
                model.PasswordHistory = new List<PasswordHistoryView>();
                var tasks = new List<Task>();
                async Task decryptAndAddHistoryAsync(PasswordHistory ph)
                {
                    var decPh = await ph.DecryptAsync(OrganizationId, model.Key);
                    model.PasswordHistory.Add(decPh);
                }
                foreach (var ph in PasswordHistory)
                {
                    tasks.Add(decryptAndAddHistoryAsync(ph));
                }
                await Task.WhenAll(tasks);
            }
            return model;
        }

        public CipherData ToCipherData(string userId)
        {
            var c = new CipherData
            {
                Id = Id,
                OrganizationId = OrganizationId,
                FolderId = FolderId,
                UserId = OrganizationId != null ? userId : null,
                Edit = Edit,
                OrganizationUseTotp = OrganizationUseTotp,
                Favorite = Favorite,
                RevisionDate = RevisionDate,
                CreationDate = CreationDate,
                Type = Type,
                CollectionIds = CollectionIds.ToList(),
                DeletedDate = DeletedDate,
                Reprompt = Reprompt,
                Key = Key?.EncryptedString
            };
            BuildDataModel(this, c, new HashSet<string>
            {
                nameof(Name),
                nameof(Notes)
            });
            switch (c.Type)
            {
                case Enums.CipherType.Login:
                    c.Login = Login.ToLoginData();
                    break;
                case Enums.CipherType.SecureNote:
                    c.SecureNote = SecureNote.ToSecureNoteData();
                    break;
                case Enums.CipherType.Card:
                    c.Card = Card.ToCardData();
                    break;
                case Enums.CipherType.Identity:
                    c.Identity = Identity.ToIdentityData();
                    break;
                default:
                    break;
            }
            c.Fields = Fields?.Select(f => f.ToFieldData()).ToList();
            c.Attachments = Attachments?.Select(a => a.ToAttachmentData()).ToList();
            c.PasswordHistory = PasswordHistory?.Select(ph => ph.ToPasswordHistoryData()).ToList();
            return c;
        }
    }
}
