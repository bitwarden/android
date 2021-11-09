using System;
namespace Bit.Core.Models.Request
{
    public class VerifyOtpRequest
    {
        public string Otp;

        public VerifyOtpRequest(string otp)
        {
            Otp = otp;
        }
    }
}
