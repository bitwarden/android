using System;

namespace Bit.Core.Utilities
{
    public static class VersionHelpers
    {
        private const char HOTFIX_SEPARATOR = '-';

        /// <summary>
        /// Compares two server versions and gets whether the <paramref name="targetVersion"/>
        /// is greater than or equal to <paramref name="compareToVersion"/>.
        /// WARNING: This doesn't take into account hotfix suffix.
        /// </summary>
        /// <param name="targetVersion">Version to compare</param>
        /// <param name="compareToVersion">Version to compare against</param>
        /// <returns>
        /// <c>True</c> if <paramref name="targetVersion"/> is greater than or equal to <paramref name="compareToVersion"/>; <c>False</c> otherwise.
        /// </returns>
        public static bool IsServerVersionGreaterThanOrEqualTo(string targetVersion, string compareToVersion)
        {
            return GetServerVersionWithoutHotfix(targetVersion).CompareTo(GetServerVersionWithoutHotfix(compareToVersion)) >= 0;
        }

        public static Version GetServerVersionWithoutHotfix(string version)
        {
            if (string.IsNullOrWhiteSpace(version))
            {
                throw new ArgumentNullException(nameof(version));
            }

            return new Version(version.Split(HOTFIX_SEPARATOR)[0]);
        }
    }
}
