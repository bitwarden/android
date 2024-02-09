using System;
using System.Threading.Tasks;
using Microsoft.Maui.Controls;
using Microsoft.Maui;

namespace Bit.App.Utilities
{
    public static class PageExtensions
    {
        public static async Task TraverseNavigationRecursivelyAsync(this Page page, Func<Page, Task> actionOnPage)
        {
            if (page?.Navigation?.ModalStack != null)
            {
                foreach (var p in page.Navigation.ModalStack)
                {
                    if (p is NavigationPage modalNavPage)
                    {
                        await TraverseNavigationStackRecursivelyAsync(modalNavPage.CurrentPage, actionOnPage);
                    }
                    else
                    {
                        await TraverseNavigationStackRecursivelyAsync(p, actionOnPage);
                    }
                }
            }

            await TraverseNavigationStackRecursivelyAsync(page, actionOnPage);
        }

        private static async Task TraverseNavigationStackRecursivelyAsync(this Page page, Func<Page, Task> actionOnPage)
        {
            if (page is MultiPage<Page> multiPage && multiPage.Children != null)
            {
                foreach (var p in multiPage.Children)
                {
                    await TraverseNavigationStackRecursivelyAsync(p, actionOnPage);
                }
            }

            if (page is NavigationPage && page.Navigation != null)
            {
                if (page.Navigation.NavigationStack != null)
                {
                    foreach (var p in page.Navigation.NavigationStack)
                    {
                        await TraverseNavigationStackRecursivelyAsync(p, actionOnPage);
                    }
                }
            }

            await actionOnPage(page);
        }
    }
}
