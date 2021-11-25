using System.Threading.Tasks;

namespace Bit.App.Utilities
{
    public interface IPasswordPromptable
    {
        Task<bool> PromptPasswordAsync();
    }
}
