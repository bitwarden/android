using Bit.Core.Enums;

namespace Bit.App.Abstractions
{
    public interface IPasswordRepromptService
    {
        string[] ProtectedFields { get; }

        Task<bool> PromptAndCheckPasswordIfNeededAsync(CipherRepromptType repromptType = CipherRepromptType.Password);

        Task<(string password, bool valid)> ShowPasswordPromptAndGetItAsync();

        Task<bool> ShouldByPassMasterPasswordRepromptAsync();
    }
}
