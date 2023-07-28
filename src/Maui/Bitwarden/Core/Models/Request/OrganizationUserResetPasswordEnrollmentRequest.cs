namespace Bit.Core.Models.Request
{
    public class OrganizationUserResetPasswordEnrollmentRequest
    {
        public string MasterPasswordHash { get; set; }
        public string ResetPasswordKey { get; set; }
    }
}
