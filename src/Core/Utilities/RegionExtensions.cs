using Bit.Core.Models.Data;
using BwRegion = Bit.Core.Enums.Region;

namespace Bit.Core.Utilities
{
    public static class RegionExtensions
    {
        public static EnvironmentUrlData GetUrls(this BwRegion region)
        {
            switch (region)
            {
                case BwRegion.US:
                    return EnvironmentUrlData.DefaultUS;
                case BwRegion.EU:
                    return EnvironmentUrlData.DefaultEU;
                default:
                    return null;
            }
        }

        public static string BaseUrl(this BwRegion region)
        {
            switch (region)
            {
                case BwRegion.US:
                    return EnvironmentUrlData.DefaultUS.Base;
                case BwRegion.EU:
                    return EnvironmentUrlData.DefaultEU.Base;
                default:
                    return null;
            }
        }

        public static string Domain(this BwRegion region)
        {
            switch (region)
            {
                case BwRegion.US:
                    return EnvironmentUrlData.DefaultUS.Domain;
                case BwRegion.EU:
                    return EnvironmentUrlData.DefaultEU.Domain;
                default:
                    return null;
            }
        }

    }
}

