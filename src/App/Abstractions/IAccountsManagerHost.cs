using System.Threading.Tasks;
using Bit.Core.Enums;

namespace Bit.App.Abstractions
{
    public interface INavigationParams { }

    public interface IAccountsManagerHost
    {
        Task SetPreviousPageInfoAsync();
        void Navigate(NavigationTarget navTarget, INavigationParams navParams = null);
        Task UpdateThemeAsync();
    }
}
