#nullable enable

using Bit.Core.Enums;

namespace Bit.Core.Utilities.Fido2
{
    public enum Fido2UserVerificationPreference
    {
        Discouraged,
        Preferred,
        Required
    }

    public static class Fido2UserVerificationPreferenceExtensions
    {
        public static Fido2UserVerificationPreference ToFido2UserVerificationPreference(string? preference)
        {
            switch (preference)
            {
                case "required":
                    return Fido2UserVerificationPreference.Required;
                case "discouraged":
                    return Fido2UserVerificationPreference.Discouraged;
                default:
                    return Fido2UserVerificationPreference.Preferred;
            }
        }

        public static Fido2UserVerificationPreference GetUserVerificationPreferenceFrom(Fido2UserVerificationPreference preference, CipherRepromptType repromptType)
        {
            if (repromptType != CipherRepromptType.None)
            {
                return Fido2UserVerificationPreference.Required;
            }

            return preference;
        }
    }
}
