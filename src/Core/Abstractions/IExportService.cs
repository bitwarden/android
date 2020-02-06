using System.Threading.Tasks;

namespace Bit.Core.Abstractions
{
    public interface IExportService
    {
        Task GetExport(string format = "csv");
        Task GetOrganizationExport(string organizationId, string format = "csv");
        string GetFileName(string prefix = null, string extension = "csv");
    }
}
