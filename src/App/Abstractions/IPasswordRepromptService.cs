using System.Threading.Tasks;

namespace Bit.App.Abstractions
{
    public interface IPasswordRepromptService
    {
        string[] ProtectedFields { get; }

        Task<bool> ShowPasswordPromptAsync();
    }
}
