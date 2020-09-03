using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.Core.Enums;
using Bit.Core.Models.Data;
using Bit.Core.Models.Domain;

namespace Bit.Core.Abstractions
{
    public interface IPolicyService
    {
        void ClearCache();
        Task<IEnumerable<Policy>> GetAll(PolicyType? type);
        Task Replace(Dictionary<string, PolicyData> policies);
        Task Clear(string userId);
        Task<MasterPasswordPolicyOptions> GetMasterPasswordPolicyOptions(IEnumerable<Policy> policies = null);
        Task<bool> EvaluateMasterPassword(int passwordStrength, string newPassword,
            MasterPasswordPolicyOptions enforcedPolicyOptions);
    }
}
