using Bit.Core.Enums;

namespace Bit.Core.Models.Domain
{
    public class StorageOptions : Domain
    {
        public StorageLocation? StorageLocation { get; set; }
        public bool? UseSecureStorage { get; set; }
        public string UserId { get; set; }
        public string Email { get; set; }
        public bool? SkipTokenStorage { get; set; }
    }
}
