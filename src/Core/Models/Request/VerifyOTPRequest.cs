using System;
namespace Bit.Core.Models.Request
{
    public class VerifyOTPRequest
    {
        public string OTP;

        public VerifyOTPRequest(string otp)
        {
            OTP = otp;
        }
    }
}
