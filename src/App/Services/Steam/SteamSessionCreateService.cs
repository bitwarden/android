using System;
using System.Collections.Generic;
using System.Collections.Specialized;
using System.Net;
using System.Security.Cryptography;
using System.Text;
using Bit.App.Models.Steam;
using Bit.App.Utilities;
using Bit.App.Utilities.Steam;
using Newtonsoft.Json;

namespace Bit.App.Services.Steam
{
    public class SteamSessionCreateService
    {
        private class RSAResponse
        {
            [JsonProperty("success")]
            public bool Success { get; set; }

            [JsonProperty("publickey_exp")]
            public string Exponent { get; set; }

            [JsonProperty("publickey_mod")]
            public string Modulus { get; set; }

            [JsonProperty("timestamp")]
            public string Timestamp { get; set; }

            [JsonProperty("steamid")]
            public ulong SteamID { get; set; }
        }

        public enum Status
        {
            Okay,
            GeneralFailure,
            BadRSA,
            BadCredentials,
            NeedCaptcha,
            Need2FA,
            NeedEmail,
            TooManyFailedLogins,
        }

        private class LoginResponse
        {
            [JsonProperty("success")]
            public bool Success { get; set; }

            [JsonProperty("login_complete")]
            public bool LoginComplete { get; set; }

            [JsonProperty("oauth")]
            public string OAuthDataString { get; set; }

            public OAuth OAuthData
            {
                get
                {
                    return OAuthDataString != null ? JsonConvert.DeserializeObject<OAuth>(OAuthDataString) : null;
                }
            }

            [JsonProperty("captcha_needed")]
            public bool CaptchaNeeded { get; set; }

            [JsonProperty("captcha_gid")]
            public string CaptchaGID { get; set; }

            [JsonProperty("emailsteamid")]
            public ulong EmailSteamID { get; set; }

            [JsonProperty("emailauth_needed")]
            public bool EmailAuthNeeded { get; set; }

            [JsonProperty("requires_twofactor")]
            public bool TwoFactorNeeded { get; set; }

            [JsonProperty("message")]
            public string Message { get; set; }

            internal class OAuth
            {
                [JsonProperty("steamid")]
                public ulong SteamID { get; set; }

                [JsonProperty("oauth_token")]
                public string OAuthToken { get; set; }

                [JsonProperty("wgtoken")]
                public string SteamLogin { get; set; }

                [JsonProperty("wgtoken_secure")]
                public string SteamLoginSecure { get; set; }

                [JsonProperty("webcookie")]
                public string Webcookie { get; set; }
            }
        }

        public SteamSessionCreateService(string username, string password)
        {
            this.username = username;
            this.clearPassword = password;
        }

        private string username, clearPassword;
        private ulong steamID;

        private bool requiresCaptcha;
        public string CaptchaID = null;
        public string CaptchaText = null;

        private bool requiresEmail;
        public string EmailCode = null;

        private bool requires2FA;
        private string twoFactorCode = null;

        public (Status, SteamSession) TryCreateSession()
        {
            CookieContainer cookieContainer = new CookieContainer();

            cookieContainer.Add(new Cookie("mobileClientVersion", "0 (2.1.3)", "/", ".steamcommunity.com"));
            cookieContainer.Add(new Cookie("mobileClient", "android", "/", ".steamcommunity.com"));
            cookieContainer.Add(new Cookie("Steam_Language", "english", "/", ".steamcommunity.com"));

            NameValueCollection headers = new NameValueCollection();
            headers.Add("X-Requested-With", "com.valvesoftware.android.steam.community");

            SteamWebHelper.MobileLoginRequest(@"https://steamcommunity.com/login?oauth_client_id=DE45CD61&oauth_scope=read_profile%20write_profile%20read_client%20write_client", "GET", null, cookieContainer, headers);

            RSAResponse rsaResponse = GetRSAResponse(cookieContainer);
            if (!rsaResponse.Success) return (Status.BadRSA, null);

            string encryptedPassword = EncryptPassword(rsaResponse.Exponent, rsaResponse.Modulus);

            NameValueCollection postData = BuildPostData(encryptedPassword, rsaResponse.Timestamp);

            string response = SteamWebHelper.MobileLoginRequest(SteamAPIEndpoints.COMMUNITY_BASE + "/login/dologin", "POST", postData, cookieContainer);
            if (response == null) return (Status.GeneralFailure, null);

            var loginResponse = JsonConvert.DeserializeObject<LoginResponse>(response);

            return EvaluateLoginResponse(loginResponse, cookieContainer);
        }

        private (Status, SteamSession) EvaluateLoginResponse(LoginResponse loginResponse, CookieContainer cookieContainer)
        {
            if (loginResponse.Message != null && loginResponse.Message.Contains("Incorrect login"))
            {
                return (Status.BadCredentials, null);
            }

            if (loginResponse.CaptchaNeeded)
            {
                requiresCaptcha = true;
                CaptchaID = loginResponse.CaptchaGID;
                return (Status.NeedCaptcha, null);
            }

            if (loginResponse.EmailAuthNeeded)
            {
                requiresEmail = true;
                steamID = loginResponse.EmailSteamID;
                return (Status.NeedEmail, null);
            }

            if (loginResponse.TwoFactorNeeded && !loginResponse.Success)
            {
                requires2FA = true;
                return (Status.Need2FA, null);
            }

            if (loginResponse.Message != null && loginResponse.Message.Contains("too many login failures"))
            {
                return (Status.TooManyFailedLogins, null);
            }

            if (loginResponse.OAuthData == null || loginResponse.OAuthData.OAuthToken == null || loginResponse.OAuthData.OAuthToken.Length == 0)
            {
                return (Status.GeneralFailure, null);
            }

            if (!loginResponse.LoginComplete)
            {
                return (Status.BadCredentials, null);
            }
            else
            {
                var readableCookies = cookieContainer.GetCookies(new Uri("https://steamcommunity.com"));
                var oAuthData = loginResponse.OAuthData;

                SteamSession session = new SteamSession();
                session.OAuthToken = oAuthData.OAuthToken;
                session.SteamID = oAuthData.SteamID;
                session.SteamLogin = session.SteamID + "%7C%7C" + oAuthData.SteamLogin;
                session.SteamLoginSecure = session.SteamID + "%7C%7C" + oAuthData.SteamLoginSecure;
                session.WebCookie = oAuthData.Webcookie;
                session.SessionID = readableCookies["sessionid"].Value;
                return (Status.Okay, session);
            }
        }

        private RSAResponse GetRSAResponse(CookieContainer cookieContainer)
        {
            NameValueCollection postData = new NameValueCollection();
            postData.Add("username", username);
            string response = SteamWebHelper.MobileLoginRequest(SteamAPIEndpoints.COMMUNITY_BASE + "/login/getrsakey", "POST", postData, cookieContainer);

            return JsonConvert.DeserializeObject<RSAResponse>(response);
        }

        private NameValueCollection BuildPostData(string encryptedPassword, string timestamp)
        {
            NameValueCollection postData = new NameValueCollection();
            postData.Add("username", username);
            postData.Add("password", encryptedPassword);

            postData.Add("twofactorcode", twoFactorCode ?? "");

            postData.Add("captchagid", requiresCaptcha ? CaptchaID : "-1");
            postData.Add("captcha_text", requiresCaptcha ? CaptchaText : "");

            postData.Add("emailsteamid", (requires2FA || requiresEmail) ? steamID.ToString() : "");
            postData.Add("emailauth", requiresEmail ? EmailCode : "");

            postData.Add("rsatimestamp", timestamp);
            postData.Add("remember_login", "false");
            postData.Add("oauth_client_id", "DE45CD61");
            postData.Add("oauth_scope", "read_profile write_profile read_client write_client");
            postData.Add("loginfriendlyname", "#login_emailauth_friendlyname_mobile");
            postData.Add("donotcache", SteamTimeSyncHelper.GetSystemUnixTime().ToString());
            return postData;
        }

        private string EncryptPassword(string exponent, string modulus)
        {
            RNGCryptoServiceProvider secureRandom = new RNGCryptoServiceProvider();
            byte[] encryptedPasswordBytes;
            using (var rsaEncryptor = new RSACryptoServiceProvider())
            {
                var passwordBytes = Encoding.ASCII.GetBytes(clearPassword);
                var rsaParameters = rsaEncryptor.ExportParameters(false);
                rsaParameters.Exponent = HexStringToByteArray(exponent);
                rsaParameters.Modulus = HexStringToByteArray(modulus);
                rsaEncryptor.ImportParameters(rsaParameters);
                encryptedPasswordBytes = rsaEncryptor.Encrypt(passwordBytes, false);
            }

            string encryptedPassword = Convert.ToBase64String(encryptedPasswordBytes);
            return encryptedPassword;
        }
        private static byte[] HexStringToByteArray(string hex)
        {
            int hexLen = hex.Length;
            byte[] ret = new byte[hexLen / 2];
            for (int i = 0; i < hexLen; i += 2)
            {
                ret[i / 2] = Convert.ToByte(hex.Substring(i, 2), 16);
            }
            return ret;
        }
    }
}
