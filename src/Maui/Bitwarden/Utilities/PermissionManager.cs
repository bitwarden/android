using System.Threading.Tasks;
using static Microsoft.Maui.ApplicationModel.Permissions;

namespace Bit.App.Utilities
{
    public static class PermissionManager
    {
        public static async Task<PermissionStatus> CheckAndRequestPermissionAsync<T>(T permission)
            where T : BasePermission
        {
            var status = await permission.CheckStatusAsync();
            if (status != PermissionStatus.Granted)
            {
                status = await permission.RequestAsync();
            }

            return status;
        }
    }
}
