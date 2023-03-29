using System.Threading.Tasks;

namespace Bit.Core.Abstractions
{
    public interface IStateMigrationService
    {
        Task MigrateIfNeededAsync();
    }
}
