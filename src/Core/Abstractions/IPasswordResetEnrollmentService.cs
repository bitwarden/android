using System;
using System.Threading.Tasks;

namespace Bit.Core.Abstractions
{
    public interface IPasswordResetEnrollmentService
    {
        Task EnrollIfRequiredAsync(string organizationSsoId);
        Task EnrollAsync(string organizationId);
    }
}

