using System.Threading.Tasks;

namespace Bit.App.Styles
{
    /// <summary>
    /// This is an interface to mark the pages that need theme update special treatment
    /// given that they aren't updated automatically by the Forms theme system.
    /// </summary>
    public interface IThemeDirtablePage
    {
        bool IsThemeDirty { get; set; }

        Task UpdateOnThemeChanged();
    }
}
