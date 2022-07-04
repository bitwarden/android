using System.Threading.Tasks;
using Bit.Core.Models.View;
using Xamarin.Forms;

namespace Bit.App.Utilities.Helpers
{
    public interface ICipherHelper
    {
        Task<string> ShowCipherOptionsAsync(Page page, CipherView cipher);
        Task CopyUsernameAsync(CipherView cipher);
        Task CopyPasswordAsync(CipherView cipher);
    }
}
