using Bit.Core.Enums;
using Bit.Core.Models.Data;

namespace Bit.Core.Utilities
{
    public static class RegionExtensions
    {
        public static EnvironmentUrlData GetUrls(this Region region)
        {
            switch (region)
            {
                case Region.US:
                    return EnvironmentUrlData.DefaultUS;
                case Region.EU:
                    return EnvironmentUrlData.DefaultEU;
                case Region.SelfHosted:
                default:
                    return null;
            }
        }

        public static string BaseUrl(this Region region)
        {
            switch (region)
            {
                case Region.US:
                    return EnvironmentUrlData.DefaultUS.Base;
                case Region.EU:
                    return EnvironmentUrlData.DefaultEU.Base;
                case Region.SelfHosted:
                default:
                    return null;
            }
        }

        public static string Domain(this Region region)
        {
            switch (region)
            {
                case Region.US:
                    return EnvironmentUrlData.DefaultUS.Domain;
                case Region.EU:
                    return EnvironmentUrlData.DefaultEU.Domain;
                case Region.SelfHosted:
                default:
                    return null;
            }
        }

    }
}

