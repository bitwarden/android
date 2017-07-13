using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.App.Models.Data;

namespace Bit.App.Abstractions
{
    public interface IAttachmentRepository : IRepository<AttachmentData, string>
    {
        Task<IEnumerable<AttachmentData>> GetAllByLoginIdAsync(string loginId);
        Task<IEnumerable<AttachmentData>> GetAllByUserIdAsync(string userId);
    }
}
