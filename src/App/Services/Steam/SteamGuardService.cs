,using System;
using System.Collections.Generic;
using System.Collections.Specialized;
using System.Net;
using System.Security.Cryptography;
using System.Text;
using Bit.App.Abstractions;
using Bit.App.Models.Steam;
using Bit.App.Utilities;
using Bit.App.Utilities.Steam;
using Bit.Core.Enums;
using Newtonsoft.Json;

namespace Bit.App.Services.Steam
{
    public class SteamGuardService : ISteamGuardService
    {
        private class HasPhoneResponse
        {
            [JsonProperty("has_phone")]
            public bool HasPhone { get; set; }
        }

        private class AddAuthenticatorResponse
        {
            [JsonProperty("response")]
            public SteamGuardData Response { get; set; }
        }

        private class FinalizeAuthenticatorResponse
        {
            [JsonProperty("response")]
            public FinalizeAuthenticatorInternalResponse Response { get; set; }

            internal class FinalizeAuthenticatorInternalResponse
            {
                [JsonProperty("status")]
                public int Status { get; set; }

                [JsonProperty("server_time")]
                public long ServerTime { get; set; }

                [JsonProperty("want_more")]
                public bool WantMore { get; set; }

                [JsonProperty("success")]
                public bool Success { get; set; }
            }
        }

        public string TOTPSecret => _steamGuardData.SharedSecret;

        public string RecoveryCode => _steamGuardData.RevocationCode;

        public string CaptchaGID => _sessionService.CaptchaID;

        public SteamGuardServiceError Error { get; set; } = SteamGuardServiceError.None;

        private SteamSessionCreateService _sessionService;
        private SteamGuardData _steamGuardData = new SteamGuardData();
        private SteamSession _steamSession;


        public SteamGuardService()
        {
            _steamGuardData.DeviceID = RandomDeviceID();
        }

        private bool HasPhoneAttached()
        {
            var postData = new NameValueCollection();
            postData.Add("op", "has_phone");
            postData.Add("arg", "null");
            postData.Add("sessionid", _steamSession.SessionID);

            CookieContainer cookieContainer = new CookieContainer();
            cookieContainer.Add(new Cookie("mobileClientVersion", "0 (2.1.3)", "/", ".steamcommunity.com"));
            cookieContainer.Add(new Cookie("mobileClient", "android", "/", ".steamcommunity.com"));

            cookieContainer.Add(new Cookie("steamid", _steamSession.SteamID.ToString(), "/", ".steamcommunity.com"));
            cookieContainer.Add(new Cookie("steamLogin", _steamSession.SteamLogin, "/", ".steamcommunity.com")
            {
                HttpOnly = true
            });

            cookieContainer.Add(new Cookie("steamLoginSecure", _steamSession.SteamLoginSecure, "/", ".steamcommunity.com")
            {
                HttpOnly = true,
                Secure = true
            });
            cookieContainer.Add(new Cookie("Steam_Language", "english", "/", ".steamcommunity.com"));
            cookieContainer.Add(new Cookie("dob", "", "/", ".steamcommunity.com"));
            cookieContainer.Add(new Cookie("sessionid", _steamSession.SessionID, "/", ".steamcommunity.com"));

            string response = SteamWebHelper.Request(SteamAPIEndpoints.COMMUNITY_BASE + "/steamguard/phoneajax", "POST", postData, cookieContainer);
            if (response == null) return false;

            var hasPhoneResponse = JsonConvert.DeserializeObject<HasPhoneResponse>(response);
            return hasPhoneResponse.HasPhone;
        }

        public void RequestSMSCode()
        {
            // adding a phone to Steam should not be handled by Bitwarden
            bool hasPhone = HasPhoneAttached();
            if (!hasPhone)
            {
                throw new Exception("USER HAS TO APPEND A PHONE NUMBER");
            }


            var postData = new NameValueCollection();
            postData.Add("access_token", _steamSession.OAuthToken);
            postData.Add("steamid", _steamSession.SteamID.ToString());
            postData.Add("authenticator_type", "1");
            postData.Add("device_identifier", _steamGuardData.DeviceID);
            postData.Add("sms_phone_id", "1");

            string response = SteamWebHelper.MobileLoginRequest(SteamAPIEndpoints.STEAMAPI_BASE + "/ITwoFactorService/AddAuthenticator/v0001", "POST", postData);
            if (response == null)
            {
                throw new Exception("GENERAL EXCEPTION");
            }

            var addAuthenticatorResponse = JsonConvert.DeserializeObject<AddAuthenticatorResponse>(response);
            if (addAuthenticatorResponse == null || addAuthenticatorResponse.Response == null)
            {
                throw new Exception("GENERAL EXCEPTION");
            }

            if (addAuthenticatorResponse.Response.Status == 29)
            {
                throw new Exception("ALLREADY LINKED TO STEAM AUTHENTICATOR");
            }

            if (addAuthenticatorResponse.Response.Status != 1)
            {
                throw new Exception("GENERAL EXCEPTION");
            }

            _steamGuardData = addAuthenticatorResponse.Response;
        }

        private (SteamSessionCreateService.Status status, SteamSession session) GetSessionStatus()
        {
            (SteamSessionCreateService.Status status, SteamSession session) createSessionResponse = _sessionService.TryCreateSession();
            _steamSession = createSessionResponse.session;
            return createSessionResponse;
        }

        static string RandomDeviceID()
        {
            string SplitOnRatios(string str, int[] ratios, string intermediate)
            {
                string result = "";

                int pos = 0;
                for (int index = 0; index < ratios.Length; index++)
                {
                    result += str.Substring(pos, ratios[index]);
                    pos = ratios[index];

                    if (index < ratios.Length - 1)
                        result += intermediate;
                }

                return result;
            }


            using (var sha1 = new SHA1Managed())
            {
                RNGCryptoServiceProvider secureRandom = new RNGCryptoServiceProvider();
                byte[] randomBytes = new byte[8];
                secureRandom.GetBytes(randomBytes);

                byte[] hashedBytes = sha1.ComputeHash(randomBytes);
                string random32 = BitConverter.ToString(hashedBytes).Replace("-", "").Substring(0, 32).ToLower();

                return "android:" + SplitOnRatios(random32, new[] { 8, 4, 4, 4, 12 }, "-");
            }
        }

        public SteamGuardServiceResponse SubmitUsernamePassword(string username, string password)
        {
            _sessionService = new SteamSessionCreateService(username, password);
            var sessionStatus = GetSessionStatus();
            switch (sessionStatus.status)
            {
                case SteamSessionCreateService.Status.Error_EmptyResponse:
                    Error = SteamGuardServiceError.EmptyResponse;
                    return SteamGuardServiceResponse.Error;
                case SteamSessionCreateService.Status.NeedEmail:
                    return SteamGuardServiceResponse.NeedEmailCode;
                case SteamSessionCreateService.Status.NeedCaptcha:
                    return SteamGuardServiceResponse.NeedCaptcha;
                case SteamSessionCreateService.Status.BadCredentials:
                    return SteamGuardServiceResponse.WrongCredentials;
            }
            return SteamGuardServiceResponse.Error;
        }

        public SteamGuardServiceResponse SubmitCaptcha(string captcha)
        {
            _sessionService.CaptchaText = captcha;
            var sessionStatus = GetSessionStatus();
            switch (sessionStatus.status)
            {
                case SteamSessionCreateService.Status.NeedEmail:
                    return SteamGuardServiceResponse.NeedEmailCode;
                case SteamSessionCreateService.Status.NeedCaptcha:
                    return SteamGuardServiceResponse.WrongCaptcha;
            }
            return SteamGuardServiceResponse.Error;
        }

        SteamGuardServiceResponse ISteamGuardService.SubmitEmailCode(string code)
        {
            _sessionService.EmailCode = code;
            var sessionStatus = GetSessionStatus();
            switch (sessionStatus.status)
            {
                case SteamSessionCreateService.Status.NeedEmail:
                    return SteamGuardServiceResponse.WrongEmailCode;
                case SteamSessionCreateService.Status.Okay:
                    return SteamGuardServiceResponse.NeedSMSCode;
            }
            return SteamGuardServiceResponse.Error;

        }

        SteamGuardServiceResponse ISteamGuardService.SubmitSMSCode(string code)
        {
            var postData = new NameValueCollection();
            postData.Add("steamid", _steamSession.SteamID.ToString());
            postData.Add("access_token", _steamSession.OAuthToken);
            postData.Add("activation_code", code);
            int tries = 0;
            while (tries <= 30)
            {
                postData.Set("authenticator_code", _steamGuardData.GenerateSteamGuardCode());
                postData.Set("authenticator_time", SteamTimeSyncHelper.GetSteamUnixTime().ToString());

                string response = SteamWebHelper.MobileLoginRequest(SteamAPIEndpoints.STEAMAPI_BASE + "/ITwoFactorService/FinalizeAddAuthenticator/v0001", "POST", postData);
                if (response == null)
                {
                    Error = SteamGuardServiceError.EmptyResponse;
                    return SteamGuardServiceResponse.Error;
                }

                var finalizeResponse = JsonConvert.DeserializeObject<FinalizeAuthenticatorResponse>(response);

                if (finalizeResponse == null || finalizeResponse.Response == null)
                {
                    Error = SteamGuardServiceError.CorruptResponse;
                    return SteamGuardServiceResponse.Error;
                }

                if (finalizeResponse.Response.Status == 89)
                {
                    return SteamGuardServiceResponse.WrongSMSCode;
                }

                if (finalizeResponse.Response.Status == 88)
                {
                    if (tries >= 30)
                    {
                        Error = SteamGuardServiceError.GuardSyncFailed;
                        return SteamGuardServiceResponse.Error;
                    }
                }

                if (!finalizeResponse.Response.Success)
                {
                    Error = SteamGuardServiceError.SuccessMissing;
                    return SteamGuardServiceResponse.Error;
                }

                if (finalizeResponse.Response.WantMore)
                {
                    tries++;
                    continue;
                }

                return SteamGuardServiceResponse.Okay;
            }

            Error = SteamGuardServiceError.General;
            return SteamGuardServiceResponse.Error;
        }
    }
}
