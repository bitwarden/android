namespace Bit.App.Abstractions
{
    public interface IPasswordGenerationService
    {
        string GeneratePassword(
            int? length = null,
            bool? uppercase = null,
            bool? lowercase = null,
            bool? numbers = null,
            bool? special = null,
            bool? ambiguous = null,
            int? minUppercase = null,
            int? minLowercase = null,
            int? minNumbers = null,
            int? minSpecial = null);
    }
}
