using System;
using System.Threading.Tasks;

namespace Bit.Core.Abstractions
{
    public interface IPasswordResetEnrollmentService
    {
        Task EnrollIfRequired(string organizationSsoId);
        Task Enroll(string organizationId);
    }
}

