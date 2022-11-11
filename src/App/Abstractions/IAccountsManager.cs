using System;
using System.Threading.Tasks;
using Bit.App.Models;

namespace Bit.App.Abstractions
{
    public interface IAccountsManager
    {
        void Init(Func<AppOptions> getOptionsFunc, IAccountsManagerHost accountsManagerHost);
        Task NavigateOnAccountChangeAsync(bool? isAuthed = null);
        Task LogOutAsync(string userId, bool userInitiated, bool expired);
        Task PromptToSwitchToExistingAccountAsync(string userId);
    }
}
