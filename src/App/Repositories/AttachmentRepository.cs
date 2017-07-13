using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models.Data;

namespace Bit.App.Repositories
{
    public class AttachmentRepository : Repository<AttachmentData, string>, IAttachmentRepository
    {
        public AttachmentRepository(ISqlService sqlService)
            : base(sqlService)
        { }

        public Task<IEnumerable<AttachmentData>> GetAllByLoginIdAsync(string loginId)
        {
            var attachments = Connection.Table<AttachmentData>().Where(a => a.LoginId == loginId).Cast<AttachmentData>();
            return Task.FromResult(attachments);
        }

        public Task<IEnumerable<AttachmentData>> GetAllByUserIdAsync(string userId)
        {
            var attachments = Connection.Query<AttachmentData>(@"
                SELECT
                    A.*
                FROM
                    Attachment AS A
                INNER JOIN
                    Site AS S ON S.Id = A.LoginId
                WHERE
                    S.UserId = ?", userId);
            return Task.FromResult<IEnumerable<AttachmentData>>(attachments);
        }
    }
}
