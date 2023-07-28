using System.Threading.Tasks;

namespace Bit.App.Abstractions
{
    public interface IPasswordRepromptService
    {
        string[] ProtectedFields { get; }

        Task<bool> ShowPasswordPromptAsync();

        Task<(string password, bool valid)> ShowPasswordPromptAndGetItAsync();

        Task<bool> Enabled();
    }
}
